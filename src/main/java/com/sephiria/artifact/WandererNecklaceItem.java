package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 流浪者的项链（风之歌，稀有品质）：【唯一】攻击速度 +6/12/18%、最高元素伤害 +2/4/6（0 / 1 / 2 级）。
 *
 * <p>「最高元素伤害」是固定值：加在物理强度 / 火 / 冰 / 电四项里数值最高的那一项上。
 */
public class WandererNecklaceItem extends ArtifactItem {
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 6.0D, 12.0D, 18.0D };
	private static final double[] HIGHEST_ELEMENT_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WandererNecklaceItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.wanderer_necklace.flavor";
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

	@Override
	public double highestElementBonus(int level) {
		return valueAt(HIGHEST_ELEMENT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.attack_speed",
						Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.highest_element",
						Component.literal(Numbers.format(this.highestElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
