package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 闪烁的眼睛（影子 + 精密，羁绊品质）：【唯一】暴击时给「海市蜃楼」充能；充到 100% 时，
 * 下一次受到的伤害被无条件闪避，并以自身为中心 8 格球形范围释放混沌伤害。
 *
 * <p><b>双连击</b>神器：同时给影子与精密各加 1 级（见 {@link #combos()}）。
 *
 * <p>海市蜃楼：每次暴击默认 +1%，至少 +1%；每 0.1 秒没有获得层数，下次暴击的增量 +1%
 * （上限 10%）；反之（0.1 秒内又暴击）增量 -2%（下限 1%）。满 100% 后不再涨，等一次伤害触发。
 *
 * <p>混沌伤害 = 被闪避那一下的<b>税前伤害</b> × 150%（见 {@link #onIncomingDamage}），
 * 视为物理 / 火 / 冰 / 电四属性；这里只额外吃「物理伤害增幅」这一条通用物理加成，
 * 其余元素加成在税前伤害里已经算过了。
 */
public class FlickeringEyeItem extends ArtifactItem {
	/** 海市蜃楼满值。 */
	private static final double FULL = 100.0D;
	/** 触发时的混沌伤害倍率（%）：被闪避那一下的税前伤害 × 它。 */
	private static final double DAMAGE_PERCENT = 150.0D;
	/** 外圈半径（格）：混沌伤害与黑色特效。 */
	private static final double RADIUS = 8.0D;
	/** 内圈半径（格）：金色特效。 */
	private static final double INNER_RADIUS = 2.0D;
	/** 每 0.1 秒 = 2 tick。 */
	private static final int GAIN_STEP_TICKS = 2;
	/** 暴击增量的上下限（%）。 */
	private static final double MIN_GAIN = 1.0D;
	private static final double MAX_GAIN = 10.0D;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 金色 / 黑色粒子。 */
	private static final int GOLD = 0xFFD24A;
	private static final int BLACK = 0x101010;

	private static final Map<UUID, State> STATES = new HashMap<>();

	public FlickeringEyeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	/** 双连击：影子与精密各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.SHADOW, ArtifactCombo.PRECISION);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.flickering_eye.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 0;
	}

	@Override
	public boolean flickeringEye(int level) {
		return true;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component charge = Component.literal(Numbers.format(FULL)).withColor(COLOUR_BONUS);
		Component damage = Component.literal(Numbers.format(DAMAGE_PERCENT)).withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.flickering_eye_charge", charge),
				Component.translatable("artifact.sephiria_fantasy.affix.flickering_eye_damage", damage));
	}

	/** 暴击时充能（由 {@link SephiriaDamage#applyCrit} 在暴击成功后调用）。 */
	public static void onCrit(ServerPlayer player) {
		if (!ArtifactEffects.flickeringEye(player)) {
			return;
		}

		State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
		long now = player.level().getGameTime();

		if (state.lastGainTick >= 0L) {
			long elapsed = now - state.lastGainTick;

			if (elapsed >= GAIN_STEP_TICKS) {
				state.gain = Math.min(MAX_GAIN, state.gain + (double) (elapsed / GAIN_STEP_TICKS));
			} else {
				state.gain = Math.max(MIN_GAIN, state.gain - 2.0D);
			}
		}

		state.mirage = Math.min(FULL, state.mirage + state.gain);
		state.lastGainTick = now;

		boolean reachedFull = state.mirage >= FULL;

		if (reachedFull && state.announcedFull < 0L) {
			state.announcedFull = now;

			if (player.level() instanceof ServerLevel level) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
			}
		}
	}

	/**
	 * 受到伤害前的检查：海市蜃楼满了就闪避这一次，并释放混沌伤害。
	 *
	 * @param amount 这一下在进入护甲 / 减伤之前的值（税前伤害）
	 * @return true 表示这次伤害已被闪避、调用方应取消原伤害
	 */
	public static boolean onIncomingDamage(ServerPlayer player, float amount) {
		if (amount <= 0.0F) {
			return false;
		}

		State state = STATES.get(player.getUUID());

		if (state == null || state.mirage < FULL || !ArtifactEffects.flickeringEye(player)) {
			return false;
		}

		// 先清空再打伤害：混沌伤害自己暴击时不会再充能，也不会递归触发闪避
		state.mirage = 0.0D;
		state.gain = MIN_GAIN;
		state.lastGainTick = -1L;
		state.announcedFull = -1L;

		if (!(player.level() instanceof ServerLevel level)) {
			return true;
		}

		double multiplier = 1.0D + (PlayerStats.physicalAmpPercent(player)
				+ ArtifactEffects.comboAllElementPercent(player)) / 100.0D;
		float chaos = (float) (amount * DAMAGE_PERCENT / 100.0D * multiplier);
		Vec3 center = player.position();

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS))) {
			if (!SephiriaDamage.autoTargetable(victim, player)
					|| victim.position().distanceToSqr(center) > RADIUS * RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(level, SephiriaDamage.chaos(level, player), chaos);
		}

		playSphere(level, center);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
		return true;
	}

	/** 海市蜃楼的充能值（0..100）；没带这件神器返回 -1。 */
	public static double charge(ServerPlayer player) {
		if (!ArtifactEffects.flickeringEye(player)) {
			return -1.0D;
		}

		State state = STATES.get(player.getUUID());
		return state == null ? 0.0D : state.mirage;
	}

	/** 以玩家为中心画一个黑金球：内圈 2 格金色，外圈 8 格黑色。 */
	private static void playSphere(ServerLevel level, Vec3 center) {
		RandomSource random = level.getRandom();
		Vec3 at = center.add(0.0D, 1.0D, 0.0D);

		for (int index = 0; index < 180; index++) {
			double radius = Math.cbrt(random.nextDouble()) * RADIUS;
			double theta = random.nextDouble() * Math.PI * 2.0D;
			double phi = Math.acos(2.0D * random.nextDouble() - 1.0D);
			double sinPhi = Math.sin(phi);
			double x = radius * sinPhi * Math.cos(theta);
			double y = radius * Math.cos(phi);
			double z = radius * sinPhi * Math.sin(theta);
			int colour = radius <= INNER_RADIUS ? GOLD : BLACK;
			float scale = radius <= INNER_RADIUS ? 1.3F : 1.0F;
			level.sendParticles(new DustParticleOptions(colour, scale),
					at.x + x, at.y + y, at.z + z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}

		level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 0.82F, 0.29F),
				at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	/** 每名玩家的海市蜃楼状态（运行时，不进存档）。 */
	private static final class State {
		private double mirage;
		private double gain = MIN_GAIN;
		private long lastGainTick = -1L;
		/** 满层提示音只响一次；触发闪避时清回 -1。 */
		private long announcedFull = -1L;
	}
}
