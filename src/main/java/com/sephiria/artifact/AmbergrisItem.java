package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 龙涎香（余烬，高级品质）：火焰属性伤害 +2/4/6/8/10（0..4 级）。
 *
 * <p>没有【唯一】：带几件就叠几份。「火焰属性伤害」与「火元素强度」是同一个属性，
 * 并进面板值后太阳剑与「灼伤」每跳都按它换算（见 {@code PlayerStats#elementTotal}）。
 */
public class AmbergrisItem extends ArtifactItem {
	/** 各等级的「火元素强度」加成（点）。 */
	private static final double[] FIRE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public AmbergrisItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.ambergris.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return FIRE_BY_LEVEL.length - 1;
	}

	@Override
	public double fireElementBonus(int level) {
		return valueAt(FIRE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.fire_element",
						Component.literal(Numbers.format(this.fireElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
