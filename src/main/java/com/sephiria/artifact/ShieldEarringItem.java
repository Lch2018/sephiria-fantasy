package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 盾牌耳环（守护，高级）：【唯一】最大 HP +1/2/3/4、闪避 -5/4/3/2。 */
public class ShieldEarringItem extends ArtifactItem {
	private static final double[] MAX_HP_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D };
	private static final double[] DODGE_BY_LEVEL = { -5.0D, -4.0D, -3.0D, -2.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ShieldEarringItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.shield_earring.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MAX_HP_BY_LEVEL.length - 1;
	}

	@Override
	public double maxHpBonus(int level) {
		return valueAt(MAX_HP_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.max_hp",
						Component.literal(Numbers.format(this.maxHpBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dodge_penalty",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
