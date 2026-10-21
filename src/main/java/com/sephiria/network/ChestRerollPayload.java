package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 客户端 → 服务端：宝箱界面点「刷新」。 */
public record ChestRerollPayload() implements CustomPacketPayload {
	public static final ChestRerollPayload INSTANCE = new ChestRerollPayload();

	public static final CustomPacketPayload.Type<ChestRerollPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "chest_reroll"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ChestRerollPayload> STREAM_CODEC =
			StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
