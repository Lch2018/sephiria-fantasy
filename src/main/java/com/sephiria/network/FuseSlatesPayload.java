package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：石板合成界面点「合成」。
 *
 * <p>包里不带任何物品数据：服务端自己看当前菜单的两个输入格，校验后再合并图案
 * （客户端只是提前算一份用于预览）。
 */
public record FuseSlatesPayload() implements CustomPacketPayload {
	public static final FuseSlatesPayload INSTANCE = new FuseSlatesPayload();

	public static final CustomPacketPayload.Type<FuseSlatesPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "fuse_slates"));

	public static final StreamCodec<ByteBuf, FuseSlatesPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
