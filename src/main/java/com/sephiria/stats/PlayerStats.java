package com.sephiria.stats;

import com.sephiria.network.StatsSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

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
		public double attackSpeed = DEFAULT_ATTACK_SPEED;
		public double meleeRange = DEFAULT_MELEE_RANGE;
	}

	/** 鐗╃悊寮哄害鍊嶇巼锛?.0 = 榛樿鍊?20銆?*/
	public static double damageMultiplier(ServerPlayer player) {
		return of(player).physical / DEFAULT_STRENGTH;
	}

	/** 杩戞垬鏀诲嚮鑼冨洿鍊嶇巼锛?.0 = 榛樿鍊?100%銆?*/
	public static double rangeMultiplier(ServerPlayer player) {
		return of(player).meleeRange / DEFAULT_MELEE_RANGE;
	}

	/** 鏀诲嚮閫熷害鍊嶇巼锛?.0 = 榛樿鍊?100%銆?*/
	public static double attackSpeedMultiplier(ServerPlayer player) {
		return of(player).attackSpeed / DEFAULT_ATTACK_SPEED;
	}

	public static Values of(ServerPlayer player) {
		return STATS.computeIfAbsent(player.getUUID(), uuid -> new Values());
	}

	/** 改完属性后调用：把最新数值推给客户端。 */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, StatsSyncPayload.of(of(player)));
	}

	/** 进服时推一次，面板才有初始值。 */
	public static void syncOnJoin(ServerPlayer player) {
		sync(player);
	}
}
