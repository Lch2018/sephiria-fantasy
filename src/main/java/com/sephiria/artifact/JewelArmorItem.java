package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 宝石盔甲（学院，稀有，【唯一】）：魔法书暴击率 +10/20/30%，MP再生 +4/6/8，物理伤害 -3/5/7。 */
public class JewelArmorItem extends ArtifactItem {
	private static final double[] CRIT_BY_LEVEL = { 10.0D, 20.0D, 30.0D };
	private static final double[] REGEN_BY_LEVEL = { 0.4D, 0.6D, 0.8D };
	private static final double[] PHYSICAL_PENALTY_BY_LEVEL = { -3.0D, -5.0D, -7.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public JewelArmorItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.jewel_armor.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookCritChanceBonus(int level) {
		return valueAt(CRIT_BY_LEVEL, level);
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(REGEN_BY_LEVEL, level);
	}

	@Override
	public double physicalBonus(int level) {
		return valueAt(PHYSICAL_PENALTY_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_crit_chance",
						Component.literal(Numbers.format(this.magicBookCritChanceBonus(level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.physical_minus",
						Component.literal(Numbers.format(-this.physicalBonus(level))).withColor(COLOUR_BONUS)));
	}
}
