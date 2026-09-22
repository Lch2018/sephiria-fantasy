package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：把某个技能放进第几号技能栏（{@code entry} 为空串表示清空这一格）。
 *
 * <p>{@code entry} 是「物品 id @ 等级」的字符串，服务端会先确认背包里确实有对应的、生效的神器
 * 才会写进栏位——客户端塞一个不存在的东西过来只会被忽略。
 */
public record AssignArtifactSkillPayload(int slot, String entry) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AssignArtifactSkillPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "assign_artifact_skill"));

	public static final StreamCodec<FriendlyByteBuf, AssignArtifactSkillPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, AssignArtifactSkillPayload payload) -> {
				buffer.writeVarInt(payload.slot());
				buffer.writeUtf(payload.entry());
			},
			(FriendlyByteBuf buffer) -> new AssignArtifactSkillPayload(buffer.readVarInt(), buffer.readUtf()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
