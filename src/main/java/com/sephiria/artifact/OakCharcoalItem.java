package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 橡木炭（余烬，高级品质）：灼伤的攻击速度 +10/20/30/40/50%（0..4 级）。
 *
 * <p>没有【唯一】：带几件就叠几份。压短的是「灼伤」每跳的间隔（见 {@code Debuffs#burnIntervalTicks}）：
 * 实际间隔 = 基础 10 刻 ÷ (1 + 加成)，与乌云的「消耗速度」同一套算法。
 */
public class OakCharcoalItem extends ArtifactItem {
	/** 各等级的「灼伤的攻击速度」加成（%）。 */
	private static final double[] SPEED_BY_LEVEL = { 10.0D, 20.0D, 30.0D, 40.0D, 50.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public OakCharcoalItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.oak_charcoal.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double burnTickSpeedPercent(int level) {
		return valueAt(SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.burn_tick_speed",
						Component.literal(Numbers.format(this.burnTickSpeedPercent(level))).withColor(COLOUR_BONUS)));
	}
}
