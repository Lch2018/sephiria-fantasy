package com.sephiria.stats;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.network.StatsSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家属性：生命值之外的那些数值。
 *
 * <p>生命值不在这里——它就是原版的玩家血量，直接读 {@code LivingEntity} 即可，不需要另存一份
 * （否则两处数据还得互相同步）。这里只管自定义的那几项：
 * 蓝量、蓝量再生、物理强度、火/冰/电元素强度、防御力。
 *
 * <p>数值目前只是存储与展示：面板负责显示，还没有任何战斗逻辑消费它们。
 * 存储只在服务端内存里，玩家重登会回到默认值；变化时推给客户端供面板显示。
 */
public final class PlayerStats {
	/** 默认值：MP 50、MP 再生 1、三种元素与物理强度各 20、防御 0。 */
	public static final double DEFAULT_MP = 50.0D;
	public static final double DEFAULT_MP_REGEN = 1.0D;
	public static final double DEFAULT_STRENGTH = 20.0D;
	public static final double DEFAULT_DEFENSE = 0.0D;
	/** 攻击速度（百分比）：武器面板攻速 = 武器基础攻速 × 它。 */
	public static final double DEFAULT_ATTACK_SPEED = 100.0D;
	/** 近战攻击范围（百分比）：武器的攻击范围与技能伤害范围都乘它。 */
	public static final double DEFAULT_MELEE_RANGE = 100.0D;
	/** 武器伤害（百分比）：赛菲莉亚武器的全部伤害（普攻 + 技能）都乘它；神器伤害不吃。 */
	public static final double DEFAULT_WEAPON_DAMAGE = 100.0D;
	/** 特殊攻击伤害（百分比）：只加成武器技能的伤害。 */
	public static final double DEFAULT_SPECIAL_ATTACK = 100.0D;
	/** 多少点经验换一个升级宝箱。 */
	public static final double EXPERIENCE_PER_CHEST = 1000.0D;

	private static final Map<UUID, Values> STATS = new HashMap<>();

	private PlayerStats() {
	}

	/** 一名玩家的全部自定义属性。 */
	public static final class Values {
		public double mp = DEFAULT_MP;
		public double mpRegen = DEFAULT_MP_REGEN;
		public double physical = DEFAULT_STRENGTH;
		public double fire = DEFAULT_STRENGTH;
		public double ice = DEFAULT_STRENGTH;
		public double lightning = DEFAULT_STRENGTH;
		public double defense = DEFAULT_DEFENSE;
		/** 树叶（货币）：每获得 1 点原版经验 +1。 */
		public double leaves = 0.0D;
		/** 累计获得的经验：每满 1000 发一个升级宝箱（计数器会扣掉已兑换的部分）。 */
		public double experienceTowardsChest = 0.0D;
		public double attackSpeed = DEFAULT_ATTACK_SPEED;
		public double meleeRange = DEFAULT_MELEE_RANGE;
		public double weaponDamage = DEFAULT_WEAPON_DAMAGE;
		public double specialAttack = DEFAULT_SPECIAL_ATTACK;
	}

	/**
	 * 伤害倍率：所有 SEPHIRIA 伤害都是物理伤害，所以这里 = 物理强度倍率 ×（1 + 物理伤害增幅）。
	 *
	 * <p>物理伤害增幅是独立的一条乘算项（默认 0 = 没有增幅），不是加在物理强度上的。
	 */
	public static double damageMultiplier(ServerPlayer player) {
		return physicalTotal(player) / DEFAULT_STRENGTH
				* (1.0D + physicalAmpPercent(player) / 100.0D)
				* (weaponDamageTotal(player) / DEFAULT_WEAPON_DAMAGE);
	}

	/**
	 * 技能伤害倍率：在武器伤害的基础上再乘「特殊攻击伤害」。
	 *
	 * <p>只有武器技能用它——普通攻击不吃特殊攻击伤害。
	 */
	public static double skillDamageMultiplier(ServerPlayer player) {
		return damageMultiplier(player) * (specialAttackTotal(player) / DEFAULT_SPECIAL_ATTACK);
	}

	/** 武器伤害的实际值（%%）：面板值 × (1 + 神器与连击的百分比加成)。 */
	public static double weaponDamageTotal(ServerPlayer player) {
		return of(player).weaponDamage * (1.0D + ArtifactEffects.weaponDamagePercent(player) / 100.0D);
	}

