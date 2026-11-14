package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 星红宝石（湖泊，高级）：最终HP +5/10/20%。 */
public class StarRubyItem extends ArtifactItem {
	private static final double[] HP_BY_LEVEL = { 5.0D, 10.0D, 20.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public StarRubyItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.ADVANCED; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.star_ruby.flavor"; }
	@Override public boolean unique() { return false; }
	@Override public int maxLevel() { return 2; }
	@Override public double maxHpPercentBonus(int level) { return valueAt(HP_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.max_hp_percent",
				Component.literal(Numbers.format(this.maxHpPercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
