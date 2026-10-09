package com.sephiria.client;

import com.sephiria.network.CloudSyncPayload;

/**
 * 客户端的乌云容量镜像：HUD 的乌云 UI 读它。
 *
 * <p>服务端在容量/上限变化时推 {@link CloudSyncPayload}，这里存下来。上限 &gt; 0 才算
 * 「乌云激活」——没激活（连击不到 2 档 / 掉档 / 死亡）时服务端推 0/0，UI 就跟着消失；
 * 进新世界时 {@link #reset()} 清一次，免得显示上一个世界的残值。
 */
public final class CloudClientData {
	private static double capacity;
	private static double max;

	private CloudClientData() {
	}

	public static void accept(CloudSyncPayload payload) {
		capacity = payload.capacity();
		max = payload.max();
	}

	/** 换世界 / 重连时清空（云状态本来就不跨世界）。 */
	public static void reset() {
		capacity = 0.0D;
		max = 0.0D;
	}

	/** 乌云是否激活（连击 ≥ 2 档）；UI 的显示开关。 */
	public static boolean active() {
		return max > 0.0D;
	}

	public static double capacity() {
		return capacity;
	}

	public static double max() {
		return max;
	}
}
