package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：按下第几号技能栏（神器技能）。
 *
 * <p>只带栏位编号：栏位里放的是哪件神器、等级多少、冷却还剩多久、蓝够不够，全由服务端自己查，
 * 客户端说的不算。
 */
public record CastArtifactSkillPayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CastArtifactSkillPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "cast_artifact_skill"));

	public static final StreamCodec<FriendlyByteBuf, CastArtifactSkillPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, CastArtifactSkillPayload payload) -> buffer.writeVarInt(payload.slot()),
			(FriendlyByteBuf buffer) -> new CastArtifactSkillPayload(buffer.readVarInt()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
