package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：当前持有的石板物品数量（黑鳞片的提示框用）。 */
public record SlateCountPayload(int count) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "slate_count");

	public static void write(FriendlyByteBuf buffer, SlateCountPayload payload) {
		buffer.writeVarInt(payload.count());
	}

	public static SlateCountPayload read(FriendlyByteBuf buffer) {
		return new SlateCountPayload(buffer.readVarInt());
	}
}
