package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：选择第 {@code index} 张卡。 */
public record CardSelectPayload(int index) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "card_select");

	public static void write(FriendlyByteBuf buffer, CardSelectPayload payload) {
		buffer.writeVarInt(payload.index());
	}

	public static CardSelectPayload read(FriendlyByteBuf buffer) {
		return new CardSelectPayload(buffer.readVarInt());
	}
}
