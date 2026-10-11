package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 蓝爪（冰川，传说品质）：【唯一】冻伤赋予时有 30/40/50/60% 的概率额外附加一次；
 * 冰属性伤害 +2/4/6/9；最大 MP +5/10/15/20（0..3 级）。
 *
 * <p>「额外附加一次」在 {@code Debuffs#applyFrostbite} 里掷：命中就多叠一层冻伤
 * ——可能因此正好凑满阈值、当场把目标冻住（那是这套连击的主要爆发点）。
 *
 * <p>最大 MP 走本模组自己的蓝量池（{@code PlayerStats.maxMpTotal}），不是原版属性，
 * 与剑耳环削上限那个词条共用 {@code maxMpBonus} 这一条。
 */
public class BlueClawItem extends ArtifactItem {
	/** 各等级的「额外附加一次」概率（%）。 */
	private static final double[] EXTRA_CHANCE_BY_LEVEL = { 30.0D, 40.0D, 50.0D, 60.0D };
	/** 各等级的冰元素强度加成（点）。 */
	private static final double[] ICE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 9.0D };
	/** 各等级的最大 MP 加成（点）。 */
	private static final double[] MAX_MP_BY_LEVEL = { 5.0D, 10.0D, 15.0D, 20.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BlueClawItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GLACIER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.blue_claw.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return ICE_BY_LEVEL.length - 1;
	}

	@Override
	public double frostbiteExtraChance(int level) {
		return valueAt(EXTRA_CHANCE_BY_LEVEL, level);
	}

	@Override
	public double iceElementBonus(int level) {
		return valueAt(ICE_BY_LEVEL, level);
	}

	@Override
	public double maxMpBonus(int level) {
		return valueAt(MAX_MP_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.frostbite_extra",
						Component.literal(Numbers.format(this.frostbiteExtraChance(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_element",
						Component.literal(Numbers.format(this.iceElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
						Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
