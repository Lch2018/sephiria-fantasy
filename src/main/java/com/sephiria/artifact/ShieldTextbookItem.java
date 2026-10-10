package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 盾牌术教材（坚固，普通品质）：特殊攻击伤害 +5% / +8%（0 / 1 级）。
 *
 * <p><b>没有【唯一】</b>：背包里放两本，两本的词条都会生效（特殊攻击伤害 +13%），
 * 而且坚固连击也 +2 级——这正是设计上要的效果，所以这里 unique() 返回 false。
 */
public class ShieldTextbookItem extends ArtifactItem {
	/** 各等级的特殊攻击伤害加成（百分点）。 */
	private static final double[] SPECIAL_BY_LEVEL = { 5.0D, 8.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ShieldTextbookItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.shield_textbook.flavor";
	}

	@Override
	public boolean unique() {
		// 设计上就是可以带多本、每本都生效
		return false;
	}

	@Override
	public int maxLevel() {
		return SPECIAL_BY_LEVEL.length - 1;
	}

	@Override
	public double specialAttackBonus(int level) {
		return valueAt(SPECIAL_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.special_attack",
				Component.literal(Numbers.format(this.specialAttackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
