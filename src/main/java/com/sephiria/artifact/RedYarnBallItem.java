package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 红色线球（余烬，稀有品质）：【唯一】灼伤异常状态额外伤害 +20/40/60/80/100%（0..4 级）。
 *
 * <p>放大的是「灼伤」每跳的伤害（见 {@code Debuffs#activateBurn}）：每跳乘 (1 + 加成)。
 */
public class RedYarnBallItem extends ArtifactItem {
	/** 各等级的「灼伤异常状态额外伤害」加成（%）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 20.0D, 40.0D, 60.0D, 80.0D, 100.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public RedYarnBallItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.red_yarn_ball.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double burnDamagePercent(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.burn_damage",
						Component.literal(Numbers.format(this.burnDamagePercent(level))).withColor(COLOUR_BONUS)));
	}
}
