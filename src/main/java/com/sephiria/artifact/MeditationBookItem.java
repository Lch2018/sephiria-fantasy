package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 冥想书（学院，稀有，【唯一】）：魔法书伤害 +6/8/11/14%，魔法书加速 +6/8/11/14。 */
public class MeditationBookItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 6.0D, 8.0D, 11.0D, 14.0D };
	private static final double[] HASTE_BY_LEVEL = { 6.0D, 8.0D, 11.0D, 14.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MeditationBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.meditation_book.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookDamageBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double magicBookHasteBonus(int level) {
		return valueAt(HASTE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
						Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_BONUS)));
	}
}
