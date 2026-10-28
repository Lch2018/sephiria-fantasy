package com.sephiria.network;

import net.minecraft.resources.ResourceLocation;

/** 占位：1.20.1 核心版暂不向客户端同步技能页。 */
public record ArtifactSkillsPayload(java.util.List<Entry> entries) {
	public record Entry(ResourceLocation item, int level) {
	}
}
