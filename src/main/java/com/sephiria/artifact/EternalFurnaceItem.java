package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 永恒熔炉（太阳剑，普通品质）：【唯一】每 10/8/5 秒生成 1 支太阳剑（只在太阳剑效果激活时走表）、
 * 暴击几率 +1/2.5/5%（0..2 级）。
 *
 * <p>「生成太阳剑」是容量增加：由 {@code sun.SunSword#generate} 每个周期把当前数量 +1（不超上限）；
 * 暴击几率走通用的 {@code ArtifactEffects#critChanceBonus}。
 */
public class EternalFurnaceItem extends ArtifactItem {
	/** 各等级的每隔多少秒 +1 支（秒）。 */
	private static final int[] SECONDS_BY_LEVEL = { 10, 8, 5 };
	/** 各等级的暴击几率（%）。 */
	private static final double[] CRIT_BY_LEVEL = { 1D, 2.5D, 5D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EternalFurnaceItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.eternal_furnace.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SECONDS_BY_LEVEL.length - 1;
	}

	@Override
	public int sunSwordCapacityEverySeconds(int level) {
		return SECONDS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, SECONDS_BY_LEVEL.length - 1)];
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_capacity_every",
						Component.literal(Numbers.format(this.sunSwordCapacityEverySeconds(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)));
	}
}
