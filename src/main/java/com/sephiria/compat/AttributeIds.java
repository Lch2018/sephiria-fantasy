package com.sephiria.compat;

import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 1.20.1 的属性修饰符仍按 UUID 认身份（1.21+ 才改成 ResourceLocation）。
 * 这里用名字派生出稳定的 UUID，保证「按名字摘掉」在重启后仍能对上同一条修饰符。
 */
public final class AttributeIds {
	private AttributeIds() {
	}

	public static UUID of(String path) {
		return UUID.nameUUIDFromBytes(("sephiria:" + path).getBytes(StandardCharsets.UTF_8));
	}

	public static UUID of(ResourceLocation id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
	}
}
