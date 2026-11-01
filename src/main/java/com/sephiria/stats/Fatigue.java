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
 * 疲劳：每 tick +1 点，6000 tick 回满；获得经验时把当前点数按 0.08 折算成额外经验。
 *
 * <p>点数只随时间恢复，不在获得经验时消耗（用户口径：「当获取经验时会额外获取当前点数的经验值」）。
 */
public final class Fatigue {
	public static final int MAX_POINTS = 6000;
	public static final int REGEN_PER_TICK = 1;
	/** 每 12 点疲劳提供 1 点额外经验；不满 12 点不给。 */
	public static final int POINTS_PER_EXPERIENCE = 12;

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

	/** 当前疲劳折算出的额外经验：每满 12 点给 1 点。 */
	public static int bonusExperience(ServerPlayer player) {
		return points(player) / POINTS_PER_EXPERIENCE;
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
