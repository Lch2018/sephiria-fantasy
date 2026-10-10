package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 故障探测针（精密，稀有品质）：【唯一】暴击几率 +3/6/9/12/15/18/21%（0..6 级）。
 *
 * <p>这条是通用暴击几率：任何由玩家造成的伤害都吃（含神器伤害），与渴望护符那条
 * 「只有武器攻击吃」的区别见武器数据表第十三节。
 */
public class FaultProbeItem extends ArtifactItem {
	/** 各等级的暴击几率加成（百分点）。 */
	private static final double[] CRIT_CHANCE_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D, 15.0D, 18.0D, 21.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FaultProbeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.fault_probe.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_CHANCE_BY_LEVEL.length - 1;
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_CHANCE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)));
	}
}
