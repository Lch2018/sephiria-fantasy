package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Dash;
import com.sephiria.ability.Invulnerability;
import com.sephiria.ability.SkillStorage;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 *       无敌窗口内挨到攻击判定为<b>招架成功</b>，获得一层专注——判定写在 {@link #register()}
 *       的伤害拦截里，所以它必须注册在 {@link Invulnerability} 之前（Fabric 的事件按注册顺序
 *       派发，后者会把伤害直接吃掉、不再往后传）。</li>
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

	/** 两段位移的距离与无敌时长。 */
	private static final double PARRY_DISTANCE = 1.5D;
	private static final double FURY_DISTANCE = 5.0D;
	private static final int DASH_TICKS = 4;
	private static final int INVULNERABLE_TICKS = 4;

	/** 招架的范围（横扫之刃 II = 原版横扫的两倍）与伤害。 */
	private static final double PARRY_RANGE = 2.0D;
	private static final double PARRY_HEIGHT = 0.5D;
	private static final double PARRY_MAX_DISTANCE = 6.0D;
	private static final float PARRY_DAMAGE = 8.0F;

	/** 狂怒：路径两侧的判定半径与伤害。 */
	private static final double FURY_PATH_RADIUS = 1.5D;
	private static final float FURY_DAMAGE = 14.0F;

	/** 冷却（tick）：0.8 秒 / 0.2 秒。 */
	private static final int PARRY_COOLDOWN = 16;
	private static final int FURY_COOLDOWN = 4;

	/** 招架窗口的到期刻：窗口内挨打就算招架成功。 */
	private static final Map<UUID, Long> PARRY_UNTIL = new ConcurrentHashMap<>();
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

	/** 注册专注存储、结算推进器与招架判定（必须早于 {@link Invulnerability#register()}）。 */
	public static void register() {
		SkillStorage.registerManual(FOCUS, FOCUS_MAX);
		ServerTickEvents.END_SERVER_TICK.register(SephiriaDaggerItem::tick);

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isParrying(player)) {
				// 招架成功：给一层专注，并结束这次招架窗口（一次招架只算一次）
				PARRY_UNTIL.remove(player.getUUID());
				SkillStorage.regenerate(player, FOCUS, FOCUS_MAX);
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
			}
			return true;   // 伤害拦不拦由 Invulnerability 决定
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

		PENDING.add(Pending.aura(player, DASH_TICKS, PARRY_DAMAGE, PARRY_RANGE, PARRY_HEIGHT, PARRY_MAX_DISTANCE));
	}

	/** 狂怒：长突进 + 无敌，落地后结算突进路径上的范围伤害，并消耗一层专注。 */
	private static void fury(ServerPlayer player, ItemStack stack) {
		Vec3 direction = lookDirection(player);
		Invulnerability.grant(player, INVULNERABLE_TICKS);
		Dash.startVertical(player, direction, FURY_DISTANCE, DASH_TICKS, Dash.DEFAULT_DECAY);
		player.getCooldowns().addCooldown(stack, FURY_COOLDOWN);

		SkillStorage.consume(player, FOCUS, 1.0D);
		PENDING.add(Pending.path(player, direction, FURY_DISTANCE, DASH_TICKS, FURY_DAMAGE, FURY_PATH_RADIUS));
	}

	/** 完整的准星朝向：匕首沿真正的视线突进，抬头会向上冲、低头会向下冲。 */
	private static Vec3 lookDirection(ServerPlayer player) {
		return player.getLookAngle();
	}

	private static boolean isParrying(ServerPlayer player) {
		Long until = PARRY_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	private static void tick(MinecraftServer server) {
		PARRY_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= 0);

		if (PENDING.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		Iterator<Pending> iterator = PENDING.iterator();

		while (iterator.hasNext()) {
			Pending pending = iterator.next();

			if (pending.dueTick > now) {
				continue;
			}

			iterator.remove();
			pending.resolve();
		}
	}

	/** 一次待结算的范围伤害；位置在施放时定格，所以位移结束后仍然准确。 */
	private static final class Pending {
		private final ServerPlayer player;
		private final Vec3 origin;
		private final Vec3 direction;
		private final long dueTick;
		private final float damage;
		private final AABB area;

		private Pending(ServerPlayer player, Vec3 origin, Vec3 direction, long dueTick, float damage, AABB area) {
			this.player = player;
			this.origin = origin;
			this.direction = direction;
			this.dueTick = dueTick;
			this.damage = damage;
			this.area = area;
		}

		/** 以自身为中心的一块区域。 */
		static Pending aura(ServerPlayer player, int delay, float damage, double range, double height, double maxDistance) {
			AABB area = player.getBoundingBox().inflate(range, height, range);
			return new Pending(player, player.position(), Vec3.ZERO, player.level().getGameTime() + delay, damage, area);
		}

		/** 沿突进路径的一条带状区域。 */
		static Pending path(ServerPlayer player, Vec3 direction, double distance, int delay, float damage, double radius) {
			Vec3 start = player.position();
			Vec3 end = start.add(direction.scale(distance));
			AABB area = new AABB(start, end).inflate(radius, 1.5D, radius);
			return new Pending(player, start, direction, player.level().getGameTime() + delay, damage, area);
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area);
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
