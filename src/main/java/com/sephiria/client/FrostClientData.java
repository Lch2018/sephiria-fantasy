package com.sephiria.client;

import com.sephiria.network.FrostSyncPayload;

/**
 * 客户端的冰霜武具状态镜像：HUD 的「冰霜武具：剑 锤 矛 弓x/7」那一行读它。
 *
 * <p>服务端在状态变化时推 {@link FrostSyncPayload}（带没带 / 攒没攒满 / 弓剩几发），这里存下来。
 * 四件都没带时服务端推一份全 false，UI 就跟着消失；进新世界时 {@link #reset()} 清一次，
 * 免得显示上一个世界的残值。
 */
public final class FrostClientData {
	private static boolean hasScabbard;
	private static boolean scabbardReady;
	private static boolean hasHammer;
	private static boolean hammerReady;
	private static boolean hasSpear;
	private static boolean spearReady;
	private static boolean hasBow;
	private static int bowAmmo;
	private static int bowMax;
	private static boolean hasGuillotine;
	private static boolean guillotineReady;

	private FrostClientData() {
	}

	public static void accept(FrostSyncPayload payload) {
		hasScabbard = payload.hasScabbard();
		scabbardReady = payload.scabbardReady();
		hasHammer = payload.hasHammer();
		hammerReady = payload.hammerReady();
		hasSpear = payload.hasSpear();
		spearReady = payload.spearReady();
		hasBow = payload.hasBow();
		bowAmmo = payload.bowAmmo();
		bowMax = payload.bowMax();
		hasGuillotine = payload.hasGuillotine();
		guillotineReady = payload.guillotineReady();
	}

	/** 换世界 / 重连时清空（充能与弹药本来就不跨世界）。 */
	public static void reset() {
		hasScabbard = false;
		scabbardReady = false;
		hasHammer = false;
		hammerReady = false;
		hasSpear = false;
		spearReady = false;
		hasBow = false;
		bowAmmo = 0;
		bowMax = 0;
		hasGuillotine = false;
		guillotineReady = false;
	}

	/** UI 的显示开关：一件都没带就不画那一行。 */
	public static boolean visible() {
		return hasScabbard || hasHammer || hasSpear || hasBow || hasGuillotine;
	}

	public static boolean hasScabbard() {
		return hasScabbard;
	}

	public static boolean scabbardReady() {
		return scabbardReady;
	}

	public static boolean hasHammer() {
		return hasHammer;
	}

	public static boolean hammerReady() {
		return hammerReady;
	}

	public static boolean hasSpear() {
		return hasSpear;
	}

	public static boolean spearReady() {
		return spearReady;
	}

	public static boolean hasBow() {
		return hasBow;
	}

	public static int bowAmmo() {
		return bowAmmo;
	}

	public static int bowMax() {
		return bowMax;
	}

	public static boolean hasGuillotine() {
		return hasGuillotine;
	}

	public static boolean guillotineReady() {
		return guillotineReady;
	}
}
