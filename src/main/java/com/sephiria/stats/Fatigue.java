package com.sephiria.stats;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 经验加加（EXP++，内部标识仍叫 fatigue）：每 tick +1 点，上限 6000；
 * 获得经验时消耗当前点数的 40%，消耗量每满 30 点给 3 点额外经验。
 *
 * <p>点数只随时间恢复；40% 的消耗发生在获得经验结算那一下（见 {@link #claimBonus}）。
 */
public final class Fatigue {
	public static final int MAX_POINTS = 6000;
	public static final int REGEN_PER_TICK = 1;
	/** 每 30 点消耗量提供一组额外经验。 */
	public static final int POINTS_PER_EXPERIENCE = 30;
	/** 每满一组给 3 点额外经验。 */
	public static final int EXPERIENCE_PER_GROUP = 3;
	/** 每次获得经验消耗当前点数的百分比。 */
	public static final int CONSUME_PERCENT = 40;

	private static final AttachmentType<Integer> POINTS = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "fatigue_points"));

	private static int syncTicker;

	private Fatigue() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Fatigue::tick);
	}

	public static int points(ServerPlayer player) {
		Integer value = player.getAttached(POINTS);
		return value == null ? 0 : Math.max(0, Math.min(MAX_POINTS, value));
	}

	/**
	 * 获得经验时结算经验加加：消耗当前点数的 40%，消耗量每满 30 点给 3 点额外经验。
	 * 不满 30 点时仍会消耗这 40%，但额外经验为 0。
	 */
	public static int claimBonus(ServerPlayer player) {
		int points = points(player);

		if (points <= 0) {
			return 0;
		}

		int consumed = Math.round(points * (CONSUME_PERCENT / 100.0F));
		int bonus = (consumed / POINTS_PER_EXPERIENCE) * EXPERIENCE_PER_GROUP;

		if (consumed > 0) {
			player.setAttached(POINTS, points - consumed);
		}

		return bonus;
	}

	private static void tick(MinecraftServer server) {
		boolean sync = ++syncTicker % 20 == 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int points = points(player);

			if (points < MAX_POINTS) {
				points = Math.min(MAX_POINTS, points + REGEN_PER_TICK);
				player.setAttached(POINTS, points);
			}

			if (sync) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
						new com.sephiria.network.FatigueSyncPayload(points));
			}
		}
	}
}
