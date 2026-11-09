package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 神射手的曲柄（坚固，传说品质）：【唯一】弩重装速度 +30/50%、弩攻击伤害 +30/40%；
 * 装填完成时 10 秒内弩攻击伤害再 +30/40%。
 *
 * <p>数值由 {@link ArtifactEffects} 汇总，弩那边（{@code SephiriaCrossbowItem}）负责实际结算。
 */
public class SharpshooterCrankItem extends ArtifactItem {
	private static final double[] RELOAD_SPEED_BY_LEVEL = { 30.0D, 50.0D };
	private static final double[] DAMAGE_BY_LEVEL = { 30.0D, 40.0D };
	private static final double[] RELOAD_BUFF_BY_LEVEL = { 30.0D, 40.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SharpshooterCrankItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.sharpshooter_crank.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return RELOAD_SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double crossbowReloadSpeedPercent(int level) {
		return valueAt(RELOAD_SPEED_BY_LEVEL, level);
	}

	@Override
	public double crossbowDamagePercent(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double crossbowReloadBuffPercent(int level) {
		return valueAt(RELOAD_BUFF_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crossbow_reload_speed",
						Component.literal(Numbers.format(this.crossbowReloadSpeedPercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crossbow_damage",
						Component.literal(Numbers.format(this.crossbowDamagePercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crossbow_reload_buff",
						Component.literal(Numbers.format(this.crossbowReloadBuffPercent(level))).withColor(COLOUR_BONUS)));
	}
}
