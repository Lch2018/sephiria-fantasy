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
	private static double physicalAmp = 0.0D;
	private static double leaves = 0.0D;
	private static PlayerStats.PhysicalBreakdown attackBreakdown = new PlayerStats.PhysicalBreakdown(
			PlayerStats.DEFAULT_ATTACK_SPEED, 0.0D, 0.0D, 0.0D, 0.0D);
	private static PlayerStats.PhysicalBreakdown breakdown = new PlayerStats.PhysicalBreakdown(
			PlayerStats.DEFAULT_STRENGTH, 0.0D, 0.0D, 0.0D, 0.0D);

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
		physicalAmp = payload.physicalAmp();
		leaves = payload.leaves();
		breakdown = new PlayerStats.PhysicalBreakdown(payload.physicalBase(), payload.artifactFlat(),
				payload.potionFlat(), payload.artifactPercent(), payload.potionPercent());
		attackBreakdown = new PlayerStats.PhysicalBreakdown(payload.attackBase(), payload.attackArtifactFlat(),
				payload.attackPotionFlat(), payload.attackArtifactPercent(), payload.attackPotionPercent());
	}

	/** 伤害倍率：物理强度倍率 ×（1 + 物理伤害增幅），提示框用它把基准伤害换算成实际伤害。 */
	public static double damageMultiplier() {
		return physical / PlayerStats.DEFAULT_STRENGTH * (1.0D + physicalAmp / 100.0D);
	}

	/** 攻击速度倍率（1.0 = 100%）。 */
	public static double attackSpeedMultiplier() {
		return attackSpeed / PlayerStats.DEFAULT_ATTACK_SPEED;
	}

	/** 近战攻击范围倍率（1.0 = 100%）。 */
	public static double rangeMultiplier() {
		return meleeRange / PlayerStats.DEFAULT_MELEE_RANGE;
	}

	/** 物理伤害增幅（%），默认 0。 */
	public static double physicalAmp() {
		return physicalAmp;
	}

	/** 物理强度的来源明细（面板悬停时显示算式）。 */
	public static PlayerStats.PhysicalBreakdown physicalBreakdown() {
		return breakdown;
	}

	/** 树叶（货币）持有量。 */
	public static double leaves() {
		return leaves;
	}

	/** 攻击速度的来源明细（面板悬停时显示算式）。 */
	public static PlayerStats.PhysicalBreakdown attackSpeedBreakdown() {
		return attackBreakdown;
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
