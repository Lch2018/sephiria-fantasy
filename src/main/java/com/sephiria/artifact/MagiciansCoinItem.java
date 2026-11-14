package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 魔法师的硬币（学院，普通品质）：【唯一】魔法书加速 +15/20/30；奖励中出现魔法书的几率 +25%。
 *
 * <p>魔法书加速每 1 点让魔法书技能的冷却速度 +1%（见 {@code ArtifactSkills#cooldownTicksFor}）；
 * 奖励概率由 {@code ArtifactEffects.magicBookRewardChancePercent} 汇总，商店 / 宝箱 / 抽卡 / 选卡共用。
 */
public class MagiciansCoinItem extends ArtifactItem {
	private static final double[] HASTE_BY_LEVEL = { 15.0D, 20.0D, 30.0D };
	private static final double REWARD_CHANCE = 25.0D;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MagiciansCoinItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.magicians_coin.flavor";
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
	public double magicBookRewardChanceBonus(int level) {
		return REWARD_CHANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_haste",
						Component.literal(Numbers.format(this.magicBookHasteBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_reward_chance",
						Component.literal(Numbers.format(REWARD_CHANCE)).withColor(COLOUR_BONUS)));
	}
}
