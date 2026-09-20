package com.sephiria.weapon;

import com.sephiria.Sephiria;
import com.sephiria.ability.Dash;
import com.sephiria.ability.Invulnerability;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * 长棍：轻快连击 + 右键的两段「回击」。
 *
 * <p><b>左键</b>：相对入鞘…相对<b>出鞘状态下的刀</b>——攻速慢 {@value #ATTACK_SPEED_SCALE_TEXT}、
 * 伤害高 {@value #DAMAGE_SCALE_TEXT}、横扫范围大 {@value #SWEEP_RANGE_SCALE_TEXT}。
 *
 * <p><b>右键·回击</b>（两段，自动衔接）：
 * <ol>
 *   <li>沿准星方向（含垂直分量）突进 {@value #STEP1_DISTANCE} 格 / {@value #STEP_DASH_TICKS} tick（0.1 秒），
 *       期间无敌 {@value #INVULNERABLE_TICKS} tick（0.3 秒，比位移多 0.2 秒）；
 *       位移结束后以自身为原点结算「回击 1」。</li>
 *   <li>无敌结束后获得<b>最终伤害减免 50%</b>，持续 {@value #REDUCTION_TICKS} tick（0.3 秒）。</li>
 *   <li>{@value #STEP2_DELAY_TICKS} tick（0.4 秒）后再向准星方向突进 {@value #STEP2_DISTANCE} 格，
 *       落地结算「回击 2」；若无敌期间挨到攻击（<b>完美防御</b>）则改为「回击 3」。</li>
 * </ol>
 *
 * <p>伤害与范围都以匕首的「招架」为基准：招架伤害 {@value #PARRY_DAMAGE}、范围
 * {@value #PARRY_RANGE}（水平半径，垂直 {@value #PARRY_HEIGHT}）。
 * 回击 1 = {@value #RIPOSTE1_DAMAGE} 点（与招架同伤害）、同范围；
 * 回击 2 = {@value #RIPOSTE2_DAMAGE} 点、半径 {@value #RIPOSTE2_RANGE} 格；
 * 回击 3 = {@value #RIPOSTE3_DAMAGE} 点、半径 {@value #RIPOSTE3_RANGE} 格。
 * 三段的伤害与范围都是绝对值，便于单独调平衡。
 */
public class SephiriaStaffItem extends Item implements SephiriaWeapon {
	/** 出鞘状态下的刀的基准值。 */
	private static final float KATANA_UNSHEATHED_DAMAGE = 5.0F;
	private static final float KATANA_UNSHEATHED_SPEED = 1.6F * 2.0F;
	private static final double KATANA_UNSHEATHED_SWEEP_RANGE = 1.0D;

	/** 左键相对出鞘刀的缩放：伤害 +5%、攻速 -10%、范围 +10%。 */
	private static final double DAMAGE_SCALE = 1.05D;
	private static final double ATTACK_SPEED_SCALE = 0.9D;
	private static final double SWEEP_RANGE_SCALE = 2.0D;
	private static final String DAMAGE_SCALE_TEXT = "+5%";
	private static final String ATTACK_SPEED_SCALE_TEXT = "10%";
	private static final String SWEEP_RANGE_SCALE_TEXT = "10%";

	/** 攻速目标值（供物品注册使用）。 */
	public static final float ATTACK_SPEED = 4.16F;
	/** 伤害目标值（供物品注册使用）。 */
	public static final float ATTACK_DAMAGE = 2.52F;

	/** 横扫基准（原版）与对其它目标的伤害系数。 */
	private static final double VANILLA_SWEEP_RANGE = 1.0D;
	private static final double VANILLA_SWEEP_HEIGHT = 0.25D;
	private static final double SWEEP_RATIO = 0.5D;
	private static final float SWEEP_CHARGE_THRESHOLD = 0.9F;

	/** 匕首「招架」的基准：伤害、水平半径、垂直半径。 */
	private static final float PARRY_DAMAGE = 8.0F;
	private static final double PARRY_RANGE = 2.0D * 1.8D;
	private static final double PARRY_HEIGHT = 0.5D * 1.8D;

	/** 回击三段的伤害与范围：都是绝对值（不是前一段的倍数），便于单独调平衡。 */
	private static final float RIPOSTE1_DAMAGE = 8.0F;
	private static final float RIPOSTE2_DAMAGE = 11.0F;
	private static final double RIPOSTE2_RANGE = 5.0D;
	private static final double RIPOSTE2_HEIGHT = RIPOSTE2_RANGE * 0.25D;
	private static final float RIPOSTE3_DAMAGE = 13.0F;
	private static final double RIPOSTE3_RANGE = 5.5D;
	private static final double RIPOSTE3_HEIGHT = RIPOSTE3_RANGE * 0.25D;

	/** 第一段位移与无敌、减免；第二段位移与出现时机。 */
	private static final double STEP1_DISTANCE = 1.0D;
	private static final double STEP2_DISTANCE = 2.0D;
	private static final int STEP_DASH_TICKS = 2;
	private static final int INVULNERABLE_TICKS = 6;
	private static final int REDUCTION_TICKS = 6;
	private static final int STEP2_DELAY_TICKS = 8;
	/** 技能冷却：1.8 秒。比整段（0.5 秒）长得多，避免连点把无敌无限续上。 */
	private static final int COOLDOWN_TICKS = 36;

	/** 回击期间"格挡窗口"的到期刻：窗口内挨打算完美防御（伤害由本类自己取消）。 */
	private static final Map<UUID, Long> GUARD_UNTIL = new ConcurrentHashMap<>();
	/** 最终伤害减免的到期刻：无敌结束后生效。 */
	private static final Map<UUID, Long> REDUCTION_UNTIL = new ConcurrentHashMap<>();
	/** 本次回击是否已经判定为完美防御。 */
	private static final Set<UUID> PERFECT = ConcurrentHashMap.newKeySet();
	/** 待结算的范围伤害与后续段。 */
	private static final List<Pending> PENDING = new ArrayList<>();

	private final WeaponBranch branch;

	public SephiriaStaffItem(WeaponBranch branch, Item.Properties properties) {
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
				Line.attack(ATTACK_DAMAGE, ATTACK_SPEED, VANILLA_SWEEP_RANGE * SWEEP_RANGE_SCALE).build(),
				Line.titled("tooltip.sephiria.skill.riposte1")
						.damage(RIPOSTE1_DAMAGE).cooldownTicks(COOLDOWN_TICKS)
						.range(PARRY_RANGE).distance(STEP1_DISTANCE).build(),
				Line.titled("tooltip.sephiria.skill.riposte2")
						.damage(RIPOSTE2_DAMAGE).cooldownSame()
						.range(RIPOSTE2_RANGE).distance(STEP2_DISTANCE).build(),
				Line.titled("tooltip.sephiria.skill.riposte3")
						.damage(RIPOSTE3_DAMAGE).cooldownSame().range(RIPOSTE3_RANGE).build(),
				desc("tooltip.sephiria.skill.riposte", "tooltip.sephiria.desc.riposte"));
	}


	/** 注册横扫判定、回击判定与推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SephiriaStaffItem::tick);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!(stack.getItem() instanceof SephiriaStaffItem)) {
				return InteractionResult.PASS;
			}

			if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			if (serverPlayer.getAttackStrengthScale(0.5F) <= SWEEP_CHARGE_THRESHOLD
					|| serverPlayer.isSprinting() || !serverPlayer.onGround()) {
				return InteractionResult.PASS;
			}

			sweep(serverPlayer, serverLevel, entity);
			return InteractionResult.PASS;
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && isGuarding(player)) {
				// 完美防御：无敌窗口里挨到攻击。伤害在这里直接取消（不依赖其它无敌实现的注册顺序）
				PERFECT.add(player.getUUID());
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.5F);
				return false;
			}

			return true;
		});
	}

	/** 左键横扫：参数按棍相对出鞘刀的比例。 */
	private static void sweep(ServerPlayer player, ServerLevel level, Entity target) {
		double rangeMul = PlayerStats.rangeMultiplier(player);
		double range = VANILLA_SWEEP_RANGE * SWEEP_RANGE_SCALE * rangeMul;
		double height = VANILLA_SWEEP_HEIGHT * SWEEP_RANGE_SCALE * rangeMul;
		float mainDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float sweepDamage = 1.0F + (float) (SWEEP_RATIO * mainDamage);
		AABB area = target.getBoundingBox().inflate(range, height, range);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim == target || victim.isAlliedTo(player)) {
				continue;
			}

			if (victim.hurtServer(level, level.damageSources().playerAttack(player), sweepDamage)) {
				victim.knockback(0.4D, player.getX() - victim.getX(), player.getZ() - victim.getZ(),
						level.damageSources().playerAttack(player), sweepDamage);
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.1F);
	}

	// ------------------------------------------------------------------ 右键：回击

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
		long now = player.level().getGameTime();

		PERFECT.remove(serverPlayer.getUUID());
		GUARD_UNTIL.put(serverPlayer.getUUID(), now + INVULNERABLE_TICKS);
		REDUCTION_UNTIL.put(serverPlayer.getUUID(), now + INVULNERABLE_TICKS + REDUCTION_TICKS);
		Invulnerability.grant(serverPlayer, INVULNERABLE_TICKS);
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

		Vec3 direction = player.getLookAngle();
		Dash.startVertical(serverPlayer, direction, STEP1_DISTANCE, STEP_DASH_TICKS, Dash.DEFAULT_DECAY);
		serverPlayer.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);

		double step1Damage = RIPOSTE1_DAMAGE * PlayerStats.damageMultiplier(serverPlayer);
		PENDING.add(Pending.aura(serverPlayer, STEP_DASH_TICKS, (float) step1Damage, PARRY_RANGE, PARRY_HEIGHT));
		PENDING.add(Pending.followUp(serverPlayer, PlayerStats.damageMultiplier(serverPlayer), STEP2_DELAY_TICKS));
		return InteractionResult.CONSUME;
	}

	/** 无敌是否已结束、减免是否还在窗口内（供伤害减免的 mixin 查询）。 */
	public static float damageMultiplier(LivingEntity entity) {
		Long until = REDUCTION_UNTIL.get(entity.getUUID());

		if (until == null || entity.level().getGameTime() >= until) {
			return 1.0F;
		}

		Long guard = GUARD_UNTIL.get(entity.getUUID());

		// 无敌期间伤害本来就被取消，这里只处理无敌结束后的那一段
		if (guard != null && entity.level().getGameTime() < guard) {
			return 1.0F;
		}

		return 0.5F;
	}

	private static boolean isGuarding(ServerPlayer player) {
		Long until = GUARD_UNTIL.get(player.getUUID());
		return until != null && player.level().getGameTime() < until;
	}

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		GUARD_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);
		REDUCTION_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);

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

	/** 待结算的一段：范围伤害，或第二段回击的触发。 */
	private static final class Pending {
		private final ServerPlayer player;
		private final long dueTick;
		private final float damage;
		private final double range;
		private final double height;
		/**
		 * 这一段是不是「第二段回击的触发」。
		 *
		 * <p>必须是一个明确的布尔值：早先用 followUpMultiplier 的正负当标记，而范围伤害那段的
		 * 标记值恰好也满足判定，于是每段范围伤害都会当成第二段触发，触发出来的第二段又排一段
		 * 范围伤害——自我循环，玩家会一直无冷却地突进斩击。
		 */
		private final boolean followUp;
		private final double followUpMultiplier;

		private Pending(ServerPlayer player, long dueTick, float damage, double range, double height,
				boolean followUp, double followUpMultiplier) {
			this.player = player;
			this.dueTick = dueTick;
			this.damage = damage;
			this.range = range;
			this.height = height;
			this.followUp = followUp;
			this.followUpMultiplier = followUpMultiplier;
		}

		/** 以自身为原点的范围伤害（结算时取自身位置，所以打的是落点）。 */
		static Pending aura(ServerPlayer player, int delay, float damage, double range, double height) {
			return new Pending(player, player.level().getGameTime() + delay, damage, range, height, false, 1.0D);
		}

		/**
		 * 第二段回击：再突进一段并结算更大的范围。
		 *
		 * <p>它结算时只排「范围伤害」，绝不再排第二段——否则两段互相触发就是死循环。
		 */
		static Pending followUp(ServerPlayer player, double damageMultiplier, int delay) {
			return new Pending(player, player.level().getGameTime() + delay, 0.0F, 0.0D, 0.0D, true, damageMultiplier);
		}

		void resolve() {
			if (player.isRemoved() || !(player.level() instanceof ServerLevel level)) {
				return;
			}

			if (this.followUp) {
				secondStep(level, this.followUpMultiplier);
				return;
			}

			AABB area = player.getBoundingBox().inflate(this.range, this.height, this.range);

			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
				if (victim == player || victim.isAlliedTo(player)) {
					continue;
				}

				victim.hurtServer(level, level.damageSources().playerAttack(player), this.damage);
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.9F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					player.getX(), player.getY(0.5D), player.getZ(), 6, 1.0D, 0.2D, 1.0D, 0.0D);
		}

		/** 第二段：完美防御则换成回击 3，否则回击 2。 */
		private void secondStep(ServerLevel level, double damageMultiplier) {
			boolean perfect = PERFECT.remove(player.getUUID());
			double damage = perfect
					? RIPOSTE3_DAMAGE * damageMultiplier
					: RIPOSTE2_DAMAGE * damageMultiplier;
			double range = perfect ? RIPOSTE3_RANGE : RIPOSTE2_RANGE;
			double height = perfect ? RIPOSTE3_HEIGHT : RIPOSTE2_HEIGHT;

			Dash.startVertical(player, player.getLookAngle(), STEP2_DISTANCE, STEP_DASH_TICKS, Dash.DEFAULT_DECAY);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, perfect ? 0.7F : 1.0F);
			PENDING.add(Pending.aura(player, STEP_DASH_TICKS, (float) damage, range, height));
		}
	}
}
