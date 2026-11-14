package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 布甲（守护，普通）：防御力 +3/6。 */
public class ClothArmorItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 3.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ClothArmorItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.cloth_armor.flavor";
	}

	@Override
	public boolean unique() {
		return false;
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
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.defense",
				Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)));
	}
}
