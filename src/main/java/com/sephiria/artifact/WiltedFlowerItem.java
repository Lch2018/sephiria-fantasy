package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 枯萎的花（湖泊，普通）：暴击几率 +2/4/6%、最大MP +2/4/6。 */
public class WiltedFlowerItem extends ArtifactItem {
	private static final double[] CRIT_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	private static final double[] MP_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WiltedFlowerItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.COMMON; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.wilted_flower.flavor"; }
	@Override public boolean unique() { return false; }
	@Override public int maxLevel() { return 2; }
	@Override public double critChanceBonus(int level) { return valueAt(CRIT_BY_LEVEL, level); }
	@Override public double maxMpBonus(int level) { return valueAt(MP_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.crit_chance",
						Component.literal(Numbers.format(this.critChanceBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
						Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
