package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 火红的夕阳（太阳剑，稀有品质）：【唯一】太阳剑投掷时有 20/40/60/80/100% 概率「增大」——
 * 那一发伤害 ×1.33、划痕也画得更长（0..4 级）。
 *
 * <p>投掷概率与倍率都在 {@code sun.SunSword#throwSword} 里结算；这类词条走
 * {@code ArtifactEffects#sunSwordEnlargeChance} 求和。
 */
public class CrimsonSunsetItem extends ArtifactItem {
	/** 各等级的投掷时「增大」的概率（%）。 */
	private static final double[] CHANCE_BY_LEVEL = { 20D, 40D, 60D, 80D, 100D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public CrimsonSunsetItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.crimson_sunset.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CHANCE_BY_LEVEL.length - 1;
	}

	@Override
	public double sunSwordEnlargeChance(int level) {
		return valueAt(CHANCE_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_enlarge",
						Component.literal(Numbers.format(this.sunSwordEnlargeChance(level))).withColor(COLOUR_BONUS)));
	}
}
