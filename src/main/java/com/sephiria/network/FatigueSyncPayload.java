package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：经验加加（EXP++）点数（HUD 显示）。 */
public record FatigueSyncPayload(int points) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "fatigue_sync");

	public static void write(FriendlyByteBuf buffer, FatigueSyncPayload payload) {
		buffer.writeVarInt(payload.points());
	}

	public static FatigueSyncPayload read(FriendlyByteBuf buffer) {
		return new FatigueSyncPayload(buffer.readVarInt());
	}
}
