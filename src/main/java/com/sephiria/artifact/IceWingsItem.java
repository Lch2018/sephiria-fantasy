package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 冰之翼（冰霜武具，传说品质）：【唯一】冰霜武具额外触发 0/0/0/1/1/1/2 次；暴击伤害 +2/4/6/8/10/12/20%
 * （0..6 级，共 7 档）。
 *
 * <p>「额外触发」与连击 10 档的同一项相加（见 {@code frost.FrostArmament}）：剑鞘多放一道剑气、
 * 锤子多扔一把、冰长矛多来一根。暴击伤害走通用的 {@code critDamageBonus}，任何伤害都吃。
 */
public class IceWingsItem extends ArtifactItem {
	/** 各等级的「冰霜武具额外触发次数」（次）。 */
	private static final int[] EXTRA_TRIGGERS_BY_LEVEL = { 0, 0, 0, 1, 1, 1, 2 };
	/** 各等级的暴击伤害加成（%）：加在默认的 150% 上。 */
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D, 10.0D, 12.0D, 20.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public IceWingsItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.ice_wings.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public int frostExtraTriggersBonus(int level) {
		return EXTRA_TRIGGERS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, EXTRA_TRIGGERS_BY_LEVEL.length - 1)];
	}

	@Override
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.frost_extra_triggers",
						Component.literal(Numbers.format(this.frostExtraTriggersBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
