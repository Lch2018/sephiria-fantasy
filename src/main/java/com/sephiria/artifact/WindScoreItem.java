package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 乐谱「风」（风之歌，高级品质）：【唯一】近战攻击范围 +10/20/30/40/50%（0..4 级）。
 *
 * <p>加成的是「近战攻击范围」这个属性（百分比），所以普通攻击与技能的判定范围一起变大。
 */
public class WindScoreItem extends ArtifactItem {
	private static final double[] RANGE_BY_LEVEL = { 10.0D, 20.0D, 30.0D, 40.0D, 50.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WindScoreItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.wind_score.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return RANGE_BY_LEVEL.length - 1;
	}

	@Override
	public double meleeRangePercentBonus(int level) {
		return valueAt(RANGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.melee_range",
				Component.literal(Numbers.format(this.meleeRangePercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
