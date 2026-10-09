package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 石花（乌云，普通品质）：闪电属性伤害 +3/5、暴击伤害 +3/6%（0..1 级）。
 *
 * <p>没有【唯一】：带几件就叠几份。两条都是现成的词条钩子
 * （{@code lightningElementBonus} 并进电元素强度面板值，{@code critDamageBonus} 加在默认 150% 上）。
 */
public class StoneFlowerItem extends ArtifactItem {
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 3.0D, 5.0D };
	/** 各等级的暴击伤害加成（百分点）。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 3.0D, 6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public StoneFlowerItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.stone_flower.flavor";
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
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.lightning_element",
						Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
