package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：附魔面板里点了一次「升级 N 级」。
 *
 * <p>只带「想升几级」，实际升多少、扣几枚附魔币由服务端按神器剩余等级与附魔币栏的数量重算。
 */
public record UpgradeArtifactPayload(int levels) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UpgradeArtifactPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "upgrade_artifact"));

	public static final StreamCodec<FriendlyByteBuf, UpgradeArtifactPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, UpgradeArtifactPayload payload) -> buffer.writeVarInt(payload.levels()),
			(FriendlyByteBuf buffer) -> new UpgradeArtifactPayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
