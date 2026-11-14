package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 野兽之心（守护，传说）：【唯一】防御力 +4/8/12/16/20、所有伤害放大 +2/4/6/8/10%、幸运值 +2/2/4/4/6。 */
public class BeastHeartItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 4.0D, 8.0D, 12.0D, 16.0D, 20.0D };
	private static final double[] ALL_DAMAGE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 8.0D, 10.0D };
	private static final double[] LUCK_BY_LEVEL = { 2.0D, 2.0D, 4.0D, 4.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BeastHeartItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.beast_heart.flavor";
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
	public double allDamagePercentBonus(int level) {
		return valueAt(ALL_DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double luckBonus(int level) {
		return valueAt(LUCK_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.all_damage",
						Component.literal(Numbers.format(this.allDamagePercentBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.luck",
						Component.literal(Numbers.format(this.luckBonus(level))).withColor(COLOUR_BONUS)));
	}
}
