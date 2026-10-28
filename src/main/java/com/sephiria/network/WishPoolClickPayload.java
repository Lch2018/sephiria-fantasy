package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：许愿池界面的抽卡 / 溢出开关。 */
public record WishPoolClickPayload(int action, boolean slatePool, int itemId) {
	public static final int ACTION_DRAW_SINGLE = 0;
	public static final int ACTION_DRAW_TEN = 1;
	public static final int ACTION_TOGGLE_OVERFLOW = 4;

	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "wish_pool_click");

	public static WishPoolClickPayload draw(boolean slatePool, boolean ten) {
		return new WishPoolClickPayload(ten ? ACTION_DRAW_TEN : ACTION_DRAW_SINGLE, slatePool, 0);
	}

	public static WishPoolClickPayload toggleOverflow() {
		return new WishPoolClickPayload(ACTION_TOGGLE_OVERFLOW, false, 0);
	}

	public static void write(FriendlyByteBuf buffer, WishPoolClickPayload payload) {
		buffer.writeVarInt(payload.action());
		buffer.writeBoolean(payload.slatePool());
		buffer.writeVarInt(payload.itemId());
	}

	public static WishPoolClickPayload read(FriendlyByteBuf buffer) {
		return new WishPoolClickPayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
	}
}
