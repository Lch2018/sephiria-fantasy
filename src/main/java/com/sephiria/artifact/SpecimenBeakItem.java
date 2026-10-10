package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 标本喙（影子，稀有品质）：【唯一】物理伤害 +1/2/3/5/8、闪避 +2/3/4/5/6（0..4 级）。
 */
public class SpecimenBeakItem extends ArtifactItem {
	/** 各等级的物理强度加成。 */
	private static final double[] PHYSICAL_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 5.0D, 8.0D };
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 2.0D, 3.0D, 4.0D, 5.0D, 6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SpecimenBeakItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.specimen_beak.flavor";
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
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.physical",
						Component.literal(Numbers.format(this.physicalBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
