package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 服务端 → 客户端：疲劳点数（HUD 显示）。 */
public record FatigueSyncPayload(int points) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FatigueSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "fatigue_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FatigueSyncPayload> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, FatigueSyncPayload::points, FatigueSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
