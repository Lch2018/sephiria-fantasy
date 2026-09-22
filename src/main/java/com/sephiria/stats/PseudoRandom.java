package com.sephiria.stats;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 暴击与闪避的判定：伪随机分布（PRD）。
 *
 * <p>不是每一下都独立按标称几率掷骰子，而是「连着不暴击就暗中提高几率」：内部记一个连续未暴击
 * 次数 n，第 n 次的实际几率 = C × (n + 1)，暴击后清零。长期暴击率仍然等于标称值，但不会出现
 * 长时间不暴击的手感问题。
 *
 * <p>C 取自 Dota 那套经典 PRD 常数表（下标 = 标称几率 5%..50%，每 5% 一档，档内线性插值）；
 * 标称几率低于 5% 时把第一档的 C 按比例缩小，高于 50% 时按最后一档算。
 */
public final class PseudoRandom {
	/** 标称几率 5%..50% 对应的 C 值。 */
	private static final double[] PRD_CONSTANTS = {
			0.00380D, 0.01475D, 0.03222D, 0.05570D, 0.08475D,
			0.11895D, 0.15798D, 0.20155D, 0.24931D, 0.30210D };
	/** 表里相邻两档的标称几率差（%）。 */
	private static final double TABLE_STEP = 5.0D;

	/** 暴击：每个玩家「距上次暴击打了几下」。 */
	private static final Map<UUID, Integer> CRIT_MISSES = new HashMap<>();
	/** 闪避：每个玩家「距上次闪避挨了几下」。 */
	private static final Map<UUID, Integer> DODGE_MISSES = new HashMap<>();

	private PseudoRandom() {
	}

	/** 玩家退出时清掉两边的计数。 */
	public static void forget(ServerPlayer player) {
		CRIT_MISSES.remove(player.getUUID());
		DODGE_MISSES.remove(player.getUUID());
	}

	/** 掷一次暴击判定。 */
	public static boolean crit(ServerPlayer player, double chancePercent) {
		return roll(CRIT_MISSES, player, chancePercent);
	}

	/** 掷一次闪避判定：连着没闪掉会逐次提高几率，闪掉后归零。 */
	public static boolean dodge(ServerPlayer player, double chancePercent) {
		return roll(DODGE_MISSES, player, chancePercent);
	}

	/**
	 * 掷一次暴击判定。
	 *
	 * @param chancePercent 标称几率（%）：&lt;= 0 恒不触发，&gt;= 100 恒触发
	 * @return 这一下是否触发
	 */
	private static boolean roll(Map<UUID, Integer> misses, ServerPlayer player, double chancePercent) {
		if (chancePercent <= 0.0D) {
			return false;
		}

		if (chancePercent >= 100.0D) {
			misses.remove(player.getUUID());
			return true;
		}

		int count = misses.getOrDefault(player.getUUID(), 0);
		double chance = Math.min(1.0D, constant(chancePercent) * (count + 1));

		if (player.getRandom().nextDouble() < chance) {
			misses.remove(player.getUUID());
			return true;
		}

		misses.put(player.getUUID(), count + 1);
		return false;
	}

	/** 标称几率（%）→ PRD 常数 C。 */
	private static double constant(double chancePercent) {
		double clamped = Math.min(chancePercent, TABLE_STEP * PRD_CONSTANTS.length);
		int index = Math.min((int) (clamped / TABLE_STEP), PRD_CONSTANTS.length - 1);
		double low = PRD_CONSTANTS[index];
		double high = index + 1 < PRD_CONSTANTS.length ? PRD_CONSTANTS[index + 1] : low;
		double fraction = clamped / TABLE_STEP - index;

		// 低于 5% 时按比例缩小第一档：几率越小，每次提升的幅度也应该越小
		double scale = Math.min(1.0D, chancePercent / TABLE_STEP);

		return (low + (high - low) * fraction) * scale;
	}
}
