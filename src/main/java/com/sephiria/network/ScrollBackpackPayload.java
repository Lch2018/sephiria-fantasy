package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：背包界面滚轮，参数是滚动的行数（正 = 向下）。 */
public record ScrollBackpackPayload(int deltaRows) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "scroll_backpack");

	public static void write(FriendlyByteBuf buffer, ScrollBackpackPayload payload) {
		buffer.writeVarInt(payload.deltaRows() + 8);
	}

	public static ScrollBackpackPayload read(FriendlyByteBuf buffer) {
		return new ScrollBackpackPayload(buffer.readVarInt() - 8);
	}
}
