package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 六叶草（影子，传说品质）：【唯一】闪避 +3/6/9/12、幸运值 +3/6/9/12、
 * 暴击几率 +2/4/6/8%、无视防御伤害 +1/1/2/2（0..3 级）。
 *
 * <p>它是第一件给「幸运值」的神器：幸运值只影响稀有及以上物品的掉落概率
 * （商店 / 宝箱 / 抽卡），换算见 {@link com.sephiria.stats.PlayerStats} 顶部的曲线。
 */
public class SixLeafCloverItem extends ArtifactItem {
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D };
	/** 各等级的幸运值（点）。 */
	private static final double[] LUCK_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D };
	/** 各等级的暴击几率（%）：任何由玩家造成的伤害都吃。 */
	private static final double[] CRIT_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级的无视防御伤害（点）。 */
	private static final double[] IGNORE_DEFENSE_BY_LEVEL = { 1.0D, 1.0D, 2.0D, 2.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SixLeafCloverItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.six_leaf_clover.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DODGE_BY_LEVEL.length - 1;
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public double luckBonus(int level) {
		return valueAt(LUCK_BY_LEVEL, level);
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_BY_LEVEL, level);
	}

	@Override
	public double ignoreDefenseBonus(int level) {
		return valueAt(IGNORE_DEFENSE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.luck",
						Component.literal(Numbers.format(this.luckBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.ignore_defense",
						Component.literal(Numbers.format(this.ignoreDefenseBonus(level))).withColor(COLOUR_BONUS)));
	}
}
