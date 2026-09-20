package com.sephiria.client;

import com.sephiria.network.StatsSyncPayload;
import com.sephiria.stats.PlayerStats;

/**
 * 客户端的属性镜像：属性面板读它。
 *
 * <p>服务端在进服与每次变更时推 {@link StatsSyncPayload}，这里存下来；没收到过时用默认值，
 * 面板在单机/联机下都能显示合理数字。
 */
public final class ClientStats {
	private static double mp = PlayerStats.DEFAULT_MP;
	private static double mpRegen = PlayerStats.DEFAULT_MP_REGEN;
	private static double physical = PlayerStats.DEFAULT_STRENGTH;
	private static double fire = PlayerStats.DEFAULT_STRENGTH;
	private static double ice = PlayerStats.DEFAULT_STRENGTH;
	private static double lightning = PlayerStats.DEFAULT_STRENGTH;
	private static double defense = PlayerStats.DEFAULT_DEFENSE;
	private static double attackSpeed = PlayerStats.DEFAULT_ATTACK_SPEED;
	private static double meleeRange = PlayerStats.DEFAULT_MELEE_RANGE;

	private ClientStats() {
	}

	public static void accept(StatsSyncPayload payload) {
		mp = payload.mp();
		mpRegen = payload.mpRegen();
		physical = payload.physical();
		fire = payload.fire();
		ice = payload.ice();
		lightning = payload.lightning();
		defense = payload.defense();
		attackSpeed = payload.attackSpeed();
		meleeRange = payload.meleeRange();
	}

	public static double mp() {
		return mp;
	}

	public static double mpRegen() {
		return mpRegen;
	}

	public static double physical() {
		return physical;
	}

	public static double fire() {
		return fire;
	}

	public static double ice() {
		return ice;
	}

	public static double lightning() {
		return lightning;
	}

	public static double defense() {
		return defense;
	}

	public static double attackSpeed() {
		return attackSpeed;
	}

	public static double meleeRange() {
		return meleeRange;
	}
}
