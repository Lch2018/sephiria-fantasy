package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 珍珠粉（学院，高级）：魔法书伤害 +8/10/12%。 */
public class PearlPowderItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 8.0D, 10.0D, 12.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public PearlPowderItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.pearl_powder.flavor";
	}

	@Override
	public boolean unique() {
		return false;
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
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
				Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
