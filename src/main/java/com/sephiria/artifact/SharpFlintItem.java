package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 尖锐的燧石（元素，高级品质）：【唯一】暴击几率 +2/4/6/8%、最高元素伤害 +1/2/3/4（0..3 级）。
 */
public class SharpFlintItem extends ArtifactItem {
	/** 各等级的暴击几率加成（百分点）：任何由玩家造成的伤害都吃。 */
	private static final double[] CRIT_CHANCE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级的「最高元素伤害」加成（点）：加到四项强度里最高的那一项上。 */
	private static final double[] HIGHEST_ELEMENT_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SharpFlintItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ELEMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sharp_flint.flavor";
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
	public double highestElementBonus(int level) {
		return valueAt(HIGHEST_ELEMENT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.highest_element",
						Component.literal(Numbers.format(this.highestElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
