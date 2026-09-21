package com.sephiria.artifact;

import com.sephiria.util.Numbers;

/** 品质：决定名字与「X 神器 / X 石板」那一行的颜色。 */
public enum ArtifactRarity {
	/** 白：神器与石板的入门品质。 */
	COMMON("common", 0xFFFFFFFF),
	/** 蓝。 */
	ADVANCED("advanced", 0xFF5B8CFF),
	/** 黄。 */
	RARE("rare", 0xFFFFD24A),
	/** 红。 */
	LEGENDARY("legendary", 0xFFFF5555),
	/** 彩色（逐字变色）：神器的最高品质。 */
	BOND("bond", 0xFFB060FF),
	/** 粉：石板的最高品质。 */
	ETERNAL("eternal", 0xFFFF7FD0);

	private final String id;
	private final int color;

	ArtifactRarity(String id, int color) {
		this.id = id;
		this.color = color;
	}

	public String id() {
		return this.id;
	}

	/** ARGB 颜色；{@link #BOND} 是彩色，实际渲染走逐字上色，这里给一个代表色。 */
	public int color() {
		return this.color;
	}

	public String translationKey() {
		return "sephiria.rarity." + this.id;
	}

	/** 名字用的颜色：彩色品质逐字上色。 */
	public net.minecraft.network.chat.MutableComponent style(String name) {
		if (this == BOND) {
			return com.sephiria.util.Colours.rainbow(name);
		}

		return net.minecraft.network.chat.Component.literal(name).withColor(this.color);
	}
}
