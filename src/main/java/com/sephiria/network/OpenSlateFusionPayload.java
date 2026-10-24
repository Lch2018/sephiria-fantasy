package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：在商人界面点「石板合成」，请求打开合成界面。
 *
 * <p>合成界面是个容器菜单，必须由服务端打开；服务端只在玩家确实开着商人界面时认这个包。
 */
public record OpenSlateFusionPayload() implements CustomPacketPayload {
	public static final OpenSlateFusionPayload INSTANCE = new OpenSlateFusionPayload();

	public static final CustomPacketPayload.Type<OpenSlateFusionPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "open_slate_fusion"));

	public static final StreamCodec<ByteBuf, OpenSlateFusionPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
