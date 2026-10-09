package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 索利斯·帕尔沃（太阳剑，高级品质）：【唯一】太阳剑数量上限 +1/1/2/2/3、移动速度 +1/2/4/6/8%（0..4 级）。
 *
 * <p>「太阳剑数量上限」加在基础 5 支与连击档位之上（见 {@code sun.SunSword#maxCapacity}）；
 * 「移动速度」并进移速面板值，由 {@code stats/StatAttributes} 挂成原版属性修饰符。
 */
public class SolisParvoItem extends ArtifactItem {
	/** 各等级的「太阳剑数量上限」加成（支）。 */
	private static final int[] CAPACITY_BY_LEVEL = { 1, 1, 2, 2, 3 };
	/** 各等级的「移动速度」加成（%）。 */
	private static final double[] MOVE_SPEED_BY_LEVEL = { 1.0D, 2.0D, 4.0D, 6.0D, 8.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SolisParvoItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.solis_parvo.flavor";
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
	public int sunSwordCapacityBonus(int level) {
		return CAPACITY_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, CAPACITY_BY_LEVEL.length - 1)];
	}

	@Override
	public double moveSpeedPercentBonus(int level) {
		return valueAt(MOVE_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.sun_sword_capacity",
						Component.literal(Numbers.format(this.sunSwordCapacityBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.move_speed",
						Component.literal(Numbers.format(this.moveSpeedPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
