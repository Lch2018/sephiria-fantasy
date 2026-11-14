package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 书库之阳（太阳剑 + 学院，羁绊，【唯一】）：
 * 每 1% 魔法书伤害让太阳剑伤害 +1%；造成魔法书伤害时太阳剑额外触发 0/0/1/1/2 次。
 */
public class LibrarySunItem extends ArtifactItem {
	private static final int[] EXTRA_TRIGGERS_BY_LEVEL = { 0, 0, 1, 1, 2 };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LibrarySunItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.SUN_SWORD, ArtifactCombo.ACADEMY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.library_sun.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return EXTRA_TRIGGERS_BY_LEVEL.length - 1;
	}

	@Override
	public boolean librarySunEnabled(int level) {
		return true;
	}

	@Override
	public int librarySunExtraTriggers(int level) {
		return EXTRA_TRIGGERS_BY_LEVEL[Math.max(0, Math.min(EXTRA_TRIGGERS_BY_LEVEL.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.library_sun_scale"),
				Component.translatable("artifact.sephiria_fantasy.affix.library_sun_triggers",
						Component.literal(Numbers.format(this.librarySunExtraTriggers(level)))
								.withColor(COLOUR_BONUS)));
	}
}
