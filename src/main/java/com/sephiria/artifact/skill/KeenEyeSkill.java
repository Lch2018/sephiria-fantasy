package com.sephiria.artifact.skill;

import com.sephiria.Sephiria;
import com.sephiria.artifact.KeenEyeItem;
import com.sephiria.stats.TimedAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 锐利之眼（魔法书「获得技能」）：暴击几率 +10/13/16%，持续 10/15/20 秒，冷却 30 秒。
 *
 * <p>暴击几率不是原版属性，挂不进临时修饰符，所以走 {@link TimedAttributes#grantStat}
 * 的「自定义加成」：只在本类里计时、到点自动消失；战斗结算与属性面板都从
 * {@code PlayerStats#critChanceTotal} 现查这个加成，不会漏也不会重复算。
 */
public final class KeenEyeSkill implements ArtifactSkill {
	public static final KeenEyeSkill INSTANCE = new KeenEyeSkill();

	/** 冷却 30 秒。 */
	private static final int COOLDOWN_TICKS = 600;
	/** 限时加成的来源 id：战斗与面板按它查当前加成。 */
	public static final Identifier CRIT_BONUS_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "keen_eye_crit");

	private KeenEyeSkill() {
	}

	@Override
	public String id() {
		return "keen_eye";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return KeenEyeItem.keenMpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		// 数值与时长都按神器等级现算：0 级最弱最短，2 级最强最久
		TimedAttributes.grantStat(player, CRIT_BONUS_ID, KeenEyeItem.critBonusPercent(level),
				KeenEyeItem.durationSeconds(level) * 20);

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
		serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
				30, 0.5D, 0.8D, 0.5D, 0.4D);
	}
}
