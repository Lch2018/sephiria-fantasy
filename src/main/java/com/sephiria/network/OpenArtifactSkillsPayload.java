package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：打开技能页前请求一次同步。 */
public final class OpenArtifactSkillsPayload {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "open_artifact_skills");

	private OpenArtifactSkillsPayload() {
	}
}
