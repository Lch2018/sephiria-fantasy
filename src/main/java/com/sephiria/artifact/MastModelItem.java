package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 桅杆模型（乌云，普通品质）：【唯一】乌云的消耗速度 +20/40/60%（0..2 级）。
 *
 * <p>与乐谱《台风》那条「按攻击速度的比例」不同：这里是固定点数，两者相加后一起算攻击间隔
 * （消耗速度 = 100% + 攻击速度 × 台风比例 + 本项，间隔 = 20 刻 ÷ 消耗速度）。
 */
public class MastModelItem extends ArtifactItem {
	/** 各等级的「乌云的消耗速度」固定加成（%）。 */
	private static final double[] SPEED_BY_LEVEL = { 20.0D, 40.0D, 60.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MastModelItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.mast_model.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudSpeedBonus(int level) {
		return valueAt(SPEED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.cloud_speed_bonus",
						Component.literal(Numbers.format(this.cloudSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
