package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：在商店页面点了一次「刷新」。
 *
 * <p>不带数据：骰子在骰子栏里，服务端自己吃掉一颗再重刷货架。
 */
public record RefreshShopPayload() implements CustomPacketPayload {
	public static final RefreshShopPayload INSTANCE = new RefreshShopPayload();

	public static final CustomPacketPayload.Type<RefreshShopPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "refresh_shop"));

	public static final StreamCodec<ByteBuf, RefreshShopPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
