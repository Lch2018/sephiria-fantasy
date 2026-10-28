package com.sephiria.network;

import net.minecraft.resources.ResourceLocation;

/** 1.20.1 核心移植暂时不做客户端同步；只为还留在核心代码里的数据结构提供轻量占位。 */
public final class Stubs {
	private Stubs() {
	}

	public record HudEntry(ResourceLocation item, int remaining, int total, int charge) {
	}

	public record SkillEntry(ResourceLocation item, int level) {
	}
}
