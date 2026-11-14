package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 稻草人偶（湖泊，普通）：MP再生 +3/6/9、暴击伤害 +2/3/5%。 */
public class ScarecrowDollItem extends ArtifactItem {
	private static final double[] REGEN_BY_LEVEL = { 0.3D, 0.6D, 0.9D };
	private static final double[] CRIT_DAMAGE_BY_LEVEL = { 2.0D, 3.0D, 5.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ScarecrowDollItem(Properties properties) { super(properties); }
	@Override public ArtifactCombo combo() { return ArtifactCombo.LAKE; }
	@Override public ArtifactRarity rarity() { return ArtifactRarity.COMMON; }
	@Override public String flavorKey() { return "artifact.sephiria_fantasy.scarecrow_doll.flavor"; }
	@Override public boolean unique() { return false; }
	@Override public int maxLevel() { return 2; }
	@Override public double mpRegenBonus(int level) { return valueAt(REGEN_BY_LEVEL, level); }
	@Override public double critDamageBonus(int level) { return valueAt(CRIT_DAMAGE_BY_LEVEL, level); }
	@Override public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.crit_damage",
						Component.literal(Numbers.format(this.critDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
