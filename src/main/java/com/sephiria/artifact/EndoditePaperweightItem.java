package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 恩多迪特的镇纸（湖泊，普通）：MP再生 +3/6/9、最大MP +3/6/9。 */
public class EndoditePaperweightItem extends ArtifactItem {
	private static final double[] REGEN_BY_LEVEL = { 0.3D, 0.6D, 0.9D };
	private static final double[] MP_BY_LEVEL = { 3.0D, 6.0D, 9.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public EndoditePaperweightItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.COMMON; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.endodite_paperweight.flavor"; }
	@Override public boolean unique() { return false; }
	@Override public int maxLevel() { return 2; }
	@Override public double mpRegenBonus(int level) { return valueAt(REGEN_BY_LEVEL, level); }
	@Override public double maxMpBonus(int level) { return valueAt(MP_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.max_mp",
						Component.literal(Numbers.format(this.maxMpBonus(level))).withColor(COLOUR_BONUS)));
	}
}
