package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 火焰虫（余烬，稀有品质）：【唯一】灼伤层 +0/1/1/2/3/4（0..5 级）。
 *
 * <p>加在灼伤默认的 2 层上限上（见 {@code Debuffs.applyBurn}）；0 级那一档是 +0，
 * 所以刚拿到的火焰虫不会让上限变高——与电击虫同型。
 */
public class FireBugItem extends ArtifactItem {
	/** 各等级的「灼伤叠加上限」加成（层）。 */
	private static final int[] STACKS_BY_LEVEL = { 0, 1, 1, 2, 3, 4 };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FireBugItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.fire_bug.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return STACKS_BY_LEVEL.length - 1;
	}

	@Override
	public int burnStackBonus(int level) {
		return STACKS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, STACKS_BY_LEVEL.length - 1)];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.burn_stack",
						Component.literal(Numbers.format(this.burnStackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
