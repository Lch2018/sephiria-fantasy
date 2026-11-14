package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 破碎的树根（湖泊，稀有，【唯一】）：最大MP +5/10/20/35/60。 */
public class BrokenRootItem extends ArtifactItem {
	private static final double[] MP_BY_LEVEL = { 5.0D, 10.0D, 20.0D, 35.0D, 60.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BrokenRootItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.RARE; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.broken_root.flavor"; }
	@Override public boolean unique() { return true; }
	@Override public int maxLevel() { return 4; }
	@Override public double maxMpBonus(int level) { return valueAt(MP_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
				Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
