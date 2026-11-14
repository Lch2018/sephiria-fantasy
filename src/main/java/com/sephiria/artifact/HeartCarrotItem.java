package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 心形胡萝卜（湖泊，稀有，【唯一】）：最大HP +1/2/3/4/5、最大MP +5/10/15/20/30。 */
public class HeartCarrotItem extends ArtifactItem {
	private static final double[] HP_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D, 5.0D };
	private static final double[] MP_BY_LEVEL = { 5.0D, 10.0D, 15.0D, 20.0D, 30.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public HeartCarrotItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.RARE; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.heart_carrot.flavor"; }
	@Override public boolean unique() { return true; }
	@Override public int maxLevel() { return 4; }
	@Override public double maxHpBonus(int level) { return valueAt(HP_BY_LEVEL, level); }
	@Override public double maxMpBonus(int level) { return valueAt(MP_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.max_hp",
						Component.literal(Numbers.format(this.maxHpBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
						Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
