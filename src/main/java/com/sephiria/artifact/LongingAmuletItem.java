package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 渴望护符（精密，普通品质）：【唯一】武器攻击的暴击几率 +3/6/10/14%（0..3 级）。
 *
 * <p>「武器攻击的暴击几率」只加在武器打出的伤害上（普通攻击、横扫、弩矢、武器技能），
 * 神器自己造成的伤害（红色露水的溅射、无视防御伤害）吃不到——见
 * {@code PlayerStats#weaponCritChanceTotal}。
 */
public class LongingAmuletItem extends ArtifactItem {
	/** 各等级的武器攻击暴击几率加成（百分点）。 */
	private static final double[] WEAPON_CRIT_BY_LEVEL = { 3.0D, 6.0D, 10.0D, 14.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LongingAmuletItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.longing_amulet.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return WEAPON_CRIT_BY_LEVEL.length - 1;
	}

	@Override
	public double weaponCritChanceBonus(int level) {
		return valueAt(WEAPON_CRIT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.weapon_crit_chance",
						Component.literal(Numbers.format(this.weaponCritChanceBonus(level)))
								.withColor(COLOUR_BONUS)));
	}
}
