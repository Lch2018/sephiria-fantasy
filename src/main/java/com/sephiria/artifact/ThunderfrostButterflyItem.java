package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷霜蝶（乌云 + 冰川，羁绊品质）：【唯一】把乌云变成<b>暴风雪云</b>；
 * 冰属性伤害 +2/4/6/8、乌云的消耗速度 +6/12/18/24%（0..3 级）。
 *
 * <p><b>双连击</b>神器：同时给乌云与冰川各加 1 级（见 {@link #combos()}）。
 *
 * <p><b>暴风雪云</b>（口径见 {@code DarkCloud}）：本质仍是乌云，容量 / 消耗速度 / 强化概率都不变；
 * 每记攻击的伤害改成「闪电与冰里较高的一方 × 普通乌云的 20% × 110% + 较低的一方 × 普通乌云的 20% × 15%」，
 * 并拆成雷、冰两段双属性伤害（各自触发对应的减益守门）。视觉上云下会一直飘雪——这里用的是
 * 从原版雪的天气贴图裁出来的雪花粒子，所以任何群系、任何高度都是雪，不会变成雨。
 */
public class ThunderfrostButterflyItem extends ArtifactItem {
	/** 各等级冰属性伤害加成（点）。 */
	private static final double[] ICE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D };
	/** 各等级乌云的消耗速度固定加成（%）。 */
	private static final double[] CLOUD_SPEED_BY_LEVEL = { 6.0D, 12.0D, 18.0D, 24.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ThunderfrostButterflyItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	/** 双连击：乌云与冰川各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.DARK_CLOUD, ArtifactCombo.GLACIER);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.thunderfrost_butterfly.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ICE_BY_LEVEL.length - 1;
	}

	@Override
	public double iceElementBonus(int level) {
		return valueAt(ICE_BY_LEVEL, level);
	}

	@Override
	public double cloudSpeedBonus(int level) {
		return valueAt(CLOUD_SPEED_BY_LEVEL, level);
	}

	/** 固定效果：乌云变成暴风雪云。 */
	@Override
	public boolean blizzardCloud(int level) {
		return true;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		// 冰属性那条的 lang 键本身没写「+」，这里把符号带进数值里（与闪电 / 火焰那条的写法对齐）
		Component ice = Component.literal("+" + Numbers.format(this.iceElementBonus(level)))
				.withColor(COLOUR_BONUS);
		Component speed = Component.literal(Numbers.format(this.cloudSpeedBonus(level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.blizzard_cloud"),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_element", ice),
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_speed_bonus", speed),
				Component.translatable("artifact.sephiria_fantasy.affix.blizzard_damage"));
	}
}
