package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 剑术教材（风之歌，稀有品质）：【唯一】攻击速度 +7% / +14% / +26%（0 / 1 / 2 级）。 */
public class SwordTextbookItem extends ArtifactItem {
	private static final double[] ATTACK_SPEED_BY_LEVEL = { 7.0D, 14.0D, 26.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public SwordTextbookItem(Properties properties) {
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
		return "artifact.sephiria.sword_textbook.flavor";
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
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria.affix.attack_speed",
				Component.literal(Numbers.format(this.attackSpeedBonus(level))).withColor(COLOUR_BONUS)));
	}
}
