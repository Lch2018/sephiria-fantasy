package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 冰封的心脏（冰霜武具，稀有品质）：冰霜武具充能速度 +10/18/26/40/55/70/85/100%（0..7 级，共 8 档）。
 *
 * <p>没有【唯一】：背包里放几颗就叠几份。这一项与连击档位的「充能速度」相加，
 * 一起缩短三件充能表（剑鞘 / 锤子 / 长矛）的等待时间（见 {@code frost.FrostArmament} 顶部：
 * 实际刻数 = 基础刻数 ÷ (1 + 加成)，100% 就是快一倍）。
 * <b>冰弓的装填不吃这一项</b>——它是固定的 {@code BOW_RELOAD_TICKS} 16 刻。
 */
public class FrozenHeartItem extends ArtifactItem {
	/** 各等级的「冰霜武具充能速度」加成（%）。 */
	private static final double[] CHARGE_SPEED_BY_LEVEL =
			{ 10.0D, 18.0D, 26.0D, 40.0D, 55.0D, 70.0D, 85.0D, 100.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FrozenHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.frozen_heart.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return CHARGE_SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double frostChargeSpeedBonus(int level) {
		return valueAt(CHARGE_SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.frost_charge_speed",
						Component.literal(Numbers.format(this.frostChargeSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
