package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 翅膀（风之歌，传说品质）：【唯一】攻击速度 +3/6/9/12/15/18%；
 * 每 25/20/15/9/6/5% 额外攻击速度，使所有伤害放大 +1%。
 *
 * <p>「额外攻击速度」= 面板攻速超过 100% 的部分（{@code PlayerStats#attackSpeedTotal}），
 * 所以翅膀自己的攻速也算在里面；档位按向下取整算，由 {@code ArtifactEffects#wingsDamageAmpPercent}
 * 汇总进所有伤害倍率。
 */
public class WingsItem extends ArtifactItem {
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D, 15.0D, 18.0D };
	private static final double[] DAMAGE_STEP_BY_LEVEL = { 25.0D, 20.0D, 15.0D, 9.0D, 6.0D, 5.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WingsItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.wings.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ATTACK_SPEED_BY_LEVEL.length - 1;
	}

	@Override
	public double attackSpeedBonus(int level) {
		return valueAt(ATTACK_SPEED_BY_LEVEL, level);
	}

	/** 这一级每多少额外攻击速度换 1% 全伤害放大。 */
	public static double damageStep(int level) {
		return valueAt(DAMAGE_STEP_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.wings_damage",
						Component.literal(Numbers.format(damageStep(level))).withColor(COLOUR_BONUS)));
	}
}
