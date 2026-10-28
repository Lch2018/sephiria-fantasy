package com.sephiria.network;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** 占位：1.20.1 核心版暂不向客户端同步 HUD。 */
public record HudSyncPayload(List<Entry> skills, List<Entry> artifacts) {
	public record Entry(ResourceLocation item, int remaining, int total, int charge) {
	}
}
