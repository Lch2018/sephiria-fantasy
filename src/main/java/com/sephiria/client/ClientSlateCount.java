package com.sephiria.client;

import com.sephiria.network.SlateCountPayload;

/**
 * 客户端的石板数量镜像：黑鳞片提示框里的「当前持有石板数量」读它。
 *
 * <p>服务端在 {@code PlayerStats#sync} 里推 {@link SlateCountPayload}；进新世界时
 * {@link #reset()} 清一次，免得沿用上一个世界的数字。
 */
public final class ClientSlateCount {
	private static int count;

	private ClientSlateCount() {
	}

	public static void accept(SlateCountPayload payload) {
		count = Math.max(0, payload.count());
	}

	public static int count() {
		return count;
	}

	public static void reset() {
		count = 0;
	}
}
