package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 等离子头盔（余烬 + 魔法科技，羁绊品质）：【唯一】把目标身上的灼伤与触电合并成等离子体；
 * 等离子体伤害量 -15/-10/-5/0/+5%（0..4 级）。
 *
 * <p><b>双连击</b>神器：同时给余烬与魔法科技各加 1 级（见 {@link #combos()}）。
 *
 * <p>等离子体本身的口径在 {@code Debuffs#applyPlasma}：灼伤与触电的层数、持续时间合并，
 * 但两套伤害仍按各自的倍率与周期分别结算，系数取「火 + 电」（蓝色灼伤再额外加冰）。
 */
public class PlasmaHelmetItem extends ArtifactItem {
	/** 各等级的等离子体伤害量加成（%，可为负）。 */
	private static final double[] DAMAGE_PERCENT_BY_LEVEL = { -15.0D, -10.0D, -5.0D, 0.0D, 5.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public PlasmaHelmetItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	/** 双连击：余烬与魔法科技各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.EMBER, ArtifactCombo.MAGIC_TECH);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.plasma_helmet.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_PERCENT_BY_LEVEL.length - 1;
	}

	@Override
	public boolean plasmaDebuff(int level) {
		return true;
	}

	@Override
	public double plasmaDamagePercent(int level) {
		return valueAt(DAMAGE_PERCENT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		double percent = this.plasmaDamagePercent(level);
		Component value = Component.literal((percent >= 0.0D ? "+" : "") + Numbers.format(percent))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.plasma_debuff"),
				Component.translatable("artifact.sephiria_fantasy.affix.plasma_damage", value));
	}
}
