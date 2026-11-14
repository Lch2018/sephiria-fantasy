package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 发光的沙漏（学院，稀有）：右边格子的魔法书加速 +60/100/140%。 */
public class GlowingHourglassItem extends ArtifactItem {
	private static final double[] HASTE_BY_LEVEL = { 60.0D, 100.0D, 140.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	/** 提示框上下文：悬停沙漏时，右边格子里那本魔法书的名字（客户端渲染前设置、画完清掉）。 */
	private static Component rightBookName;

	public GlowingHourglassItem(Properties properties) {
		super(properties);
	}

	/** 设置右侧魔法书的名字；传 null 等同清除。 */
	public static void setRightBookName(Component name) {
		rightBookName = name == null ? null : name.copy();
	}

	public static void clearRightBookName() {
		rightBookName = null;
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.ACADEMY;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.glowing_hourglass.flavor";
	}

	@Override
	public boolean unique() {
		return false;
	}

	@Override
	public int maxLevel() {
		return HASTE_BY_LEVEL.length - 1;
	}

	@Override
	public double rightMagicBookHasteBonus(int level) {
		return valueAt(HASTE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component amount = Component.literal(Numbers.format(this.rightMagicBookHasteBonus(level)))
				.withColor(COLOUR_BONUS);

		if (rightBookName != null) {
			return java.util.List.of(Component.translatable(
					"artifact.sephiria_fantasy.affix.right_magic_book_haste_named", rightBookName, amount));
		}

		return java.util.List.of(Component.translatable(
				"artifact.sephiria_fantasy.affix.right_magic_book_haste", amount));
	}
}
