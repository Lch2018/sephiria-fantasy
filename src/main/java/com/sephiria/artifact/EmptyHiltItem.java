package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 空剑柄（学院，传说，【唯一】）：魔法书伤害 +5/10/15/20/25/30%，最终 MP +7/9/11/13/19/20%，MP再生 +2/2/2/4/4/6。 */
public class EmptyHiltItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 5.0D, 10.0D, 15.0D, 20.0D, 25.0D, 30.0D };
	private static final double[] MAX_MP_PERCENT_BY_LEVEL = { 7.0D, 9.0D, 11.0D, 13.0D, 19.0D, 20.0D };
	private static final double[] REGEN_BY_LEVEL = { 0.2D, 0.2D, 0.2D, 0.4D, 0.4D, 0.6D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EmptyHiltItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.empty_hilt.flavor";
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
	public double maxMpPercentBonus(int level) {
		return valueAt(MAX_MP_PERCENT_BY_LEVEL, level);
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(REGEN_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
						Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp_percent",
						Component.literal(Numbers.format(this.maxMpPercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)));
	}
}
