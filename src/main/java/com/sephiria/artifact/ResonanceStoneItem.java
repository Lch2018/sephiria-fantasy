package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 共鸣石（元素，传说品质）：【唯一】最高元素伤害 +8/10/12/14（0..3 级）。
 *
 * <p>纯「最高元素伤害」的单词条神器：它本身不参与战斗倍率，数值全部堆给四项强度里
 * 最高的那一项——是元素连击的核心件。
 */
public class ResonanceStoneItem extends ArtifactItem {
	/** 各等级的「最高元素伤害」加成（点）：加到四项强度里最高的那一项上。 */
	private static final double[] HIGHEST_ELEMENT_BY_LEVEL = { 8.0D, 10.0D, 12.0D, 14.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ResonanceStoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ELEMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.resonance_stone.flavor";
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
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.highest_element",
						Component.literal(Numbers.format(this.highestElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
