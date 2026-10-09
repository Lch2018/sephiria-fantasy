package com.sephiria.client;

import com.sephiria.network.SunSwordSyncPayload;

/**
 * 客户端的太阳剑数量镜像：HUD 的太阳剑 UI 读它。
 *
 * <p>服务端在数量/上限变化时推 {@link SunSwordSyncPayload}，这里存下来。上限 &gt; 0 才算
 * 「太阳剑激活」——没激活（连击不到 2 档 / 掉档 / 退出）时服务端推 0/0，UI 就跟着消失；
 * 进新世界时 {@link #reset()} 清一次，免得显示上一个世界的残值。
 */
public final class SunSwordClientData {
	private static double current;
	private static double max;

	private SunSwordClientData() {
	}

	public static void accept(SunSwordSyncPayload payload) {
		current = payload.current();
		max = payload.max();
	}

	/** 换世界 / 重连时清空（太阳剑数量本来就不跨世界）。 */
	public static void reset() {
		current = 0.0D;
		max = 0.0D;
	}

	/** 太阳剑是否激活（连击 ≥ 2 档）；UI 的显示开关。 */
	public static boolean active() {
		return max > 0.0D;
	}

	public static double current() {
		return current;
	}

	public static double max() {
		return max;
	}
}
