package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：刷新第 {@code index} 张卡。
 *
 * <p>只带下标；新卡是什么、品质会不会升、这张卡还能不能刷，全部由服务端按自己那份会话算。
 */
public record CardRefreshPayload(int index) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CardRefreshPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_refresh"));

	public static final StreamCodec<FriendlyByteBuf, CardRefreshPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, CardRefreshPayload payload) -> buffer.writeVarInt(payload.index()),
			(FriendlyByteBuf buffer) -> new CardRefreshPayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
