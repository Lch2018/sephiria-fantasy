package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：刷新第 {@code index} 张卡。 */
public record CardRefreshPayload(int index) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "card_refresh");

	public static void write(FriendlyByteBuf buffer, CardRefreshPayload payload) {
		buffer.writeVarInt(payload.index());
	}

	public static CardRefreshPayload read(FriendlyByteBuf buffer) {
		return new CardRefreshPayload(buffer.readVarInt());
	}
}
