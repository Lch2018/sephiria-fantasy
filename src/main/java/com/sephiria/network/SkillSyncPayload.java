package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：某个技能的存储量变化（或首次同步）。
 *
 * <p>HUD 要显示「现有 / 总量」，而存储本身只存在服务端，所以每次变化都推一份给拥有者。
 * 技能 id 用 {@link ResourceLocation} 传，将来加技能不用再动协议。
 *
 * <p>1.20.1 没有 CustomPacketPayload / StreamCodec，沿用 {@link HudSyncPayload} 的旧 channel
 * 写法：固定 channel id + 一对 write/read，字段含义与 1.21.1 完全相同。
 */
public record SkillSyncPayload(ResourceLocation skill, double current, double max) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "skill_sync");

	public static void write(FriendlyByteBuf buffer, SkillSyncPayload payload) {
		buffer.writeResourceLocation(payload.skill());
		buffer.writeDouble(payload.current());
		buffer.writeDouble(payload.max());
	}

	public static SkillSyncPayload read(FriendlyByteBuf buffer) {
		return new SkillSyncPayload(buffer.readResourceLocation(), buffer.readDouble(), buffer.readDouble());
	}
}
