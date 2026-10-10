package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 避雷针（乌云，传说品质）：【唯一】乌云容量 +5/10/15/20/25；战斗中乌云恢复速度 +3/6/9/12/15%；
 * 闪电属性伤害 +1/2/3/4/5（0..4 级）。
 *
 * <p>「战斗中乌云恢复速度」只放大战斗中那条回复线（每 5 秒回上限的 5% × (1 + 加成)）；
 * 脱战那条线（每 0.1 秒 5%）不受影响——脱战本来就回得飞快。
 */
public class LightningRodItem extends ArtifactItem {
	/** 各等级的乌云容量加成（点）。 */
	private static final int[] CAPACITY_BY_LEVEL = { 5, 10, 15, 20, 25 };
	/** 各等级的「战斗中乌云恢复速度」加成（%）。 */
	private static final double[] COMBAT_REGEN_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D, 15.0D };
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D, 5.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LightningRodItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.lightning_rod.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CAPACITY_BY_LEVEL.length - 1;
	}

	@Override
	public int cloudCapacityBonus(int level) {
		return CAPACITY_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, CAPACITY_BY_LEVEL.length - 1)];
	}

	@Override
	public double cloudCombatRegenPercent(int level) {
		return valueAt(COMBAT_REGEN_BY_LEVEL, level);
	}

	@Override
	public double lightningElementBonus(int level) {
		return valueAt(LIGHTNING_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_capacity",
						Component.literal(Numbers.format(this.cloudCapacityBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_combat_regen",
						Component.literal(Numbers.format(this.cloudCombatRegenPercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_element",
						Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
