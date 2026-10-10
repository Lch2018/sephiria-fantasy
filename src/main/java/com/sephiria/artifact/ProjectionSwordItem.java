package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 阿格玛投影剑190,191号（坚固，稀有品质）：【唯一】物理伤害 +2/3/4/6/8/10/12/14（0..7 级）。
 *
 * <p>8 档，是目前等级上限最高的神器——对应「投影剑」在原作里可以反复强化。
 */
public class ProjectionSwordItem extends ArtifactItem {
	private static final double[] PHYSICAL_BY_LEVEL = {
			2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 10.0D, 12.0D, 14.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ProjectionSwordItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.projection_sword.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return PHYSICAL_BY_LEVEL.length - 1;
	}

	@Override
	public double physicalBonus(int level) {
		return valueAt(PHYSICAL_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.physical",
				Component.literal(Numbers.format(this.physicalBonus(level))).withColor(COLOUR_BONUS)));
	}
}
