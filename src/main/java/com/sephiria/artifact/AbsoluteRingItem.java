package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 绝对戒指（坚固，传说品质）：【唯一】特殊攻击范围 +30/45/60%、特殊攻击伤害 +10/20/45%。
 *
 * <p>范围加成由 {@code PlayerStats#skillRangeMultiplier} 汇总到各武器技能的范围判定里；
 * 普通攻击的横扫范围不受影响。
 */
public class AbsoluteRingItem extends ArtifactItem {
	private static final double[] SPECIAL_RANGE_BY_LEVEL = { 30.0D, 45.0D, 60.0D };
	private static final double[] SPECIAL_ATTACK_BY_LEVEL = { 10.0D, 20.0D, 45.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public AbsoluteRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.STURDY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.absolute_ring.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SPECIAL_RANGE_BY_LEVEL.length - 1;
	}

	@Override
	public double specialAttackRangePercentBonus(int level) {
		return valueAt(SPECIAL_RANGE_BY_LEVEL, level);
	}

	@Override
	public double specialAttackBonus(int level) {
		return valueAt(SPECIAL_ATTACK_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.special_attack_range",
						Component.literal(Numbers.format(this.specialAttackRangePercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.special_attack",
						Component.literal(Numbers.format(this.specialAttackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
