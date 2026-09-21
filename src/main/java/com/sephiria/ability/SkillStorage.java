package com.sephiria.ability;

import com.sephiria.network.SkillSyncPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 技能存储：给"有次数/有资源"的技能用的通用池子。
 *
 * <p>每个技能注册一份配置——存储总量、每次回复量、回复间隔；每个玩家各有一份当前值。
 * 对外只有两个动作：
 * <ul>
 *   <li>{@link #consume} 消耗：够就扣掉并返回 true，不够就原样返回 false（调用方自己决定失败表现）</li>
 *   <li>{@link #regenerate} 回复：加上回复量并封顶到存储总量</li>
 * </ul>
 *
 * <p>回复是自动的，由服务器刻推进：<b>只有当前量没满时才计时</b>，每累计满
 * 一个回复间隔就 {@code +回复量}；一旦达到存储总量就停止回复并把计时归零
 * （所以满仓之后再用，要重新攒满一个间隔才会回复）。
 *
 * <p>状态目前只存在内存里，玩家重新登录会回到满仓。将来要做 HUD 显示或跨会话保留，
 * 再把它换成挂在玩家身上的数据（Fabric 的 data attachment，可以持久化 + 同步给客户端）。
 */
public final class SkillStorage {
	/** 每个技能的配置。 */
	private record Config(double max, double regenAmount, int regenIntervalTicks, double initial) {
	}

	private static final Map<Identifier, Config> CONFIGS = new LinkedHashMap<>();
	/** 按玩家的上限加成（风之歌 10 级给冲刺 +1 次这种）。 */
	private static final Map<Identifier, java.util.function.ToIntFunction<ServerPlayer>> MAX_BONUS = new LinkedHashMap<>();
	private static final Map<UUID, Map<Identifier, Pool>> POOLS = new HashMap<>();

	private SkillStorage() {
	}

	/**
	 * 注册一个技能的存储配置。
	 *
	 * @param skill              技能 id
	 * @param max                存储总量
	 * @param regenAmount        每次回复量
	 * @param regenIntervalTicks 回复间隔（tick）
	 */
	public static void register(Identifier skill, double max, double regenAmount, int regenIntervalTicks) {
		register(skill, max, regenAmount, regenIntervalTicks, max);
	}

	/**
	 * 同上，但可以指定开局值。
	 *
	 * <p>次数型资源（冲刺、弹匣）开局就该是满的，所以默认满仓；而「要攒起来」的资源
	 * （刀的剑意）开局必须是空的，否则第一次挥刀就直接放技能了。
	 */
	public static void register(Identifier skill, double max, double regenAmount, int regenIntervalTicks, double initial) {
		CONFIGS.put(skill, new Config(max, regenAmount, regenIntervalTicks, initial));
	}

	/**
	 * 注册一个<b>不自动回复</b>的技能：回复完全由调用方自己调 {@link #regenerate}
	 * （弩的装填就是这种——只有换弹完成才加弹）。
	 */
	public static void registerManual(Identifier skill, double max) {
		registerManual(skill, max, max);
	}

	/** 不自动回复，且开局是空的（见 {@link #register} 的重载）。 */
	public static void registerManual(Identifier skill, double max, double initial) {
		register(skill, max, 0.0D, 0, initial);
	}

	/** 注册回复推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(SkillStorage::tick);
	}

	/**
	 * 给某个技能注册「按玩家的上限加成」。
	 *
	 * <p>上限变化会跟着 {@link #syncAll} / 每次同步一起推给客户端，所以 HUD 的 x/y 会跟着变。
	 */
	public static void registerMaxBonus(Identifier skill, java.util.function.ToIntFunction<ServerPlayer> bonus) {
		MAX_BONUS.put(skill, bonus);
	}

	/** 某个玩家在该技能上的实际上限：注册值 + 加成。 */
	public static double max(ServerPlayer player, Identifier skill) {
		java.util.function.ToIntFunction<ServerPlayer> bonus = MAX_BONUS.get(skill);
		return max(skill) + (bonus == null ? 0.0D : bonus.applyAsInt(player));
	}

	/** 现有存储量。 */
	public static double current(ServerPlayer player, Identifier skill) {
		return pool(player, skill).current;
	}

	/** 存储总量；没注册过的技能返回 0。 */
	public static double max(Identifier skill) {
		Config config = CONFIGS.get(skill);
		return config == null ? 0.0D : config.max();
	}

	/** 消耗：返回是否扣得起。 */
	public static boolean consume(ServerPlayer player, Identifier skill, double amount) {
		Pool pool = pool(player, skill);

		if (pool.current < amount) {
			return false;
		}

		pool.current -= amount;
		sync(player, skill, pool);
		return true;
	}

	/** 回复：加上回复量并封顶到存储总量。 */
	public static void regenerate(ServerPlayer player, Identifier skill, double amount) {
		Pool pool = pool(player, skill);
		pool.current = Math.min(max(player, skill), pool.current + amount);
		sync(player, skill, pool);
	}

	/** 把某个玩家所有技能的当前值推给客户端（进服时调一次，HUD 才有初始值）。 */
	public static void syncAll(ServerPlayer player) {
		for (Identifier skill : CONFIGS.keySet()) {
			sync(player, skill, pool(player, skill));
		}
	}

	private static void sync(ServerPlayer player, Identifier skill, Pool pool) {
		ServerPlayNetworking.send(player,
				new SkillSyncPayload(skill, pool.current, max(player, skill)));
	}

	private static Pool pool(ServerPlayer player, Identifier skill) {
		return POOLS
				.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>())
				.computeIfAbsent(skill, id -> new Pool(initial(id)));
	}

	/** 某个技能的开局值；没注册过的技能从 0 开始。 */
	private static double initial(Identifier skill) {
		Config config = CONFIGS.get(skill);
		return config == null ? 0.0D : config.initial();
	}

	private static void tick(MinecraftServer server) {
		if (CONFIGS.isEmpty() || POOLS.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Map<Identifier, Pool> pools = POOLS.get(player.getUUID());

			if (pools == null) {
				continue;
			}

			for (Map.Entry<Identifier, Pool> entry : pools.entrySet()) {
				Config config = CONFIGS.get(entry.getKey());

				if (config == null) {
					continue;
				}

				Pool pool = entry.getValue();
				double before = pool.current;
				pool.tick(config, max(player, entry.getKey()));

				// 自动回复改了数值也要推给客户端，否则 HUD 会停在扣掉之后的值
				if (pool.current != before) {
					sync(player, entry.getKey(), pool);
				}
			}
		}
	}

	/** 一个玩家在一个技能上的当前值。 */
	private static final class Pool {
		private double current;
		private int timer;

		Pool(double initial) {
			// 默认按满仓算，这样新玩家一进来就有完整的次数（剑意那种从 0 攒的另说）
			this.current = initial;
		}

		void tick(Config config, double max) {
			// 不自动回复的技能（回复间隔 <= 0）跳过：它的回复由调用方自己触发
			if (config.regenIntervalTicks() <= 0 || config.regenAmount() <= 0.0D) {
				return;
			}

			if (this.current >= max) {
				// 满仓：取消回复，计时归零
				this.timer = 0;
				return;
			}

			if (++this.timer >= config.regenIntervalTicks()) {
				this.timer = 0;
				this.current = Math.min(max, this.current + config.regenAmount());
			}
		}
	}
}
