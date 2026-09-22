package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 温暖的石头（精密，稀有品质）：【唯一】暴击伤害 +10/20/30/40%（0..3 级）。
 *
 * <p>暴击伤害加在默认的 150% 上：满级 190%，也就是暴击时伤害乘 1.9。
 */
public class WarmStoneItem extends ArtifactItem {
	/** 各等级的暴击伤害加成（百分点）。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 10.0D, 20.0D, 30.0D, 40.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WarmStoneItem(Properties properties) {
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
		return "artifact.sephiria.warm_stone.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
