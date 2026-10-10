package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷云追踪指南针（魔法科技，传说品质）：【唯一】闪电属性伤害 +2/4/6/8；
 * 触电施加时，有 15/30/45/60% 概率施加强化触电（0..3 级）。
 *
 * <p>「强化触电」= 施加触电时立即额外触发一次触电伤害：掷中就在挂层那一刻按周期跳的公式
 * 补结算一次（见 {@code Debuffs#applyShock}），不额外延长持续时间。
 */
public class StormCompassItem extends ArtifactItem {
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级的强化触电概率（%）。 */
	private static final double[] ENHANCED_CHANCE_BY_LEVEL = { 15.0D, 30.0D, 45.0D, 60.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public StormCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.storm_compass.flavor";
	}

	@Override
	public boolean unique() {
		return true;
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
	public double enhancedShockChance(int level) {
		return valueAt(ENHANCED_CHANCE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_element",
						Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.enhanced_shock",
						Component.literal(Numbers.format(this.enhancedShockChance(level))).withColor(COLOUR_BONUS)));
	}
}
