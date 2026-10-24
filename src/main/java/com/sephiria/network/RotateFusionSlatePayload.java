package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：把石板合成界面的第 {@code input} 个输入格里的石板逆时针转 90°。
 *
 * <p>与服务端只认格子下标一样，客户端说的「哪一格」仅限 0 或 1，服务端会再校验。
 */
public record RotateFusionSlatePayload(int input) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RotateFusionSlatePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID,
					"rotate_fusion_slate"));

	public static final StreamCodec<ByteBuf, RotateFusionSlatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RotateFusionSlatePayload::input,
			RotateFusionSlatePayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
