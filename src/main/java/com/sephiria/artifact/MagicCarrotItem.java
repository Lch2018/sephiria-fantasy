package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 魔法胡萝卜（元素，高级品质）：【唯一】最高元素伤害 +2/3/4/6/8、移动速度 +2/4/6/8/10%（0..4 级）。
 *
 * <p>「最高元素伤害」加到物理/火/冰/电四项强度里数值最高的那一项上，见 {@code PlayerStats#highestElement}。
 */
public class MagicCarrotItem extends ArtifactItem {
	/** 各等级的「最高元素伤害」加成（点）。 */
	private static final double[] HIGHEST_ELEMENT_BY_LEVEL = { 2.0D, 3.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级的移动速度加成（百分点，100 = 100%）。 */
	private static final double[] MOVE_SPEED_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MagicCarrotItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.magic_carrot.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return HIGHEST_ELEMENT_BY_LEVEL.length - 1;
	}

	@Override
	public double highestElementBonus(int level) {
		return valueAt(HIGHEST_ELEMENT_BY_LEVEL, level);
	}

	@Override
	public double moveSpeedPercentBonus(int level) {
		return valueAt(MOVE_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.highest_element",
						Component.literal(Numbers.format(this.highestElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.move_speed",
						Component.literal(Numbers.format(this.moveSpeedPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
