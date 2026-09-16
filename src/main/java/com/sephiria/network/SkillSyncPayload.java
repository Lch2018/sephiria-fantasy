package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：某个技能的存储量变化（或首次同步）。
 *
 * <p>HUD 要显示"现有 / 总量"，而存储本身只存在服务端，所以每次变化都推一份给拥有者。
 * 技能 id 用字符串传，将来加技能不用再动协议。
 */
public record SkillSyncPayload(Identifier skill, double current, double max) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SkillSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "skill_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SkillSyncPayload> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, SkillSyncPayload::skill,
			ByteBufCodecs.DOUBLE, SkillSyncPayload::current,
			ByteBufCodecs.DOUBLE, SkillSyncPayload::max,
			SkillSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
