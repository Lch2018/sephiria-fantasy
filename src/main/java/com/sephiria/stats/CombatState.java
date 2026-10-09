package com.sephiria.stats;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 脱战 / 战斗状态：连续 8 秒没挨过「来自生物」的伤害就进入<b>脱战状态</b>，
 * 一挨打立刻回到<b>战斗状态</b>；默认（刚进服、从没挨过打）就是脱战状态。
 *
 * <p>判定口径：只有<b>实扣 &gt; 0 且伤害来源带生物实体</b>的那一下算数——环境伤害（摔落、着火、饥饿）
 * 与玩家自己扣血（献祭类）都不进入战斗状态。状态不落存档：退出重进回到默认的脱战状态。
 *
 * <p>「乌云」（乌云连击）是第一个用它的系统：战斗中容量回得慢、脱战回得快。
 */
public final class CombatState {
	/** 脱战判定：连续多少刻没挨打算脱战（8 秒）。 */
	private static final int OUT_OF_COMBAT_TICKS = 160;
	/** 上次挨「生物伤害」的刻：玩家 → 时间刻。 */
	private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

	private CombatState() {
	}

	/** 挂在伤害事件上（与萤火虫的禁用钩子各自独立）。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F) {
				return;
			}

			long now = entity.level().getGameTime();

			// 挨了来自生物的伤害 → 战斗（环境伤害与自伤不算，见类注释）
			if (entity instanceof ServerPlayer victim
					&& source.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
				LAST_HIT.put(victim.getUUID(), now);
				return;
			}

			// 主动打出伤害（任意方式）也算战斗：触电、桑德耳环、乌云雷击这些都算——
			// 乌云在打，玩家就是在战斗；等没人挨打也没人在打，8 秒后才回脱战让容量回快
			if (source.getEntity() instanceof ServerPlayer attacker && entity != attacker) {
				LAST_HIT.put(attacker.getUUID(), now);
			}
		});
	}

	/** 玩家此刻是否处于战斗状态（挨打后 8 秒内）。 */
	public static boolean isInCombat(ServerPlayer player) {
		Long last = LAST_HIT.get(player.getUUID());
		return last != null && player.level().getGameTime() - last < OUT_OF_COMBAT_TICKS;
	}

	/** 玩家退出时清状态表。 */
	public static void forget(ServerPlayer player) {
		LAST_HIT.remove(player.getUUID());
	}
}