	/** 特殊攻击伤害的实际值（%%）：面板值 + 神器固定加成。 */
	public static double specialAttackTotal(ServerPlayer player) {
		return of(player).specialAttack + ArtifactEffects.specialAttackBonus(player);
	}

	/**
	 * 物理强度的实际值：{@code (基础值 + 固定加成) × (1 + 百分比加成)}。
	 *
	 * <p>「物理伤害 +N」「物理强度 +N」这类固定值先加在基础值上；「物理强度 +x%」这类百分比
	 * 加成先把多个来源加起来，再整体乘上去（不是各自乘一次）。
	 */
	public static double physicalTotal(ServerPlayer player) {
		return physicalBreakdown(player).total();
	}

	/**
	 * 物理伤害增幅（%）：作为独立的一条乘算项，直接作用在最终物理伤害上
	 * （不是加在物理强度上）。默认 0，也就是没有增幅。
	 */
	public static double physicalAmpPercent(ServerPlayer player) {
		return ArtifactEffects.physicalAmpPercent(player);
	}

	/**
	 * 物理强度的来源明细，供面板按来源分色显示，也供结算复用。
	 *
	 * <p>目前只有神器一个来源；药水、树根祝福等来源接进来时在这里各加一项即可。
	 *
	 * <p>「最高元素伤害」如果加在物理强度上，也算神器那一份（同样是神器给的固定值）。
	 */
	public static PhysicalBreakdown physicalBreakdown(ServerPlayer player) {
		double artifactFlat = ArtifactEffects.physicalBonus(player);

		if (highestElement(player) == Element.PHYSICAL) {
			artifactFlat += ArtifactEffects.highestElementBonus(player);
		}

		return new PhysicalBreakdown(of(player).physical, artifactFlat, 0.0D,
				ArtifactEffects.physicalPercentBonus(player), 0.0D);
	}

	/** 四项强度：物理强度与三种元素强度，「最高元素伤害」只在它们之间挑一个。 */
	public enum Element {
		PHYSICAL, FIRE, ICE, LIGHTNING
	}

	/**
	 * 四项强度里数值最高的一项——「最高元素伤害 +N」只加给它。
	 *
	 * <p>比较用的是<b>不含该加成</b>的数值：加成如果参与比较，自己就会改变谁最高，
	 * 会出现「谁加上去谁就最高」的不稳定结果。
	 *
	 * <p>并列时按 物理 → 火 → 冰 → 电 的顺序取靠前的那一项。
	 */
	public static Element highestElement(ServerPlayer player) {
		Values values = of(player);
		Element best = Element.PHYSICAL;
		double highest = physicalBreakdownBase(player);

		if (values.fire > highest) {
			best = Element.FIRE;
			highest = values.fire;
		}

		if (values.ice > highest) {
			best = Element.ICE;
			highest = values.ice;
		}

		if (values.lightning > highest) {
			best = Element.LIGHTNING;
			highest = values.lightning;
		}

		return best;
	}

	/** 不含「最高元素伤害」的物理强度，只给 {@link #highestElement} 比较用（避免自引用）。 */
	private static double physicalBreakdownBase(ServerPlayer player) {
		double flat = of(player).physical + ArtifactEffects.physicalBonus(player);
		return flat * (1.0D + ArtifactEffects.physicalPercentBonus(player) / 100.0D);
	}

	/** 某项强度的实际值（面板显示用）：最高的那一项带上「最高元素伤害」。 */
	public static double elementTotal(ServerPlayer player, Element element) {
		double base = switch (element) {
			case PHYSICAL -> physicalTotal(player);
			case FIRE -> of(player).fire;
			case ICE -> of(player).ice;
			case LIGHTNING -> of(player).lightning;
		};

		// 物理强度那条链在 physicalBreakdown 里已经把加成算进去了，这里不能重复加。
		if (element == Element.PHYSICAL || highestElement(player) != element) {
			return base;
		}

		return base + ArtifactEffects.highestElementBonus(player);
	}

