package com.sephiria.network;

/** 占位：1.20.1 核心版暂不向客户端同步冰霜武具 HUD。 */
public record FrostSyncPayload(boolean hasScabbard, boolean scabbardReady,
		boolean hasHammer, boolean hammerReady,
		boolean hasSpear, boolean spearReady,
		boolean hasBow, int bowAmmo, int bowMax,
		boolean hasGuillotine, boolean guillotineReady) {
	public static FrostSyncPayload none() {
		return new FrostSyncPayload(false, false, false, false, false, false, false, 0, 0, false, false);
	}
}
