package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 常青斗篷（影子，高级品质）：【唯一】冲刺次数 +0/1/1、闪避 +4/6/8、暴击几率 +1/2/4%（0..2 级）。
 *
 * <p>冲刺次数是「冲刺」存储的上限（HUD 右下角的 x/y 那个 y），与金色斗篷那条同源。
 */
public class EvergreenCloakItem extends ArtifactItem {
	/** 各等级的冲刺次数加成。 */
	private static final double[] DASH_BY_LEVEL = { 0.0D, 1.0D, 1.0D };
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 4.0D, 6.0D, 8.0D };
	/** 各等级的暴击几率（百分点）。 */
	private static final double[] CRIT_BY_LEVEL = { 1.0D, 2.0D, 4.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EvergreenCloakItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.evergreen_cloak.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DASH_BY_LEVEL.length - 1;
	}

	@Override
	public int dashChargesBonus(int level) {
		return (int) valueAt(DASH_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.dash_charges",
						Component.literal(Numbers.format(this.dashChargesBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)));
	}
}
