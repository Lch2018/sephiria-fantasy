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
	private static double maxMp = PlayerStats.DEFAULT_MP;
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
	private static double weaponDamage = PlayerStats.DEFAULT_WEAPON_DAMAGE;
	private static double specialAttack = PlayerStats.DEFAULT_SPECIAL_ATTACK;
	private static double lifesteal = 0.0D;
	private static double normalAttackDamage = PlayerStats.DEFAULT_NORMAL_ATTACK_DAMAGE;
	private static double critChance = PlayerStats.DEFAULT_CRIT_CHANCE;
	private static double critDamage = PlayerStats.DEFAULT_CRIT_DAMAGE;
	private static double ignoreDefense = 0.0D;
	private static double moveSpeed = PlayerStats.DEFAULT_MOVE_SPEED;
	private static double dodge = 0.0D;
	private static double dodgeRate = 0.0D;
	private static double negotiation = PlayerStats.DEFAULT_NEGOTIATION;
	private static double shopDiscount = 0.0D;
	private static double leafGainPercent = PlayerStats.DEFAULT_LEAF_GAIN;
	private static double xpDropPercent = PlayerStats.DEFAULT_XP_DROP;
	private static PlayerStats.PhysicalBreakdown attackBreakdown = new PlayerStats.PhysicalBreakdown(
			PlayerStats.DEFAULT_ATTACK_SPEED, 0.0D, 0.0D, 0.0D, 0.0D);
	private static PlayerStats.PhysicalBreakdown breakdown = new PlayerStats.PhysicalBreakdown(
			PlayerStats.DEFAULT_STRENGTH, 0.0D, 0.0D, 0.0D, 0.0D);

	private ClientStats() {
	}

	public static void accept(StatsSyncPayload payload) {
		mp = payload.mp();
		maxMp = payload.maxMp();
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
		weaponDamage = payload.weaponDamage();
		specialAttack = payload.specialAttack();
		lifesteal = payload.lifesteal();
		normalAttackDamage = payload.normalAttackDamage();
		critChance = payload.critChance();
		critDamage = payload.critDamage();
		ignoreDefense = payload.ignoreDefense();
		moveSpeed = payload.moveSpeed();
		dodge = payload.dodge();
		dodgeRate = payload.dodgeRate();
		negotiation = payload.negotiation();
		shopDiscount = payload.shopDiscount();
		leafGainPercent = payload.leafGainPercent();
		xpDropPercent = payload.xpDropPercent();
		breakdown = new PlayerStats.PhysicalBreakdown(payload.physicalBase(), payload.artifactFlat(),
				payload.potionFlat(), payload.artifactPercent(), payload.potionPercent());
		attackBreakdown = new PlayerStats.PhysicalBreakdown(payload.attackBase(), payload.attackArtifactFlat(),
				payload.attackPotionFlat(), payload.attackArtifactPercent(), payload.attackPotionPercent());
	}

	/** 普通攻击倍率：与服务端公式一致（物理强度 ×（1 + 物理伤害增幅）× 武器伤害 × 普通攻击伤害）。 */
	public static double damageMultiplier() {
		return physical / PlayerStats.DEFAULT_STRENGTH * (1.0D + physicalAmp / 100.0D)
				* (weaponDamage / PlayerStats.DEFAULT_WEAPON_DAMAGE)
				* (normalAttackDamage / PlayerStats.DEFAULT_NORMAL_ATTACK_DAMAGE);
	}

	/** 技能伤害倍率：伤害倍率再乘「特殊攻击伤害」——只有武器技能吃这一项。 */
	public static double skillDamageMultiplier() {
		return damageMultiplier() * (specialAttack / PlayerStats.DEFAULT_SPECIAL_ATTACK);
	}

	/** 攻击速度倍率（1.0 = 100%）。 */
	public static double attackSpeedMultiplier() {
		return attackSpeed / PlayerStats.DEFAULT_ATTACK_SPEED;
	}

	/** 近战攻击范围倍率（1.0 = 100%）。 */
	public static double rangeMultiplier() {
		return meleeRange / PlayerStats.DEFAULT_MELEE_RANGE;
	}

	/** 普通攻击伤害（%），默认 100。 */
	public static double normalAttackDamage() {
		return normalAttackDamage;
	}

	/** 暴击几率（%），默认 0。 */
	public static double critChance() {
		return critChance;
	}

	/** 暴击伤害（%），默认 150。 */
	public static double critDamage() {
		return critDamage;
	}

	/** 无视防御伤害（点），默认 0。 */
	public static double ignoreDefense() {
		return ignoreDefense;
	}

	/** 移动速度（%），默认 100。 */
	public static double moveSpeed() {
		return moveSpeed;
	}

	/** 闪避（点），默认 0。 */
	public static double dodge() {
		return dodge;
	}

	/** 闪避率（%）：面板直接显示它。 */
	public static double dodgeRate() {
		return dodgeRate;
	}

	/** 谈判力（点），默认 0：给商店物品打折。 */
	public static double negotiation() {
		return negotiation;
	}

	/** 商店折扣（%）：谈判力换算出来的，面板与商店价签共用。 */
	public static double shopDiscount() {
		return shopDiscount;
	}

	/** 叶子获得量（%），默认 100：结算叶子时按它放大。 */
	public static double leafGainPercent() {
		return leafGainPercent;
	}

	/** 经验掉落（%），默认 100：入账经验先按它放大。 */
	public static double xpDropPercent() {
		return xpDropPercent;
	}

	/** 物理伤害增幅（%），默认 0。 */
	public static double physicalAmp() {
		return physicalAmp;
	}

	/** 物理强度的来源明细（面板悬停时显示算式）。 */
	public static PlayerStats.PhysicalBreakdown physicalBreakdown() {
		return breakdown;
	}

	/** 武器伤害（%），默认 100。 */
	public static double weaponDamage() {
		return weaponDamage;
	}

	/** 特殊攻击伤害（%），默认 100。 */
	public static double specialAttack() {
		return specialAttack;
	}

	/** 叶子（货币）持有量。 */
	public static double leaves() {
		return leaves;
	}

	/** HP 偷取（俗称吸血）：造成伤害的 0.1% 回血。 */
	public static double lifesteal() {
		return lifesteal;
	}

	/** 攻击速度的来源明细（面板悬停时显示算式）。 */
	public static PlayerStats.PhysicalBreakdown attackSpeedBreakdown() {
		return attackBreakdown;
	}

	public static double mp() {
		return mp;
	}

	/** 最大蓝量（点）：被剑耳环削过的话 HUD 与技能蓝耗校验都用它。 */
	public static double maxMp() {
		return maxMp;
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
