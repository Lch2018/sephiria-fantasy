package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：打开神器技能页。
 *
 * <p>技能列表得由服务端算（客户端没有背包内容），所以打开时先发这个包，服务端回一个
 * {@link ArtifactSkillsPayload}。
 */
public record OpenArtifactSkillsPayload() implements CustomPacketPayload {
	public static final OpenArtifactSkillsPayload INSTANCE = new OpenArtifactSkillsPayload();

	public static final CustomPacketPayload.Type<OpenArtifactSkillsPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "open_artifact_skills"));

	public static final StreamCodec<FriendlyByteBuf, OpenArtifactSkillsPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, OpenArtifactSkillsPayload payload) -> {
			},
			(FriendlyByteBuf buffer) -> INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
