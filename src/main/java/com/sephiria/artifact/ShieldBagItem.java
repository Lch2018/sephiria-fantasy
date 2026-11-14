package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 盾牌包（守护 + 学院，羁绊）：【唯一】每 10 点防御力，火焰 / 冰 / 闪电属性伤害 +1/1/2/3。
 *
 * <p>按「防御力 ÷ 10 向下取整」算份数；三元素各加同一份数，加的是元素强度（与「火焰属性伤害」
 * 同一条属性），所以会继续吃「所有元素伤害提升」那一层。
 */
public class ShieldBagItem extends ArtifactItem {
	private static final double[] BONUS_BY_LEVEL = { 1.0D, 1.0D, 2.0D, 3.0D };
	private static final double DEFENSE_PER_STEP = 10.0D;

	public ShieldBagItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.GUARD, ArtifactCombo.ACADEMY);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.shield_bag.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return BONUS_BY_LEVEL.length - 1;
	}

	/** 该玩家当前从盾牌包拿到的元素强度加成（火 / 冰 / 电三元素相同）。 */
	public static double elementBonus(ServerPlayer player, PlayerStats.Element element) {
		if (element == PlayerStats.Element.PHYSICAL) {
			return 0.0D;
		}

		int level = ArtifactEffects.highestLevelOf(player, ModItems.SHIELD_BAG);

		if (level < 0) {
			return 0.0D;
		}

		double steps = Math.floor(PlayerStats.defenseTotal(player) / DEFENSE_PER_STEP);
		return steps * BONUS_BY_LEVEL[Math.max(0, Math.min(BONUS_BY_LEVEL.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		double value = BONUS_BY_LEVEL[Math.max(0, Math.min(BONUS_BY_LEVEL.length - 1, level))];
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.shield_bag",
				Component.literal(Numbers.format(value)).withColor(0xFF55FF55)));
	}
}
