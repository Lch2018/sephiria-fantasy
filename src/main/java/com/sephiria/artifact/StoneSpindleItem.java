package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 石纺锤（湖泊，稀有，【唯一】）：消耗MP的能力伤害 +6/12/18/24/30%。 */
public class StoneSpindleItem extends ArtifactItem {
	private static final double[] DAMAGE_BY_LEVEL = { 6.0D, 12.0D, 18.0D, 24.0D, 30.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public StoneSpindleItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.RARE; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.stone_spindle.flavor"; }
	@Override public boolean unique() { return true; }
	@Override public int maxLevel() { return 4; }
	@Override public double mpAbilityDamageBonus(int level) { return valueAt(DAMAGE_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.mp_ability_damage",
				Component.literal(Numbers.format(this.mpAbilityDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
