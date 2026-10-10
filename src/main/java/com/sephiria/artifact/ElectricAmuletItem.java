package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 电击护符（魔法科技，普通品质）：闪电属性伤害 +3/4/6/8（0..3 级）。
 *
 * <p>没有【唯一】：带几件就叠几份（与克里顿的印章同型）。加成并进电元素强度的面板值，
 * 触电与各种闪电攻击都按面板值结算。
 */
public class ElectricAmuletItem extends ArtifactItem {
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 3.0D, 4.0D, 6.0D, 8.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ElectricAmuletItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.electric_amulet.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return LIGHTNING_BY_LEVEL.length - 1;
	}

	@Override
	public double lightningElementBonus(int level) {
		return valueAt(LIGHTNING_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.lightning_element",
				Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
