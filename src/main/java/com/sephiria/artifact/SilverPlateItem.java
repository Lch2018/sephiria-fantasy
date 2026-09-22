package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 银盘子（坚固，普通品质）：【唯一】普通攻击伤害 +3/6/10%、特殊攻击伤害 +3/6/10%（0..2 级）。
 *
 * <p>两条词条正好把「普通攻击」与「武器技能」各加成一份：前者只进普通攻击的倍率，
 * 后者只进技能的倍率（见武器数据表第十三节）。
 */
public class SilverPlateItem extends ArtifactItem {
	/** 各等级的普通攻击伤害加成（百分点）。 */
	private static final double[] NORMAL_ATTACK_BY_LEVEL = { 3.0D, 6.0D, 10.0D };
	/** 各等级的特殊攻击伤害加成（百分点）。 */
	private static final double[] SPECIAL_ATTACK_BY_LEVEL = { 3.0D, 6.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SilverPlateItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.silver_plate.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return NORMAL_ATTACK_BY_LEVEL.length - 1;
	}

	@Override
	public double normalAttackDamagePercentBonus(int level) {
		return valueAt(NORMAL_ATTACK_BY_LEVEL, level);
	}

	@Override
	public double specialAttackBonus(int level) {
		return valueAt(SPECIAL_ATTACK_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.normal_attack_damage",
						Component.literal(Numbers.format(this.normalAttackDamagePercentBonus(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.special_attack",
						Component.literal(Numbers.format(this.specialAttackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
