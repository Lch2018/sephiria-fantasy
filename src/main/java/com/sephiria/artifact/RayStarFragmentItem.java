package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷伊星碎片（学院，高级品质）：左侧同一行的魔法书 MP 消耗 -30/35/42/55%。
 *
 * <p>没有【唯一】：带多块时各自只影响自己左边那一格；同一块的效果不叠加给多本书。
 */
public class RayStarFragmentItem extends ArtifactItem {
	private static final double[] MP_REDUCTION_BY_LEVEL = { 30.0D, 35.0D, 42.0D, 55.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	/** 提示框上下文：悬停这块碎片时，左边格子里那本魔法书的名字（客户端渲染前设置、画完清掉）。 */
	private static Component leftBookName;

	public RayStarFragmentItem(Properties properties) {
		super(properties);
	}

	/** 设置左侧魔法书的名字；传 null 等同清除。 */
	public static void setLeftBookName(Component name) {
		leftBookName = name == null ? null : name.copy();
	}

	public static void clearLeftBookName() {
		leftBookName = null;
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.ray_star_fragment.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return MP_REDUCTION_BY_LEVEL.length - 1;
	}

	@Override
	public double magicBookMpReductionBonus(int level) {
		return valueAt(MP_REDUCTION_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component amount = Component.literal(Numbers.format(this.magicBookMpReductionBonus(level)))
				.withColor(COLOUR_BONUS);

		// 悬停在背包里且左边正好是魔法书时，把书的名字一起写出来（名字本身已按品质上色）
		if (leftBookName != null) {
			return java.util.List.of(Component.translatable(
					"artifact.sephiria_fantasy.affix.ray_star_fragment_named", leftBookName, amount));
		}

		return java.util.List.of(Component.translatable(
				"artifact.sephiria_fantasy.affix.ray_star_fragment", amount));
	}
}
