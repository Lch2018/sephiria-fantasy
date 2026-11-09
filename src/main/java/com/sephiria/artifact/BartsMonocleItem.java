package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 巴尔特的工作用单片眼镜（精密，传说品质）：【唯一】暴击几率 +10/19/28/35/45%；
 * 敌人距离 3 格以内时，暴击伤害 -30%。
 *
 * <p>减暴伤只在 {@code SephiriaDamage#applyCrit} 里按「攻击者与目标当前距离」判断，
 * 所以同一次攻击对远处目标仍是完整暴伤。
 */
public class BartsMonocleItem extends ArtifactItem {
	private static final double[] CRIT_CHANCE_BY_LEVEL = { 10.0D, 19.0D, 28.0D, 35.0D, 45.0D };
	/** 近战距离（格）与暴伤扣减（百分点）。 */
	public static final double CLOSE_RANGE = 3.0D;
	private static final double CLOSE_RANGE_CRIT_DAMAGE_PENALTY = 30.0D;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BartsMonocleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.barts_monocle.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_CHANCE_BY_LEVEL.length - 1;
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_CHANCE_BY_LEVEL, level);
	}

	@Override
	public double closeRangeCritDamagePenalty(int level) {
		return CLOSE_RANGE_CRIT_DAMAGE_PENALTY;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.barts_monocle_penalty",
						Component.literal(Numbers.format(CLOSE_RANGE_CRIT_DAMAGE_PENALTY)).withColor(COLOUR_BONUS)));
	}
}
