package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 索利斯·弗拉克托（太阳剑，普通品质）：火焰属性伤害 +2/3/5、最大生命值 +2/3/4（0..2 级）。
 *
 * <p>没有【唯一】：带几件就叠几份。「火焰属性伤害」与「火元素强度」是同一个属性，
 * 并进面板值后太阳剑按它换算伤害；「最大生命值」由 {@code stats/StatAttributes}
 * 挂成原版属性修饰符。
 */
public class SolisFractoItem extends ArtifactItem {
	/** 各等级的「火元素强度」加成（点）。 */
	private static final double[] FIRE_BY_LEVEL = { 2.0D, 3.0D, 5.0D };
	/** 各等级的「最大生命值」加成（点）。 */
	private static final double[] MAX_HP_BY_LEVEL = { 2.0D, 3.0D, 4.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SolisFractoItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.solis_fracto.flavor";
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
	public double maxHpBonus(int level) {
		return valueAt(MAX_HP_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.fire_element",
						Component.literal(Numbers.format(this.fireElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.max_hp",
						Component.literal(Numbers.format(this.maxHpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
