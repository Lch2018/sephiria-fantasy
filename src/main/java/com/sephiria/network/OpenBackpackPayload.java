package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求打开赛菲利亚背包的界面。
 *
 * <p>背包界面是个容器菜单（要能拖动物品），而容器菜单必须由服务端打开（{@code openMenu}），
 * 所以点界面上那个标签页时先发这个包。
 */
public record OpenBackpackPayload() implements CustomPacketPayload {
	public static final OpenBackpackPayload INSTANCE = new OpenBackpackPayload();

	public static final CustomPacketPayload.Type<OpenBackpackPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "open_backpack"));

	public static final StreamCodec<ByteBuf, OpenBackpackPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
