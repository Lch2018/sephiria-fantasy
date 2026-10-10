package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 电击虫（魔法科技，稀有品质）：【唯一】触电叠加上限 +0/1/1/2/2/3（0..5 级）。
 *
 * <p>「触电」整段共享一份时间轴，层数只是伤害乘数；这里加的是<b>上限</b>——
 * 上限 = 默认 2 层 + 这一项（见 {@code Debuffs#applyShock}）。
 */
public class ElectricBugItem extends ArtifactItem {
	/** 各等级给的触电叠加上限（层）。 */
	private static final int[] STACKS_BY_LEVEL = { 0, 1, 1, 2, 2, 3 };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ElectricBugItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.electric_bug.flavor";
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
	public int shockStackBonus(int level) {
		return STACKS_BY_LEVEL[Mth.clamp(level, 0, STACKS_BY_LEVEL.length - 1)];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.shock_stacks",
				Component.literal(Numbers.format(this.shockStackBonus(level))).withColor(COLOUR_BONUS)));
	}
}
