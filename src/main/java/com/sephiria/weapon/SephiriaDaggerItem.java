package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Dash;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 匕首：轻快近战 + 右键的两段式技能。
 *
 * <p>右键的技能在两个形态之间切换，由「专注」层数决定：
 * <ul>
 *   <li><b>招架</b>（没有专注时）：朝准星方向突进 {@value #PARRY_DISTANCE} 格、耗时
 *       {@value #DASH_TICKS} tick（0.2 秒），期间无敌；位移结束后以<b>自身为中心</b>
 *       结算一次范围伤害（半径同横扫之刃 II，即原版横扫的两倍），伤害 {@value #PARRY_DAMAGE}。
 *       冷却 {@value #PARRY_COOLDOWN} tick（0.8 秒）。<br>
 *       无敌窗口内挨到攻击判定为<b>招架成功</b>：获得一层专注并<b>刷新技能冷却</b>，可以立刻接狂怒——判定与减伤都写在 {@link #register()}
 *       的伤害拦截里（自包含，不依赖其它无敌实现的注册顺序），奖励延后一刻发放。</li>
 *   <li><b>狂怒</b>（有专注时）：消耗一层专注，朝准星方向突进 {@value #FURY_DISTANCE} 格、
 *       同样耗时 {@value #DASH_TICKS} tick 且无敌；位移结束后对<b>突进路径上</b>的敌人
 *       造成 {@value #FURY_DAMAGE} 范围伤害。冷却 {@value #FURY_COOLDOWN} tick（0.2 秒）。</li>
 * </ul>
 *
 * <p>专注用技能存储管理（见 {@link SkillStorage}），默认上限 {@value #FOCUS_MAX} 层，
 * 不自动回复，只能靠招架攒。HUD 的匕首特有属性显示它。
 */
public class SephiriaDaggerItem extends Item implements SephiriaWeapon {
	/** 专注在技能存储里的 id（HUD 也读它）。 */
	public static final Identifier FOCUS = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "focus");

	/** 专注上限：默认 1 层。 */
	public static final double FOCUS_MAX = 1.0D;

	/** 两段位移的距离；无敌时长 {@value #INVULNERABLE_TICKS} 刻比位移多 2 刻，落地后仍有一点容错。 */
	/** 攻速目标值（供物品注册使用）。 */
	public static final float ATTACK_SPEED = 6.4F;
	/** 伤害目标值（供物品注册使用）。 */
	public static final float ATTACK_DAMAGE = 2.05F;

	/** 招架：突进距离、范围、伤害。 */
	private static final double PARRY_DISTANCE = 1.5D;
	private static final double FURY_DISTANCE = 8.0D;
	private static final int DASH_TICKS = 4;
	private static final int INVULNERABLE_TICKS = 6;

	/** 招架范围的放大倍数：1.0 = 横扫之刃 II，当前取 1.8。 */
	private static final double PARRY_RANGE_SCALE = 1.65D;
	/** 招架的范围与伤害：横扫之刃 II 的基础（原版横扫的 2 倍）再放大 {@value #PARRY_RANGE_SCALE} 倍。 */
	private static final double PARRY_RANGE = 2.0D * PARRY_RANGE_SCALE;
	private static final double PARRY_HEIGHT = 0.5D * PARRY_RANGE_SCALE;
	private static final float PARRY_DAMAGE = 8.0F;

	/** 狂怒：路径两侧的判定半径（原判定半径的 2 倍）与伤害。 */
	private static final double FURY_PATH_RADIUS = 3.0D;
	private static final float FURY_DAMAGE = 14.0F;

	/** 冷却（tick）：0.8 秒 / 0.2 秒。 */
	private static final int PARRY_COOLDOWN = 16;
	private static final int FURY_COOLDOWN = 4;

	/** 招架窗口的到期刻：窗口内挨打就算招架成功。 */
	private static final Map<UUID, Long> PARRY_UNTIL = new ConcurrentHashMap<>();
	/** 刚判定招架成功、等待下一刻发奖励的玩家。 */
	private static final Set<UUID> PENDING_FOCUS = ConcurrentHashMap.newKeySet();
	/** 待结算的范围伤害。 */
	private static final List<Pending> PENDING = new ArrayList<>();

	private final WeaponBranch branch;

	public SephiriaDaggerItem(WeaponBranch branch, Item.Properties properties) {
		super(properties);
		this.branch = branch;
	}

	@Override
	public WeaponBranch branch() {
		return this.branch;
	}

	@Override
	public java.util.List<Component> detailLines(ItemStack stack) {
		return java.util.List.of(
				Line.attack(ATTACK_DAMAGE, ATTACK_SPEED, WeaponSweep.RANGE).build(),
				Line.titled("tooltip.sephiria.skill.parry")
						.damage(PARRY_DAMAGE).cooldownTicks(PARRY_COOLDOWN)
						.range(PARRY_RANGE).distance(PARRY_DISTANCE).build(),
				desc("tooltip.sephiria.skill.parry", "tooltip.sephiria.desc.parry"),
				Line.titled("tooltip.sephiria.skill.fury")
						.damage(FURY_DAMAGE).cooldownTicks(FURY_COOLDOWN)
						.range(FURY_PATH_RADIUS).distance(FURY_DISTANCE).build(),
				desc("tooltip.sephiria.skill.fury", "tooltip.sephiria.desc.fury"),
				Line.titled("tooltip.sephiria.skill.focus")
						.stat("tooltip.sephiria.part.max", FOCUS_MAX, COLOR_RANGE).build());
	}


	/** 注册专注存储、结算推进器与招架判定（招架窗口内的伤害由本处理器直接取消，与注册顺序无关）。 */
	public static void register() {
		SkillStorage.registerManual(FOCUS, FOCUS_MAX);
		ServerTickEvents.END_SERVER_TICK.register(SephiriaDaggerItem::tick);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isParrying(player)) {
				// 招架成功：结束这次窗口（一次招架只算一次），并直接取消这次伤害——
				// 不再指望后面注册的 Invulnerability 处理器代劳，招架窗口自己就能挡住。
				// 奖励也挪到下一刻发（见 tick）：在伤害事件里改技能存储、发包容易把
				// 后续处理器搅乱，之前「招架成功却仍然掉血」就是这么来的。
				PARRY_UNTIL.remove(player.getUUID());
				PENDING_FOCUS.add(player.getUUID());
				return false;
			}

			return true;
		});
	}

	/** 右键：有专注放狂怒，否则招架。 */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}

		if (level.isClientSide()) {
			return InteractionResult.CONSUME;
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;

		if (SkillStorage.current(serverPlayer, FOCUS) >= 1.0D) {
			fury(serverPlayer, stack);
		}
		else {
			parry(serverPlayer, stack);
		}

		return InteractionResult.CONSUME;
	}

	/** 招架：短突进 + 无敌，落地后以自身为中心结算范围伤害。 */
	private static void parry(ServerPlayer player, ItemStack stack) {
		Vec3 direction = lookDirection(player);
		Invulnerability.grant(player, INVULNERABLE_TICKS);
		PARRY_UNTIL.put(player.getUUID(), player.level().getGameTime() + INVULNERABLE_TICKS);
		Dash.startVertical(player, direction, PARRY_DISTANCE, DASH_TICKS, Dash.DEFAULT_DECAY);
		player.getCooldowns().addCooldown(stack, PARRY_COOLDOWN);

		PENDING.add(Pending.aura(player, DASH_TICKS, (float) (PARRY_DAMAGE * PlayerStats.damageMultiplier(player)), PARRY_RANGE, PARRY_HEIGHT));
	}

	/** 狂怒：长突进 + 无敌，落地后结算突进路径上的范围伤害，并消耗一层专注。 */
	private static void fury(ServerPlayer player, ItemStack stack) {
		Vec3 direction = lookDirection(player);
		Invulnerability.grant(player, INVULNERABLE_TICKS);
		Dash.startVertical(player, direction, FURY_DISTANCE, DASH_TICKS, Dash.DEFAULT_DECAY);
		player.getCooldowns().addCooldown(stack, FURY_COOLDOWN);

		SkillStorage.consume(player, FOCUS, 1.0D);
		PENDING.add(Pending.path(player, direction, FURY_DISTANCE, DASH_TICKS, (float) (FURY_DAMAGE * PlayerStats.damageMultiplier(player)), FURY_PATH_RADIUS));
	}

	/** 完整的准星朝向：匕首沿真正的视线突进，抬头会向上冲、低头会向下冲。 */
	private static Vec3 lookDirection(ServerPlayer player) {
		return player.getLookAngle();
	}

	/**
	 * 招架成功的奖励：一层专注、一声格挡音，外加<b>刷新技能冷却</b>——
	 * 冷却清掉之后可以立刻接狂怒，形成招架→狂怒的连段。
	 */
	private static void rewardParry(ServerPlayer player) {
		SkillStorage.regenerate(player, FOCUS, FOCUS_MAX);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);

		ItemStack dagger = heldDagger(player);

		if (dagger != null) {
			// 冷却是按冷却组记账的，要从物品解析出组 id 再清
			player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(dagger));
		}
	}

	/** 玩家手上（主手或副手）的匕首，没有就返回 null。 */
	private static ItemStack heldDagger(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);

			if (stack.getItem() instanceof SephiriaDaggerItem) {
				return stack;
			}
		}

		return null;
	}

	private static boolean isParrying(ServerPlayer player) {
		Long until = PARRY_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		// 到期刻是绝对游戏刻，要和当前刻比较（之前误写成 <= 0，等于从不清理）
		PARRY_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);

		if (!PENDING_FOCUS.isEmpty()) {
			for (UUID id : PENDING_FOCUS) {
				ServerPlayer player = server.getPlayerList().getPlayer(id);

				if (player != null) {
					rewardParry(player);
				}
			}

			PENDING_FOCUS.clear();
		}

		if (PENDING.isEmpty()) {
			return;
		}

		// 用快照遍历：resolve() 里可能往 PENDING 追加新的一段（比如回击的第二段），
		// 直接在原列表上迭代会抛 ConcurrentModificationException。
		for (Pending pending : new ArrayList<>(PENDING)) {
			if (pending.dueTick > now) {
				continue;
			}

			PENDING.remove(pending);
			pending.resolve();
		}
	}

	/**
	 * 一次待结算的范围伤害。
	 *
	 * <p>两种区域：{@code followsPlayer} 为真表示以自身为中心，位置在<b>结算那一刻</b>取
	 * （也就是位移结束后的落点）；否则用释放时定格的 {@code area}——狂怒要打的是整条突进路径，
	 * 那必须在起跳时就把路径框出来。
	 */
	private static final class Pending {
		private final ServerPlayer player;
		private final long dueTick;
		private final float damage;
		private final AABB area;
		private final boolean followsPlayer;
		private final double range;
		private final double height;

		private Pending(ServerPlayer player, long dueTick, float damage, AABB area,
				boolean followsPlayer, double range, double height) {
			this.player = player;
			this.dueTick = dueTick;
			this.damage = damage;
			this.area = area;
			this.followsPlayer = followsPlayer;
			this.range = range;
			this.height = height;
		}

		/** 以自身为中心的一块区域（结算时跟随自身位置）。 */
		static Pending aura(ServerPlayer player, int delay, float damage, double range, double height) {
			double mul = PlayerStats.rangeMultiplier(player);
			return new Pending(player, player.level().getGameTime() + delay, damage, null, true, range * mul, height * mul);
		}

		/** 沿突进路径的一条带状区域（释放时定格）。 */
		static Pending path(ServerPlayer player, Vec3 direction, double distance, int delay, float damage, double radius) {
			Vec3 start = player.position();
			Vec3 end = start.add(direction.scale(distance));
			double mul = PlayerStats.rangeMultiplier(player);
			AABB area = new AABB(start, end).inflate(radius * mul, 1.5D * mul, radius * mul);
			return new Pending(player, player.level().getGameTime() + delay, damage, area, false, 0.0D, 0.0D);
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			AABB box = this.followsPlayer
					? player.getBoundingBox().inflate(this.range, this.height, this.range)
					: this.area;
			List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box);
			boolean hitAnyone = false;

			for (LivingEntity victim : victims) {
				if (victim == player || victim.isAlliedTo(player)) {
					continue;
				}

				if (victim.hurtServer(level, level.damageSources().playerAttack(player), damage)) {
					hitAnyone = true;
				}
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, hitAnyone ? 1.0F : 0.8F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					player.getX(), player.getY(0.5D), player.getZ(), hitAnyone ? 3 : 1, 0.4D, 0.1D, 0.4D, 0.0D);
		}
	}
}
