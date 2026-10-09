package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷石（乌云，稀有品质）：乌云的额外伤害 +25/30/35/40/45/50/60%（0..6 级）。
 *
 * <p>没有【唯一】：带几件就叠几份。每记雷击的伤害乘 (1 + 加成)，
 * 加成在电元素强度换算完之后再乘（见 {@code cloud.DarkCloud#strike}）。
 */
public class ThunderStoneItem extends ArtifactItem {
	/** 各等级的「乌云的额外伤害」加成（%）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 25.0D, 30.0D, 35.0D, 40.0D, 45.0D, 50.0D, 60.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ThunderStoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.DARK_CLOUD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.thunder_stone.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double cloudDamagePercent(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.cloud_damage",
						Component.literal(Numbers.format(this.cloudDamagePercent(level))).withColor(COLOUR_BONUS)));
	}
}
