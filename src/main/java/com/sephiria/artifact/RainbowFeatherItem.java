package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 彩虹羽毛（风之歌，传说品质）：【唯一】匕首获取专注时额外获取 0/0/1 层；
 * 特殊攻击伤害 +10/16/22%；冲刺恢复速度 +5/5/10%。
 *
 * <p>专注额外获取同时会把专注上限抬高同样的层数，否则多出来的一层会被 1 层上限直接吞掉。
 */
public class RainbowFeatherItem extends ArtifactItem {
	private static final int[] FOCUS_BY_LEVEL = { 0, 0, 1 };
	private static final double[] SPECIAL_ATTACK_BY_LEVEL = { 10.0D, 16.0D, 22.0D };
	private static final double[] DASH_REGEN_BY_LEVEL = { 5.0D, 5.0D, 10.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public RainbowFeatherItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.rainbow_feather.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return FOCUS_BY_LEVEL.length - 1;
	}

	@Override
	public int daggerFocusBonus(int level) {
		return FOCUS_BY_LEVEL[Math.max(0, Math.min(FOCUS_BY_LEVEL.length - 1, level))];
	}

	@Override
	public double specialAttackBonus(int level) {
		return valueAt(SPECIAL_ATTACK_BY_LEVEL, level);
	}

	@Override
	public double dashRegenPercentBonus(int level) {
		return valueAt(DASH_REGEN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.dagger_focus",
						Component.literal(Numbers.format(this.daggerFocusBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.special_attack",
						Component.literal(Numbers.format(this.specialAttackBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dash_regen",
						Component.literal(Numbers.format(this.dashRegenPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
