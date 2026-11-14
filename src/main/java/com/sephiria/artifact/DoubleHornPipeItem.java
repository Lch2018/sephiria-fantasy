package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 双角管（学院，稀有，【唯一】）：三枚魔弹分三路发射（7° 夹角），
 * 每路伤害 -69/63/57/51/45/39%，魔弹 MP 消耗 +0/25/50/75/100/125%。
 */
public class DoubleHornPipeItem extends ArtifactItem {
	private static final double[] SPLIT_REDUCTION_BY_LEVEL = { 69.0D, 63.0D, 57.0D, 51.0D, 45.0D, 39.0D };
	private static final double[] MP_INCREASE_BY_LEVEL = { 0.0D, 25.0D, 50.0D, 75.0D, 100.0D, 125.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public DoubleHornPipeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.double_horn_pipe.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SPLIT_REDUCTION_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBoltSplitDamageReduction(int level) {
		return valueAt(SPLIT_REDUCTION_BY_LEVEL, level);
	}

	@Override
	public double magicBoltMpCostIncreasePercent(int level) {
		return valueAt(MP_INCREASE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_bolt_split",
						Component.literal(Numbers.format(this.magicBoltSplitDamageReduction(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_bolt_mp_increase",
						Component.literal(Numbers.format(this.magicBoltMpCostIncreasePercent(level)))
								.withColor(COLOUR_BONUS)));
	}
}
