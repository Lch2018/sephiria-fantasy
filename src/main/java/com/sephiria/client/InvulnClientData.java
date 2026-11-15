package com.sephiria.client;

import com.sephiria.network.InvulnerablePayload;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端手里的"谁正在无敌"表。
 *
 * <p>服务端在授予无敌时推 {@link InvulnerablePayload} 过来，这里只记剩余刻数、总刻数与
 * 描边颜色，由客户端每刻自己递减——不需要读游戏刻（26.2 客户端世界字段的访问方式变过，这样写更稳）。
 *
 * <p>三个用途：实体描边（{@code InvulnerableGlowMixin}）、描边颜色
 * （{@code InvulnerableGlowColorMixin}）和 HUD 的无敌条
 * （需要 {@link #fraction} 这个"还剩多少比例"的值）。
 */
public final class InvulnClientData {
	/** 实体 id → {剩余刻数, 总刻数, 描边颜色}。 */
	private static final Map<Integer, int[]> TIMERS = new ConcurrentHashMap<>();

	private InvulnClientData() {
	}

	public static void accept(InvulnerablePayload payload) {
		TIMERS.put(payload.entityId(), new int[]{payload.ticks(), payload.ticks(), payload.color()});
	}

	/** 每客户端刻递减一次（由 SephiriaClient 的 tick 回调调用）。 */
	public static void tick() {
		TIMERS.entrySet().removeIf(entry -> entry.getValue()[0] <= 0);
		TIMERS.replaceAll((id, timer) -> {
			timer[0]--;
			return timer;
		});
	}

	public static boolean isActive(Entity entity) {
		int[] timer = TIMERS.get(entity.getId());
		return timer != null && timer[0] > 0;
	}

	/** 无敌描边颜色（ARGB）；不在无敌状态时返回白色。 */
	public static int color(Entity entity) {
		int[] timer = TIMERS.get(entity.getId());
		return timer == null || timer[0] <= 0 || timer.length < 3 ? 0xFFFFFFFF : timer[2];
	}

	/** 剩余比例（1 → 0）；不在无敌状态时返回 0。 */
	public static float fraction(Entity entity) {
		int[] timer = TIMERS.get(entity.getId());

		if (timer == null || timer[0] <= 0 || timer[1] <= 0) {
			return 0.0F;
		}

		return Math.min(1.0F, timer[0] / (float) timer[1]);
	}
}
