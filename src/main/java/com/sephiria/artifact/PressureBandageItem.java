package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 压迫绷带（风之歌，普通品质）：【唯一】冲刺恢复速度 +10% / +15% / +30%（0 / 1 / 2 级）。 */
public class PressureBandageItem extends ArtifactItem {
	private static final double[] REGEN_BY_LEVEL = { 10.0D, 15.0D, 30.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public PressureBandageItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.pressure_bandage.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return REGEN_BY_LEVEL.length - 1;
	}

	@Override
	public double dashRegenPercentBonus(int level) {
		return valueAt(REGEN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.dash_regen",
				Component.literal(Numbers.format(this.dashRegenPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
