package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 弹力带（影子，高级品质）：【唯一】闪避触发时恢复冲刺次数 1/1/2、暴击伤害 +2/4/6%、闪避 +1/2/3
 * （0..2 级）。
 *
 * <p>「闪避触发时恢复冲刺」是第一条「事件触发」词条：闪避判定在 {@code Dodge} 里，命中时回一次
 * 冲刺次数（见 {@code ArtifactEffects#dodgeRestoreCharges}）。
 */
public class ResistanceBandItem extends ArtifactItem {
	/** 各等级「闪避触发时恢复的冲刺次数」。 */
	private static final double[] RESTORE_BY_LEVEL = { 1.0D, 1.0D, 2.0D };
	/** 各等级的暴击伤害加成（百分点）。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 1.0D, 2.0D, 3.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ResistanceBandItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.resistance_band.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return RESTORE_BY_LEVEL.length - 1;
	}

	@Override
	public double dodgeRestoreDashCharges(int level) {
		return valueAt(RESTORE_BY_LEVEL, level);
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.dodge_restore",
						Component.literal(Numbers.format(this.dodgeRestoreDashCharges(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
