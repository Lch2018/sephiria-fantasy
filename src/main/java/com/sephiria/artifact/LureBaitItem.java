package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 诱惑的鱼饵（湖泊 + 元素，高级，【唯一】）：海之眷顾 / 饵钓 I..III、MP再生、四项属性伤害 +1/1/2。 */
public class LureBaitItem extends ArtifactItem {
	private static final double[] REGEN_BY_LEVEL = { 0.3D, 0.5D, 0.8D };
	private static final double[] FLAT_BY_LEVEL = { 1.0D, 1.0D, 2.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LureBaitItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.LAKE;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.LAKE, ArtifactCombo.ELEMENT);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.lure_bait.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 2;
	}

	@Override
	public int fishingLuckBonus(int level) {
		return Math.min(3, level + 1);
	}

	@Override
	public int fishingLureBonus(int level) {
		return Math.min(3, level + 1);
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(REGEN_BY_LEVEL, level);
	}

	@Override
	public double physicalBonus(int level) {
		return valueAt(FLAT_BY_LEVEL, level);
	}

	@Override
	public double fireElementBonus(int level) {
		return valueAt(FLAT_BY_LEVEL, level);
	}

	@Override
	public double iceElementBonus(int level) {
		return valueAt(FLAT_BY_LEVEL, level);
	}

	@Override
	public double lightningElementBonus(int level) {
		return valueAt(FLAT_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.fishing_enchant",
						Component.literal(Numbers.format(this.fishingLuckBonus(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(this.fishingLureBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.fire_element",
						Component.literal(Numbers.format(this.fireElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_element",
						Component.literal(Numbers.format(this.iceElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_element",
						Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.physical",
						Component.literal(Numbers.format(this.physicalBonus(level))).withColor(COLOUR_BONUS)));
	}
}
