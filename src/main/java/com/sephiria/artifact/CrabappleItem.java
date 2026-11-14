package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 海棠果（学院，高级）：魔法书加速 +6/10/14，冲刺恢复速度 +5/8/12%。 */
public class CrabappleItem extends ArtifactItem {
	private static final double[] HASTE_BY_LEVEL = { 6.0D, 10.0D, 14.0D };
	private static final double[] DASH_BY_LEVEL = { 5.0D, 8.0D, 12.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public CrabappleItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.crabapple.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return HASTE_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookHasteBonus(int level) {
		return valueAt(HASTE_BY_LEVEL, level);
	}

	@Override
	public double dashRegenPercentBonus(int level) {
		return valueAt(DASH_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dash_regen",
						Component.literal(Numbers.format(this.dashRegenPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
