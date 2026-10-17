package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：在赛菲莉娅背包界面里滚了一格滚轮。
 *
 * <p>只带方向（-1 往上、+1 往下），能不能滚、滚到第几行都由服务端按背包真实行数裁——
 * 视窗的偏移在服务端，改完由服务端把这一屏的格子重新推给客户端（见
 * {@code ArtifactBackpackMenu#scrollBackpack}）。
 */
public record ScrollBackpackPayload(int delta) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ScrollBackpackPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "scroll_backpack"));

	public static final StreamCodec<FriendlyByteBuf, ScrollBackpackPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, ScrollBackpackPayload payload) -> buffer.writeVarInt(payload.delta()),
			(FriendlyByteBuf buffer) -> new ScrollBackpackPayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
