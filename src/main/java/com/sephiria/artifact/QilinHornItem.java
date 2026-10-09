package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 麒麟的角（魔法科技，稀有品质）：【唯一】电属性攻击的暴击几率 +10/20/30%（也适用于同伴伤害）；
 * 冰属性伤害 −2/−4/−6（0..2 级）。
 *
 * <p>「电属性攻击」在实现里是 {@code Kind.ELECTRIC} 那一类伤害（触电结算、附加闪电伤害、
 * 桑德耳环的闪电攻击）；魔法书那种通用伤害走通用暴击几率，不吃这一项。
 * 「同伴伤害」的同伴系统还没做，文案先按用户给的原样写着。
 *
 * <p>冰属性是负词条：直接进冰元素强度的固定值段（下限 0），见 {@code PlayerStats#elementTotal}。
 */
public class QilinHornItem extends ArtifactItem {
	/** 各等级的电属性暴击几率加成（百分点）。 */
	private static final double[] ELECTRIC_CRIT_BY_LEVEL = { 10.0D, 20.0D, 30.0D };
	/** 各等级的冰元素强度加成（点）：负数，词条表里就存负值，聚合与显示都不用再翻符号。 */
	private static final double[] ICE_BY_LEVEL = { -2.0D, -4.0D, -6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public QilinHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.qilin_horn.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ELECTRIC_CRIT_BY_LEVEL.length - 1;
	}

	@Override
	public double electricCritChanceBonus(int level) {
		return valueAt(ELECTRIC_CRIT_BY_LEVEL, level);
	}

	@Override
	public double iceElementBonus(int level) {
		return valueAt(ICE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.electric_crit",
						Component.literal(Numbers.format(this.electricCritChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.ice_element",
						Component.literal(Numbers.format(this.iceElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
