package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 学院锁子甲（守护 + 学院，稀有）：【唯一】防御力 +2/3/4/6/8/10、魔法书加速 +14/18/20/24/29/35。 */
public class AcademyChainmailItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 10.0D };
	private static final double[] HASTE_BY_LEVEL = { 14.0D, 18.0D, 20.0D, 24.0D, 29.0D, 35.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public AcademyChainmailItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.GUARD, ArtifactCombo.ACADEMY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.academy_chainmail.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DEFENSE_BY_LEVEL.length - 1;
	}

	@Override
	public double defenseBonus(int level) {
		return valueAt(DEFENSE_BY_LEVEL, level);
	}

	@Override
	public double magicBookHasteBonus(int level) {
		return valueAt(HASTE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_BONUS)));
	}
}
