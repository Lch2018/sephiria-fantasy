package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 尖尖的橡果（乌云，高级品质）：【唯一】战斗中乌云恢复速度 +3/6/9/12%、闪避 +3/3/6/6（0..3 级）。
 *
 * <p>「战斗中乌云恢复速度」只放大战斗中那条回复线（每 5 秒回上限的 5% × (1 + 加成)）；
 * 脱战那条线（每 0.1 秒 5%）不受影响——脱战本来就回得飞快。
 */
public class PointyAcornItem extends ArtifactItem {
	/** 各等级的「战斗中乌云恢复速度」加成（%）。 */
	private static final double[] COMBAT_REGEN_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D };
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 3.0D, 3.0D, 6.0D, 6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public PointyAcornItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.pointy_acorn.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return COMBAT_REGEN_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudCombatRegenPercent(int level) {
		return valueAt(COMBAT_REGEN_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_combat_regen",
						Component.literal(Numbers.format(this.cloudCombatRegenPercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
