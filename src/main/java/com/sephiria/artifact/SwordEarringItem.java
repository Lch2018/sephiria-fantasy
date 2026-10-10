package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 剑耳环（精密，稀有品质）：【唯一】暴击伤害 +10/16/24/33%，但最大蓝量 −16/13/10/7（0..3 级）。
 *
 * <p>最大蓝量是负词条：上限 = 50 + 各来源加成（这里给的是负数），见 {@code PlayerStats#maxMpTotal}。
 * 已经超出的蓝不会被强行扣掉，只是回不上去——花掉之后按新上限回。
 */
public class SwordEarringItem extends ArtifactItem {
	/** 各等级的暴击伤害加成（百分点），加在默认的 150% 上。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 10.0D, 16.0D, 24.0D, 33.0D };
	/** 各等级的最大蓝量加成（点）：负数，削上限——词条表里就存负值，聚合与显示都不用再翻符号。 */
	private static final double[] MAX_MP_BY_LEVEL = { -16.0D, -13.0D, -10.0D, -7.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SwordEarringItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sword_earring.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double maxMpBonus(int level) {
		return valueAt(MAX_MP_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
						Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
