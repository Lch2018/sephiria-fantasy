package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷文泥板（乌云，传说品质）：【唯一】乌云以 20/30/40/55/70% 的概率不被消耗（0..4 级）。
 *
 * <p>每一下开火单独掷：命中就不扣那 1 点容量（2 点射的两下各自掷）。
 * 容量不足时的出手判定照旧（掷的是「扣不扣」，不是「能不能打」）。
 */
public class RavenTabletItem extends ArtifactItem {
	/** 各等级的「不被消耗」概率（%）。 */
	private static final double[] FREE_SHOT_BY_LEVEL = { 20.0D, 30.0D, 40.0D, 55.0D, 70.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public RavenTabletItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.raven_tablet.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return FREE_SHOT_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudFreeShotChance(int level) {
		return valueAt(FREE_SHOT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.cloud_free_shot",
						Component.literal(Numbers.format(this.cloudFreeShotChance(level))).withColor(COLOUR_BONUS)));
	}
}
