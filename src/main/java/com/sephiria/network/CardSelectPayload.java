package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：选择第 {@code index} 张卡。
 *
 * <p>同样只带下标；这张卡到底是什么、玩家还有没有选卡机会，全由服务端那份会话判。
 */
public record CardSelectPayload(int index) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CardSelectPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_select"));

	public static final StreamCodec<FriendlyByteBuf, CardSelectPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, CardSelectPayload payload) -> buffer.writeVarInt(payload.index()),
			(FriendlyByteBuf buffer) -> new CardSelectPayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
