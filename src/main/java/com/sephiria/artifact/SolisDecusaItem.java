package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 索利斯·德库萨（太阳剑，高级品质）：【唯一】太阳剑伤害 +5/10/15/20%、暴击伤害 +4/8/12/16%（0..3 级）。
 *
 * <p>「太阳剑伤害」与连击档位那一项相加（{@code ArtifactEffects#sunSwordDamagePercentBonus}）；
 * 暴击伤害并进通用的暴击伤害面板值。
 */
public class SolisDecusaItem extends ArtifactItem {
	/** 各等级的太阳剑伤害（%）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 5D, 10D, 15D, 20D };
	/** 各等级的暴击伤害（%）。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 4D, 8D, 12D, 16D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SolisDecusaItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.solis_decusa.flavor";
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
	public double sunSwordDamagePercentBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_damage",
						Component.literal(Numbers.format(this.sunSwordDamagePercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
