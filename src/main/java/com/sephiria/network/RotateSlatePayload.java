package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：把赛菲利亚背包里某个格子上的石板旋转 90°（默认逆时针）。
 *
 * <p>只带格子编号，服务端自己查那块石板的朝向并接上去（客户端说的不算）。
 */
public record RotateSlatePayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RotateSlatePayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "rotate_slate"));

	public static final StreamCodec<FriendlyByteBuf, RotateSlatePayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, RotateSlatePayload payload) -> buffer.writeVarInt(payload.slot()),
			(FriendlyByteBuf buffer) -> new RotateSlatePayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
