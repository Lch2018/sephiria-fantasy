package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：玩家按下了冲刺键。
 *
 * <p>包里不带内容，服务端收到后自己查是哪个玩家发来的——位移和无敌都必须由
 * 服务端执行（客户端只说"我按了"）。
 */
public record DashPayload() implements CustomPacketPayload {
	public static final DashPayload INSTANCE = new DashPayload();

	public static final CustomPacketPayload.Type<DashPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "dash"));

	/** 空包：不写任何字节。 */
	public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
