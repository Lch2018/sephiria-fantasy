package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 封印的特加斯（余烬 + 冰川，羁绊品质）：【唯一】灼伤变更为<b>蓝色灼伤</b>；
 * 灼伤异常状态额外伤害 +3/6/9/12%（0..3 级）。
 *
 * <p><b>双连击</b>神器：同时给余烬与冰川各加 1 级（见 {@link #combos()}）。
 *
 * <p><b>蓝色灼伤</b>（口径见 {@code Debuffs#activateBurn}）：每跳的强度从「火元素强度」
 * 换成「火元素强度 + 冰元素强度」，并按两者里更高的那个决定这一跳的伤害元素类型
 * ——火 ≥ 冰走火属性（{@code sephiria:burn}），冰更高走冰属性（{@code sephiria:blue_burn}）。
 * 两者都是减益伤害，减伤 / 黄金之手 / 「造成伤害时」那几套口径完全一样，只有元素类型不同。
 *
 * <p>第二行词条与红色线球共用 {@code burnDamagePercent}（两者相加）。
 */
public class SealedTegasItem extends ArtifactItem {
	/** 各等级的「灼伤异常状态额外伤害」加成（%）。 */
	private static final double[] BURN_DAMAGE_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SealedTegasItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	/** 双连击：余烬与冰川各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.EMBER, ArtifactCombo.GLACIER);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sealed_tegas.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return BURN_DAMAGE_BY_LEVEL.length - 1;
	}

	/** 带这件神器（且生效）时灼伤一律按蓝色灼伤结算。 */
	@Override
	public double blueBurn(int level) {
		return 1.0D;
	}

	@Override
	public double burnDamagePercent(int level) {
		return valueAt(BURN_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.blue_burn"),
				Component.translatable("artifact.sephiria_fantasy.affix.burn_damage",
						Component.literal(Numbers.format(this.burnDamagePercent(level))).withColor(COLOUR_BONUS)));
	}
}
