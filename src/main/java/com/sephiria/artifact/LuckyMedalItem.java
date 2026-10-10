package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 幸运的奖章（谈判，高级品质）：【唯一】在商店每买一瓶药水时生成 +100/150/200 叶子、
 * 谈判力 +2/6/10（0..2 级）。
 *
 * <p>买药水生成的叶子走 {@code PlayerStats#grantLeaves}——它不是经验，不推进
 * 「每 1000 经验一个升级宝箱」的计数。
 */
public class LuckyMedalItem extends ArtifactItem {
	/** 各等级每买一瓶药水生成的叶子。 */
	private static final double[] POTION_BUY_LEAVES_BY_LEVEL = { 100.0D, 150.0D, 200.0D };
	/** 各等级的谈判力加成（点）。 */
	private static final double[] NEGOTIATION_BY_LEVEL = { 2.0D, 6.0D, 10.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LuckyMedalItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.NEGOTIATION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.lucky_medal.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return NEGOTIATION_BY_LEVEL.length - 1;
	}

	@Override
	public double negotiationBonus(int level) {
		return valueAt(NEGOTIATION_BY_LEVEL, level);
	}

	@Override
	public double potionBuyLeafBonus(int level) {
		return valueAt(POTION_BUY_LEAVES_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.potion_buy_leaves",
						Component.literal(Numbers.format(this.potionBuyLeafBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.negotiation",
						Component.literal(Numbers.format(this.negotiationBonus(level))).withColor(COLOUR_BONUS)));
	}
}
