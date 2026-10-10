package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 索利斯·德克里（太阳剑，传说品质）：【唯一】太阳剑无视目标防御力 +2/4/6/8/10/12/14%、
 * 太阳剑暴击几率 +5/10/15/22/30/40/50%（0..6 级）。
 *
 * <p>「无视防御力」让太阳剑那一下只吃目标 (1−m)×(1−x) 的减伤（见 {@code sun.SunSword#withDefenseIgnored}）；
 * 「太阳剑暴击几率」只在太阳剑那一下加在通用暴击几率之上（{@code SephiriaDamage.applyCrit} 的 SUN 分支）。
 */
public class SolisDekuriItem extends ArtifactItem {
	/** 各等级的无视目标防御力（%）。 */
	private static final double[] IGNORE_BY_LEVEL = { 2D, 4D, 6D, 8D, 10D, 12D, 14D };
	/** 各等级的太阳剑暴击几率（%）。 */
	private static final double[] CRIT_BY_LEVEL = { 5D, 10D, 15D, 22D, 30D, 40D, 50D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SolisDekuriItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.solis_dekuri.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return IGNORE_BY_LEVEL.length - 1;
	}

	@Override
	public double sunSwordDefenseIgnorePercent(int level) {
		return valueAt(IGNORE_BY_LEVEL, level);
	}

	@Override
	public double sunSwordCritChanceBonus(int level) {
		return valueAt(CRIT_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_defense_ignore",
						Component.literal(Numbers.format(this.sunSwordDefenseIgnorePercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_crit_chance",
						Component.literal(Numbers.format(this.sunSwordCritChanceBonus(level))).withColor(COLOUR_BONUS)));
	}
}
