package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：在商店页面点了一次「出售」。
 *
 * <p>不带数据：要卖的东西就在出售栏里，服务端按栏里的东西自己算价钱。
 */
public record SellItemPayload() implements CustomPacketPayload {
	public static final SellItemPayload INSTANCE = new SellItemPayload();

	public static final CustomPacketPayload.Type<SellItemPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sell_item"));

	public static final StreamCodec<ByteBuf, SellItemPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
