package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：根之退界面的调整 / 清空 / 应用。 */
public record RootRetreatClickPayload(int action, int option, int delta) {
	public static final int ACTION_ADJUST = 0;
	public static final int ACTION_CLEAR = 1;
	public static final int ACTION_APPLY = 2;

	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "root_retreat_click");

	public static RootRetreatClickPayload adjust(int option, int delta) {
		return new RootRetreatClickPayload(ACTION_ADJUST, option, delta);
	}

	public static RootRetreatClickPayload clear() {
		return new RootRetreatClickPayload(ACTION_CLEAR, 0, 0);
	}

	public static RootRetreatClickPayload apply() {
		return new RootRetreatClickPayload(ACTION_APPLY, 0, 0);
	}

	public static void write(FriendlyByteBuf buffer, RootRetreatClickPayload payload) {
		buffer.writeVarInt(payload.action());
		buffer.writeVarInt(payload.option());
		buffer.writeVarInt(payload.delta() + 2);
	}

	public static RootRetreatClickPayload read(FriendlyByteBuf buffer) {
		return new RootRetreatClickPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt() - 2);
	}
}
