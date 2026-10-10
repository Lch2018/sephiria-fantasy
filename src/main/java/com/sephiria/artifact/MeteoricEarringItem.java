package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 陨铁耳环（太阳剑，稀有品质）：【唯一】太阳剑投掷时落在你附近、火焰属性伤害 +2/4/7（0..2 级）。
 *
 * <p>掉落位置由 {@code sun.SunSword#throwSword} 读 {@code ArtifactEffects#sunSwordDropNearPlayer} 决定；
 * 火焰属性伤害就是火元素强度，与索利斯那两枚徽章同一个钩子。
 */
public class MeteoricEarringItem extends ArtifactItem {
	/** 各等级的投掷掉落改在玩家附近（1 = 有）。 */
	private static final int[] DROP_NEAR_BY_LEVEL = { 1, 1, 1 };
	/** 各等级的火焰属性伤害（点）。 */
	private static final double[] FIRE_BY_LEVEL = { 2D, 4D, 7D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MeteoricEarringItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.meteoric_earring.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DROP_NEAR_BY_LEVEL.length - 1;
	}

	@Override
	public int sunSwordDropNearPlayer(int level) {
		return DROP_NEAR_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, DROP_NEAR_BY_LEVEL.length - 1)];
	}

	@Override
	public double fireElementBonus(int level) {
		return valueAt(FIRE_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_drop_near"),
				Component.translatable("artifact.sephiria_fantasy.affix.fire_element",
						Component.literal(Numbers.format(this.fireElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
