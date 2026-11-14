package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：书「效率」给当前玩家的挖掘速度倍率。 */
public record BookEffectPayload(float miningSpeedFactor) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "book_effect");

	public static void write(FriendlyByteBuf buffer, BookEffectPayload payload) {
		buffer.writeFloat(payload.miningSpeedFactor());
	}

	public static BookEffectPayload read(FriendlyByteBuf buffer) {
		return new BookEffectPayload(buffer.readFloat());
	}
}
