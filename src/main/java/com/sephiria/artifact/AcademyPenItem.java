package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 学院钢笔（影子 + 学院，羁绊，【唯一】）：每 5/4/3/2/1 点闪避 +1% 魔法书伤害；
 * 闪避成功时所有魔法书剩余冷却加速 5/10/15/20/25%。
 */
public class AcademyPenItem extends ArtifactItem {
	private static final double[] DODGE_PER_PERCENT_BY_LEVEL = { 5.0D, 4.0D, 3.0D, 2.0D, 1.0D };
	private static final double[] COOLDOWN_ON_DODGE_BY_LEVEL = { 5.0D, 10.0D, 15.0D, 20.0D, 25.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public AcademyPenItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.SHADOW, ArtifactCombo.ACADEMY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.academy_pen.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DODGE_PER_PERCENT_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookDamageFromDodge(int level, double dodge) {
		double per = valueAt(DODGE_PER_PERCENT_BY_LEVEL, level);
		return per <= 0.0D ? 0.0D : Math.floor(Math.max(0.0D, dodge) / per);
	}

	@Override
	public double magicBookCooldownOnDodgePercent(int level) {
		return valueAt(COOLDOWN_ON_DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.academy_pen_damage",
						Component.literal(Numbers.format(valueAt(DODGE_PER_PERCENT_BY_LEVEL, level)))
								.withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_cooldown_on_dodge",
						Component.literal(Numbers.format(this.magicBookCooldownOnDodgePercent(level)))
								.withColor(COLOUR_BONUS)));
	}
}
