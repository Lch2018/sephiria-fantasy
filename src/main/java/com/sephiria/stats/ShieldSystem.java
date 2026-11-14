package com.sephiria.stats;

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
 * 玩家护盾：受到伤害时先扣护盾，扣完的剩余伤害才走护甲 / 防御力等减伤。
 *
 * <p>护盾按「层」记：每一层有自己的量、到期刻与可选来源键。这样 6 秒的技能盾不会把
 * 「破碎的镜子」那种一直留到打破的盾一起带走；同名来源（镜子类）用来源键覆盖，
 * 不会因为反复触发把盾叠成一堵墙。
 *
 * <p>扣盾在 {@code DamageReductionMixin} 的最后一步：此时攻击方倍率（暴击 / 黄金之手 /
 * 所有伤害放大）已经算完，而原版的护甲 / 防御力还没结算——正好对应「护盾先吃原始伤害」。
 * 护盾本身不吃任何减伤，扣多少点就是多少点。
 */
public final class ShieldSystem {
	/** 一层护盾。{@code expireTick < 0} 表示不会自然过期。 */
	private static final class Layer {
		private final String key;
		private double amount;
		private final long expireTick;

		private Layer(String key, double amount, long expireTick) {
			this.key = key;
			this.amount = amount;
			this.expireTick = expireTick;
		}
	}

	private static final Map<UUID, List<Layer>> LAYERS = new HashMap<>();

	private ShieldSystem() {
	}

	/** 由 {@code Sephiria#onInitialize()} 调用。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ShieldSystem::tick);
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(
				(entity, source) -> {
					if (entity instanceof ServerPlayer player) {
						clear(player);
					}
				});
	}

	/** 加一层护盾；{@code durationTicks <= 0} 表示不会自然过期。 */
	public static void grant(ServerPlayer player, double amount, int durationTicks) {
		grant(player, null, amount, durationTicks);
	}

	/**
	 * 加一层带来源键的护盾：同一个键的旧层会被替换（镜子类反复触发时刷新而不是叠加）。
	 *
	 * @param key           来源键；null 表示每次都是一层新的（信 / 技能这类可以叠）
	 * @param durationTicks 持续刻数；<= 0 表示永久（直到被打破）
	 */
	public static void grant(ServerPlayer player, String key, double amount, int durationTicks) {
		if (amount <= 0.0D) {
			return;
		}

		long now = player.level().getGameTime();
		long expireTick = durationTicks <= 0 ? -1L : now + durationTicks;
		List<Layer> layers = LAYERS.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>());
		prune(layers, now);

		if (key != null) {
			layers.removeIf(layer -> key.equals(layer.key));
		}

		layers.add(new Layer(key, amount, expireTick));
	}

	/** 当前总护盾（给 HUD 用，向上取整——有 0.5 点也要显示 1）。 */
	public static int hudAmount(ServerPlayer player) {
		return (int) Math.ceil(total(player));
	}

	/** 当前总护盾（点）。 */
	public static double total(ServerPlayer player) {
		List<Layer> layers = LAYERS.get(player.getUUID());

		if (layers == null) {
			return 0.0D;
		}

		prune(layers, player.level().getGameTime());

		if (layers.isEmpty()) {
			LAYERS.remove(player.getUUID());
			return 0.0D;
		}

		double total = 0.0D;

		for (Layer layer : layers) {
			total += layer.amount;
		}

		return total;
	}

	/**
	 * 用护盾吸收伤害，返回还需要玩家自己吃的剩余伤害。
	 *
	 * <p>按到期刻从近到远扣：临时盾先被消耗，永久盾留到最后。
	 */
	public static double absorb(ServerPlayer player, double damage) {
		if (damage <= 0.0D) {
			return damage;
		}

		List<Layer> layers = LAYERS.get(player.getUUID());

		if (layers == null) {
			return damage;
		}

		prune(layers, player.level().getGameTime());
		// 临时盾先消耗、永久盾留到最后：expireTick = -1 的层排到队尾
		layers.sort(java.util.Comparator.comparingLong(
				layer -> layer.expireTick < 0L ? Long.MAX_VALUE : layer.expireTick));

		double remaining = damage;
		Iterator<Layer> iterator = layers.iterator();

		while (iterator.hasNext() && remaining > 0.0D) {
			Layer layer = iterator.next();
			double taken = Math.min(layer.amount, remaining);
			layer.amount -= taken;
			remaining -= taken;

			if (layer.amount <= 1.0E-6D) {
				iterator.remove();
			}
		}

		if (layers.isEmpty()) {
			LAYERS.remove(player.getUUID());
		}

		return remaining;
	}

	/** 退出 / 换世界时清空。 */
	public static void clear(ServerPlayer player) {
		LAYERS.remove(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		if (LAYERS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			List<Layer> layers = LAYERS.get(player.getUUID());

			if (layers == null) {
				continue;
			}

			prune(layers, now);

			if (layers.isEmpty()) {
				LAYERS.remove(player.getUUID());
			}
		}
	}

	private static void prune(List<Layer> layers, long now) {
		layers.removeIf(layer -> layer.expireTick >= 0L && now >= layer.expireTick);
	}
}
