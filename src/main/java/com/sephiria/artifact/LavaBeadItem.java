package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 熔岩珠（余烬，传说品质）：【唯一】赋予灼伤时额外给予 0/1/1/2 次、灼伤层 +1/1/1/2（0..3 级）。
 *
 * <p>「额外给予」= 挂灼伤那一下多叠几层（见 {@code Debuffs.applyBurn}）：原本 1 层，
 * 3 级时一次挂上 1 + 2 = 3 层（层数上限照旧管着）。「灼伤层」与火焰虫走同一条汇总
 * （{@code ArtifactEffects.burnStackBonus}），两者相加。
 */
public class LavaBeadItem extends ArtifactItem {
	/** 各等级的「赋予灼伤时额外给予的次数」。 */
	private static final int[] EXTRA_BY_LEVEL = { 0, 1, 1, 2 };
	/** 各等级的「灼伤叠加上限」加成（层）。 */
	private static final int[] STACKS_BY_LEVEL = { 1, 1, 1, 2 };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LavaBeadItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.EMBER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.lava_bead.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return EXTRA_BY_LEVEL.length - 1;
	}

	@Override
	public int burnExtraApplications(int level) {
		return EXTRA_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, EXTRA_BY_LEVEL.length - 1)];
	}

	@Override
	public int burnStackBonus(int level) {
		return STACKS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, STACKS_BY_LEVEL.length - 1)];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.burn_extra",
						Component.literal(Numbers.format(this.burnExtraApplications(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria.affix.burn_stack",
						Component.literal(Numbers.format(this.burnStackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
