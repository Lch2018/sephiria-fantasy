package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 名匠的刀格（坚固，传说品质）：【唯一】武器伤害 +5/10/15%；
 * 刀收刀 / 拔刀的无敌时间 +40/70/120%。
 *
 * <p>切换无敌时间由 {@code SephiriaKatanaItem#toggle} 现查 {@link ArtifactEffects} 后结算。
 */
public class MastersSwordGuardItem extends ArtifactItem {
	private static final double[] WEAPON_DAMAGE_BY_LEVEL = { 5.0D, 10.0D, 15.0D };
	private static final double[] SWITCH_INVULN_BY_LEVEL = { 40.0D, 70.0D, 120.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MastersSwordGuardItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.masters_sword_guard.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return WEAPON_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double weaponDamagePercentBonus(int level) {
		return valueAt(WEAPON_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double katanaSwitchInvulnPercent(int level) {
		return valueAt(SWITCH_INVULN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.weapon_damage",
						Component.literal(Numbers.format(this.weaponDamagePercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.katana_switch_invuln",
						Component.literal(Numbers.format(this.katanaSwitchInvulnPercent(level))).withColor(COLOUR_BONUS)));
	}
}
