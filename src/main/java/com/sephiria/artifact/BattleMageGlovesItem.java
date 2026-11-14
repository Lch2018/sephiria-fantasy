package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 战斗法师的手套（学院，稀有，【唯一】）：魔法书伤害 +10/12/15/20%，移动速度 +3/4/5/6%。 */
public class BattleMageGlovesItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 10.0D, 12.0D, 15.0D, 20.0D };
	private static final double[] MOVE_BY_LEVEL = { 3.0D, 4.0D, 5.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BattleMageGlovesItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.battle_mage_gloves.flavor";
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
	public double moveSpeedPercentBonus(int level) {
		return valueAt(MOVE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
						Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.move_speed",
						Component.literal(Numbers.format(this.moveSpeedPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
