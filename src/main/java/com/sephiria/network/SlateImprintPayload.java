package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：把某个背包格里的石板刻印掉（长按进度满）。 */
public record SlateImprintPayload(int slot) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "slate_imprint");

	public static void write(FriendlyByteBuf buffer, SlateImprintPayload payload) {
		buffer.writeVarInt(payload.slot());
	}

	public static SlateImprintPayload read(FriendlyByteBuf buffer) {
		return new SlateImprintPayload(buffer.readVarInt());
	}
}
