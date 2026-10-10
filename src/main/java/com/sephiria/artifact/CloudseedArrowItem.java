package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 云种箭头（乌云，高级品质）：乌云的闪电以 5/10/15/23/35/50% 的概率被强化并发射（0..5 级）。
 *
 * <p>没有【唯一】：带几件就叠几份（概率相加）。被强化的那一记伤害翻倍，
 * 落点会多炸一圈火花作区分（见 {@code cloud.DarkCloud#strike}）。
 */
public class CloudseedArrowItem extends ArtifactItem {
	/** 各等级的「闪电被强化」概率（%）。 */
	private static final double[] ENHANCED_BY_LEVEL = { 5.0D, 10.0D, 15.0D, 23.0D, 35.0D, 50.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public CloudseedArrowItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.cloudseed_arrow.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return ENHANCED_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudEnhancedChance(int level) {
		return valueAt(ENHANCED_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_enhanced",
						Component.literal(Numbers.format(this.cloudEnhancedChance(level))).withColor(COLOUR_BONUS)));
	}
}
