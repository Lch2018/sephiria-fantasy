package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 力量护符（坚固，普通品质）：【唯一】物理伤害 +2/3/4/6（0..3 级）。
 *
 * <p>「物理伤害」在本模组的伤害体系里就是<b>物理强度</b>（伤害 = 基础伤害 × 物理强度 / 20），
 * 所以 +2 等价于整体伤害 +10%，面板会直接显示加成后的值。
 */
public class CharmOfStrengthItem extends ArtifactItem {
	/** 各等级的物理强度加成。 */
	private static final double[] PHYSICAL_BY_LEVEL = { 2.0D, 3.0D, 4.0D, 6.0D };
	/** 词条数值的颜色：绿色，和参考图一致。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public CharmOfStrengthItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.charm_of_strength.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return PHYSICAL_BY_LEVEL.length - 1;
	}

	@Override
	public double physicalBonus(int level) {
		return valueAt(PHYSICAL_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria.affix.physical",
				Component.literal(Numbers.format(this.physicalBonus(level))).withColor(COLOUR_BONUS)));
	}
}
