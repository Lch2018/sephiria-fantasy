package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 小魔法海螺（冰霜武具，高级品质）：冰霜武具的伤害 +3/6/10%、暴击伤害 +5/8/12%（0..2 级）。
 *
 * <p>没有【唯一】：背包里放几只就叠几份（连击等级同理各记 1 级）。
 *
 * <p>「冰霜武具的伤害」与连击档位的同一项相加，一起放大剑气 / 锤击那两下
 * （见 {@code frost.FrostArmament}）；暴击伤害走通用的 {@code critDamageBonus}，任何伤害都吃。
 */
public class MagicConchItem extends ArtifactItem {
	/** 各等级的「冰霜武具的伤害」加成（%）。 */
	private static final double[] FROST_DAMAGE_BY_LEVEL = { 3.0D, 6.0D, 10.0D };
	/** 各等级的暴击伤害加成（%）：加在默认的 150% 上。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 5.0D, 8.0D, 12.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MagicConchItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.magic_conch.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return FROST_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double frostArmamentDamagePercent(int level) {
		return valueAt(FROST_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.frost_armament_damage",
						Component.literal(Numbers.format(this.frostArmamentDamagePercent(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
