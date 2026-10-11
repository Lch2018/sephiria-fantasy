package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 睿智护符（冰川，普通品质）：【唯一】冰属性伤害 +3/4/6/8（0..3 级）。
 *
 * <p>冰元素强度直接进面板的固定值段（见 {@code PlayerStats#elementTotal} 的 ICE 一路），
 * 所以它同时抬高三样东西：冰元素的显示值、元素连击的「最高元素伤害」比较、以及冰冻那一下的结算伤害。
 * 风味文本按用户给的原样写着。
 */
public class WisdomAmuletItem extends ArtifactItem {
	/** 各等级的冰元素强度加成（点）。 */
	private static final double[] ICE_BY_LEVEL = { 3.0D, 4.0D, 6.0D, 8.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WisdomAmuletItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GLACIER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.wisdom_amulet.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ICE_BY_LEVEL.length - 1;
	}

	@Override
	public double iceElementBonus(int level) {
		return valueAt(ICE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.ice_element",
						Component.literal(Numbers.format(this.iceElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
