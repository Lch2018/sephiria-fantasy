package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 乔洛的浇水壶（湖泊 + 学院，羁绊，【唯一】）：最大 MP 达到 500/400/320/250/200 时，
 * 施放魔法书额外无蓝耗再施放一次；魔法书伤害 +3/6/9/12/15%。
 */
public class JoloWateringCanItem extends ArtifactItem {
	private static final int[] THRESHOLD_BY_LEVEL = { 500, 400, 320, 250, 200 };
	private static final double[] DAMAGE_BY_LEVEL = { 3.0D, 6.0D, 9.0D, 12.0D, 15.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public JoloWateringCanItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.LAKE;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.LAKE, ArtifactCombo.ACADEMY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.jolo_watering_can.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 4;
	}

	@Override
	public int wateringCanMaxMpThreshold(int level) {
		return THRESHOLD_BY_LEVEL[Math.max(0, Math.min(THRESHOLD_BY_LEVEL.length - 1, level))];
	}

	@Override
	public double magicBookDamageBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.jolo_watering_can",
						Component.literal(Numbers.format(this.wateringCanMaxMpThreshold(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.magic_book_damage",
						Component.literal(Numbers.format(this.magicBookDamageBonus(level))).withColor(COLOUR_BONUS)));
	}
}
