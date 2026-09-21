package com.sephiria.potion;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 药水的持续效果计时器（目前只有「苹果汁」那种按秒回血）。
 *
 * <p>这些效果<b>不走原版的状态效果系统</b>——不占效果栏、牛奶也解不掉，所以自己计时：
 * 每个玩家一串计时器，服务器刻推进，每满 1 秒回一次血。
 *
 * <p>和技能存储一样只存在内存里，玩家重登就没了（药水效果本来就是一次性的，不打算持久化）。
 */
public final class PotionTimers {
	/** 一个还在跑的计时器。 */
	private static final class Ticker {
		private int ticksLeft;
		private final float perSecond;

		Ticker(int ticksLeft, float perSecond) {
			this.ticksLeft = ticksLeft;
			this.perSecond = perSecond;
		}
	}

	private static final Map<UUID, List<Ticker>> TIMERS = new HashMap<>();
	/** 一秒 = 20 刻。 */
	private static final int TICKS_PER_HEAL = 20;

	private PotionTimers() {
	}

	/** 加一个「每秒回复 amount 点生命、持续 ticks 刻」的计时器。 */
	public static void healPerSecond(ServerPlayer player, float amount, int ticks) {
		if (amount <= 0.0F || ticks <= 0) {
			return;
		}

		TIMERS.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>()).add(new Ticker(ticks, amount));
	}

	/** 注册推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(PotionTimers::tick);
	}

	private static void tick(MinecraftServer server) {
		if (TIMERS.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			List<Ticker> tickers = TIMERS.get(player.getUUID());

			if (tickers == null) {
				continue;
			}

			Iterator<Ticker> iterator = tickers.iterator();

			while (iterator.hasNext()) {
				Ticker ticker = iterator.next();

				// 用「剩余刻数能被 20 整除」当整秒：开局立刻回一次，之后每满一秒一次，
				// 总共正好 seconds 次（600 刻 = 30 次）。
				if (ticker.ticksLeft % TICKS_PER_HEAL == 0) {
					player.heal(ticker.perSecond);
				}

				if (--ticker.ticksLeft <= 0) {
					iterator.remove();
				}
			}

			if (tickers.isEmpty()) {
				TIMERS.remove(player.getUUID());
			}
		}
	}
}
