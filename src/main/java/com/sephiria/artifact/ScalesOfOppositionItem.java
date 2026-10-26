package com.sephiria.artifact;

import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 对立之秤（冰川 + 余烬，羁绊品质）：【唯一】放在赛菲莉娅背包左半边时给更多火属性伤害、
 * 右半边时给更多冰属性伤害。
 *
 * <p>数值（0..5 级）：较多的一侧 +2/4/6/9/11/15，较少的一侧 +0/1/2/3/4/5。
 *
 * <p><b>双连击</b>神器：同时给冰川与余烬各加 1 级（见 {@link #combos()}）。
 *
 * <p>左右半边的判定与永恒蚀同一套：{@link EternalEclipseItem#sideOfSlot(int)}（6 格宽，
 * 前三列算左半边）。玩法效果现查背包，不依赖物品组件，所以搬动 / 交换会立刻改变加成。
 */
public class ScalesOfOppositionItem extends ArtifactItem {
	/** 较多的一侧给的属性伤害（点）。 */
	private static final double[] MORE_BY_LEVEL = { 2.0D, 4.0D, 6.0D, 9.0D, 11.0D, 15.0D };
	/** 较少的一侧给的属性伤害（点）。 */
	private static final double[] LESS_BY_LEVEL = { 0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ScalesOfOppositionItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GLACIER;
	}

	/** 双连击：冰川与余烬各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.GLACIER, ArtifactCombo.EMBER);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.scales_of_opposition.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MORE_BY_LEVEL.length - 1;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component more = Component.literal("+" + Numbers.format(valueAt(MORE_BY_LEVEL, level)))
				.withColor(COLOUR_BONUS);
		Component less = Component.literal("+" + Numbers.format(valueAt(LESS_BY_LEVEL, level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.scales_sides"),
				Component.translatable("artifact.sephiria_fantasy.affix.scales_fire", more, less),
				Component.translatable("artifact.sephiria_fantasy.affix.scales_ice", more, less));
	}

	/** 背包里最高等级的那一份在哪半边（没有 / 失效 = NONE）。 */
	private static Placed placed(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		Placed best = new Placed(EternalEclipseItem.Side.NONE, -1);

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			var stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof ScalesOfOppositionItem)) {
				continue;
			}

			int level = ArtifactEffects.effectiveLevel(stack, backpack.slotLevel(slot));

			if (level >= 0 && level > best.level()) {
				best = new Placed(EternalEclipseItem.sideOfSlot(slot), level);
			}
		}

		return best;
	}

	/** 较多侧在左半边时给火、右半边时给冰；较少侧反过来。 */
	public static double fireElementBonus(ServerPlayer player) {
		Placed placed = placed(player);

		return switch (placed.side()) {
			case LEFT -> valueAt(MORE_BY_LEVEL, placed.level());
			case RIGHT -> valueAt(LESS_BY_LEVEL, placed.level());
			default -> 0.0D;
		};
	}

	public static double iceElementBonus(ServerPlayer player) {
		Placed placed = placed(player);

		return switch (placed.side()) {
			case LEFT -> valueAt(LESS_BY_LEVEL, placed.level());
			case RIGHT -> valueAt(MORE_BY_LEVEL, placed.level());
			default -> 0.0D;
		};
	}

	/** 背包里的位置 + 实际等级。 */
	private record Placed(EternalEclipseItem.Side side, int level) {
	}
}
