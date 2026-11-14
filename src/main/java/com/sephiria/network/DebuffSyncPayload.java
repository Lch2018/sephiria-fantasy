package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：某个实体身上某个减益的层数变化（0 = 该减益消失）。
 *
 * <p>用途是视觉——减益层数要显示在敌人脚下的标签上，而层数只存在服务端的实体附件里。
 */
public record DebuffSyncPayload(ResourceLocation debuff, int entityId, int stacks) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "debuff_sync");

	public static void write(FriendlyByteBuf buffer, DebuffSyncPayload payload) {
		buffer.writeResourceLocation(payload.debuff());
		buffer.writeVarInt(payload.entityId());
		buffer.writeVarInt(payload.stacks());
	}

	public static DebuffSyncPayload read(FriendlyByteBuf buffer) {
		return new DebuffSyncPayload(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt());
	}
}
