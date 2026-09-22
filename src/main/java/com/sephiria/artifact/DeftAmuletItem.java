package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 灵巧护符（影子，普通品质）：闪避 +3/6（0..1 级）。
 *
 * <p><b>没有【唯一】</b>：带几个就叠几份，影子连击也会跟着多算几级。
 */
public class DeftAmuletItem extends ArtifactItem {
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 3.0D, 6.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public DeftAmuletItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria.deft_amulet.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return DODGE_BY_LEVEL.length - 1;
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)));
	}
}
