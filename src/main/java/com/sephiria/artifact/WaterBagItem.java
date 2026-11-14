package com.sephiria.artifact;

import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 水袋（湖泊，高级，【唯一】）：每次消耗 MP 时最大 MP +1，冷却 5 秒；
 * 上限按等级 10/15/20/25。
 */
public class WaterBagItem extends ArtifactItem {
	private static final int[] CAP_BY_LEVEL = { 10, 15, 20, 25 };
	private static final int COOLDOWN_TICKS = 100;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private static final class State {
		private int bonus;
		private long lastGainTick = Long.MIN_VALUE;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	public WaterBagItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.LAKE;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.water_bag.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CAP_BY_LEVEL.length - 1;
	}

	/** 当前生效等级的水袋状态；卸下 / 失效时清零。 */
	public static int bonusFor(ServerPlayer player) {
		int level = ArtifactEffects.highestLevelOf(player, com.sephiria.registry.ModItems.WATER_BAG);

		if (level < 0) {
			STATES.remove(player.getUUID());
			return 0;
		}

		State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
		state.bonus = Math.min(state.bonus, CAP_BY_LEVEL[Math.min(level, CAP_BY_LEVEL.length - 1)]);
		return state.bonus;
	}

	/** MP 真正扣掉后调用；5 秒冷却，每次 +1。 */
	public static void onMpSpent(ServerPlayer player, double amount) {
		if (amount <= 0.0D) {
			return;
		}

		int level = ArtifactEffects.highestLevelOf(player, com.sephiria.registry.ModItems.WATER_BAG);

		if (level < 0) {
			return;
		}

		State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
		long now = player.level().getGameTime();

		if (state.lastGainTick != Long.MIN_VALUE && now - state.lastGainTick < COOLDOWN_TICKS) {
			return;
		}

		int cap = CAP_BY_LEVEL[Math.min(level, CAP_BY_LEVEL.length - 1)];

		if (state.bonus >= cap) {
			return;
		}

		state.bonus++;
		state.lastGainTick = now;
	}

	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.water_bag",
				Component.literal(Numbers.format(CAP_BY_LEVEL[Math.min(level, CAP_BY_LEVEL.length - 1)]))
						.withColor(COLOUR_BONUS)));
	}
}
