package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求打开选卡界面。
 *
 * <p>和商店 / 背包不同，选卡不是容器菜单：服务端掷好三张卡后用
 * {@link CardSelectionPayload} 推回来，客户端收到才打开界面。
 * 所以这个包只是「我想看」的意图，发不发卡、能选哪张全由服务端决定。
 */
public record CardOpenPayload() implements CustomPacketPayload {
	public static final CardOpenPayload INSTANCE = new CardOpenPayload();

	public static final CustomPacketPayload.Type<CardOpenPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "card_open"));

	public static final StreamCodec<ByteBuf, CardOpenPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