	/**
	 * 攻击速度的来源明细，结构与物理强度那边一样。
	 *
	 * <p>攻击速度的「+N%%」在数值上就是 +N 点（基准 100 = 100%%），所以神器的加成直接进固定值那一段。
	 */
	public static PhysicalBreakdown attackSpeedBreakdown(ServerPlayer player) {
		return new PhysicalBreakdown(of(player).attackSpeed, ArtifactEffects.attackSpeedBonus(player), 0.0D,
				0.0D, 0.0D);
	}

	/** 物理强度的构成：基础 + 各来源固定值，再乘各来源百分比。 */
	public record PhysicalBreakdown(double base, double artifactFlat, double potionFlat,
			double artifactPercent, double potionPercent) {
		/** 括号里那一段（各固定值之和）。 */
		public double flat() {
			return this.base + this.artifactFlat + this.potionFlat;
		}

		/** 百分比那段（各来源百分比之和，单位 %）。 */
		public double percent() {
			return this.artifactPercent + this.potionPercent;
		}

		public double total() {
			return flat() * (1.0D + percent() / 100.0D);
		}
	}

	/** 近战攻击范围倍率：1.0 = 默认值 100%。 */
	public static double rangeMultiplier(ServerPlayer player) {
		return of(player).meleeRange * (1.0D + ArtifactEffects.meleeRangePercent(player) / 100.0D)
				/ DEFAULT_MELEE_RANGE;
	}

	/** 攻击速度倍率：1.0 = 默认值 100%。 */
	public static double attackSpeedMultiplier(ServerPlayer player) {
		return attackSpeedTotal(player) / DEFAULT_ATTACK_SPEED;
	}

	/** 攻击速度的实际值（%）：面板值 + 神器加成。 */
	public static double attackSpeedTotal(ServerPlayer player) {
		return of(player).attackSpeed + ArtifactEffects.attackSpeedBonus(player)
				+ ArtifactEffects.comboAttackSpeedPercent(player);
	}

	/** 树叶持有量。 */
	public static double leaves(ServerPlayer player) {
		return of(player).leaves;
	}

	/**
	 * 直接给树叶（商店出售这类非经验来源）。
	 *
	 * <p>和 {@link #addLeaves} 的区别：这里不推进「每 1000 经验一个升级宝箱」的计数——
	 * 卖东西不是赚经验。
	 */
	public static void grantLeaves(ServerPlayer player, double amount) {
		if (amount <= 0.0D) {
			return;
		}

		of(player).leaves += amount;
		sync(player);
	}

	/** 花掉树叶；不够则返回 false。 */
	public static boolean spendLeaves(ServerPlayer player, double amount) {
		Values values = of(player);

		if (values.leaves < amount) {
			return false;
		}

		values.leaves -= amount;
		sync(player);
		return true;
	}

	/**
	 * 获得经验：同时加树叶，并按每 1000 点发一个升级宝箱。
	 *
	 * <p>宝箱由这里直接塞进玩家背包（背包满了就掉在脚下），所以调用方不用管。
	 */
	public static void addLeaves(ServerPlayer player, int experience) {
		Values values = of(player);
		values.leaves += experience;
		values.experienceTowardsChest += experience;

		while (values.experienceTowardsChest >= EXPERIENCE_PER_CHEST) {
			values.experienceTowardsChest -= EXPERIENCE_PER_CHEST;
			ItemStack chest = new ItemStack(com.sephiria.registry.ModItems.UPGRADE_CHEST);

			if (!player.getInventory().add(chest)) {
				player.drop(chest, false);
			}
		}

		sync(player);
	}

	public static Values of(ServerPlayer player) {
		return STATS.computeIfAbsent(player.getUUID(), uuid -> new Values());
	}

	/** 改完属性后调用：把最新数值推给客户端（物理强度与攻速都用含神器加成的总值）。 */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, StatsSyncPayload.of(of(player), physicalTotal(player),
				elementTotal(player, Element.FIRE), elementTotal(player, Element.ICE),
				elementTotal(player, Element.LIGHTNING), attackSpeedTotal(player), physicalAmpPercent(player),
				physicalBreakdown(player), attackSpeedBreakdown(player), weaponDamageTotal(player),
				specialAttackTotal(player)));
	}

	/** 进服时推一次，面板才有初始值。 */
	public static void syncOnJoin(ServerPlayer player) {
		sync(player);
	}
}
