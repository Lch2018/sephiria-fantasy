package com.sephiria.artifact;

import com.sephiria.network.BookEffectPayload;

/**
 * 客户端的书效果镜像：只存「效率」给挖掘速度的倍率。
 *
 * <p>服务端在 {@code PlayerStats#sync} 里推 {@link BookEffectPayload}；进新世界时
 * {@link #reset()} 清回 1.0。横扫之刃的冷却倍率在 ATTACK_SPEED 属性上，原版属性同步会处理。
 */
public final class BookEffectData {
	private static float miningSpeedFactor = 1.0F;

	private BookEffectData() {
	}

	public static void accept(BookEffectPayload payload) {
		miningSpeedFactor = Math.max(1.0F, payload.miningSpeedFactor());
	}

	/** 挖掘速度倍率；没有书 / 没有效率时是 1.0。 */
	public static float miningSpeedFactor() {
		return miningSpeedFactor;
	}

	public static void reset() {
		miningSpeedFactor = 1.0F;
	}
}
