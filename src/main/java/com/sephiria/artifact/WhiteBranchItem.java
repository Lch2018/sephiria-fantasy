package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 白色树枝（湖泊，稀有，【唯一】）：MP再生 +5/9/15/22/30（每 10 秒）。 */
public class WhiteBranchItem extends ArtifactItem {
	private static final double[] REGEN_BY_LEVEL = { 0.5D, 0.9D, 1.5D, 2.2D, 3.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WhiteBranchItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.RARE; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.white_branch.flavor"; }
	@Override public boolean unique() { return true; }
	@Override public int maxLevel() { return 4; }
	@Override public double mpRegenBonus(int level) { return valueAt(REGEN_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
				Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)));
	}
}
