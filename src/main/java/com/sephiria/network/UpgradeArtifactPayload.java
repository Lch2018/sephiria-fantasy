package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 客户端 → 服务端：附魔面板里点了一次「升级」。空包，服务端查自己开的界面。 */
public record UpgradeArtifactPayload() implements CustomPacketPayload {
	public static final UpgradeArtifactPayload INSTANCE = new UpgradeArtifactPayload();

	public static final CustomPacketPayload.Type<UpgradeArtifactPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "upgrade_artifact"));

	public static final StreamCodec<ByteBuf, UpgradeArtifactPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
