package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 克里顿的印章（谈判，普通品质）：叶子获得量 +10/25/50%（0..2 级）。
 *
 * <p>没有【唯一】，多枚各自结算——叶子获得量是加算的百分点，
 * 见 {@code PlayerStats#leafGainPercentTotal}。
 */
public class CrittonsSealItem extends ArtifactItem {
	/** 各等级的叶子获得量加成（百分点，100 = 100%）。 */
	private static final double[] LEAF_GAIN_BY_LEVEL = { 10.0D, 25.0D, 50.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public CrittonsSealItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.NEGOTIATION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.crittons_seal.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return LEAF_GAIN_BY_LEVEL.length - 1;
	}

	@Override
	public double leafGainPercentBonus(int level) {
		return valueAt(LEAF_GAIN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria.affix.leaf_gain",
				Component.literal(Numbers.format(this.leafGainPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
