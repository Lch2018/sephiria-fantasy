package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：按下第几号神器技能栏（0..5）。 */
public record CastArtifactSkillPayload(int slot) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "cast_artifact_skill");

	public static void write(FriendlyByteBuf buffer, CastArtifactSkillPayload payload) {
		buffer.writeVarInt(payload.slot());
	}

	public static CastArtifactSkillPayload read(FriendlyByteBuf buffer) {
		return new CastArtifactSkillPayload(buffer.readVarInt());
	}
}
