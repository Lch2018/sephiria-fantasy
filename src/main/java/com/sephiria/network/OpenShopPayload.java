package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求打开商店页面。
 *
 * <p>和背包一样，商店是个容器菜单，必须由服务端 {@code openMenu} 打开——货架内容在服务端。
 */
public record OpenShopPayload() implements CustomPacketPayload {
	public static final OpenShopPayload INSTANCE = new OpenShopPayload();

	public static final CustomPacketPayload.Type<OpenShopPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "open_shop"));

	public static final StreamCodec<ByteBuf, OpenShopPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
