package com.sephiria.client;

import com.sephiria.network.InvulnerablePayload;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端手里的「谁正在无敌」表。
 *
 * <p>服务端在授予无敌时推 {@link InvulnerablePayload} 过来，这里只记剩余刻数与总刻数、
 * 由客户端每刻自己递减——不需要读游戏刻（换世界时也不会因为刻数归零而卡住）。
 *
 * <p>目前只有 HUD 的无敌条读它（{@link #fraction} 给「还剩多少比例」）；
 * 26.3 的实体描边（{@code InvulnerableGlowMixin}）还没移植。
 */
public final class InvulnClientData {
	/** 实体 id → {剩余刻数, 总刻数}。 */
	private static final Map<Integer, int[]> TIMERS = new ConcurrentHashMap<>();

	private InvulnClientData() {
	}

	public static void accept(InvulnerablePayload payload) {
		TIMERS.put(payload.entityId(), new int[]{payload.ticks(), payload.ticks()});
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

	/** 剩余比例（1 → 0）；不在无敌状态时返回 0。 */
	public static float fraction(Entity entity) {
		int[] timer = TIMERS.get(entity.getId());

		if (timer == null || timer[0] <= 0 || timer[1] <= 0) {
			return 0.0F;
		}

		return Math.min(1.0F, timer[0] / (float) timer[1]);
	}

	/** 换世界 / 重连时清空：旧世界的无敌刻数不该带到新世界。 */
	public static void reset() {
		TIMERS.clear();
	}
}
