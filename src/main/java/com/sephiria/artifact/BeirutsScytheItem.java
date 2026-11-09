package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 贝鲁特之镰（精密，传说品质）：【唯一】暴击几率 +2.5/5/7.5/10%；
 * 攻击暴击几率超过 100% 时，溢出的部分转为处决几率，处决使这一下的暴击伤害翻倍。
 *
 * <p>处决判定在 {@code SephiriaDamage#applyCrit}：溢出的几率单独掷一次，命中时把该次暴击的
 * 伤害倍率再乘 2；没有超过 100% 时不掷。
 */
public class BeirutsScytheItem extends ArtifactItem {
	private static final double[] CRIT_CHANCE_BY_LEVEL = { 2.5D, 5.0D, 7.5D, 10.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BeirutsScytheItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.beiruts_scythe.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_CHANCE_BY_LEVEL.length - 1;
	}

	@Override
	public double critChanceBonus(int level) {
		return valueAt(CRIT_CHANCE_BY_LEVEL, level);
	}

	@Override
	public boolean executionEnabled(int level) {
		return true;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.beiruts_scythe_execution"));
	}
}
