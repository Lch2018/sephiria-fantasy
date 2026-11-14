package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 奥布拉斯之血（湖泊，高级，【唯一】）：MP吸收 +3/6/9/12。 */
public class ObrasBloodItem extends ArtifactItem {
	private static final double[] ABSORB_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ObrasBloodItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.ADVANCED; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.obras_blood.flavor"; }
	@Override public boolean unique() { return true; }
	@Override public int maxLevel() { return 3; }
	@Override public double mpAbsorbBonus(int level) { return valueAt(ABSORB_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.mp_absorb",
				Component.literal(Numbers.format(this.mpAbsorbBonus(level))).withColor(COLOUR_BONUS)));
	}
}
