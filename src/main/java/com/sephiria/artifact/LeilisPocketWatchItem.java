package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 莱莉的怀表（学院，传说，【唯一】）：魔法书加速 +40/60/80，暴击伤害 +8/14/20%，魔法书 MP 消耗减少 +5/5/10%。 */
public class LeilisPocketWatchItem extends ArtifactItem {
	private static final double[] HASTE_BY_LEVEL = { 40.0D, 60.0D, 80.0D };
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 8.0D, 14.0D, 20.0D };
	private static final double[] MP_REDUCTION_BY_LEVEL = { 5.0D, 5.0D, 10.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LeilisPocketWatchItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.leilis_pocket_watch.flavor";
	}

	@Override
	public boolean unique() {
		return true;
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
	public double critDamageBonus(int level) {
		return valueAt(CRIT_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double magicBookMpReductionGlobalBonus(int level) {
		return valueAt(MP_REDUCTION_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_mp_reduction",
						Component.literal(Numbers.format(this.magicBookMpReductionGlobalBonus(level)))
								.withColor(COLOUR_BONUS)));
	}
}
