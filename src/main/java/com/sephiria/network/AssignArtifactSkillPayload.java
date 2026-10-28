package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：把某个技能放进第 slot 个技能栏（entry 为空 = 清空）。 */
public record AssignArtifactSkillPayload(int slot, String entry) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "assign_artifact_skill");

	public static void write(FriendlyByteBuf buffer, AssignArtifactSkillPayload payload) {
		buffer.writeVarInt(payload.slot());
		buffer.writeUtf(payload.entry());
	}

	public static AssignArtifactSkillPayload read(FriendlyByteBuf buffer) {
		return new AssignArtifactSkillPayload(buffer.readVarInt(), buffer.readUtf());
	}
}
