package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：天赋页的加减 / 重置。 */
public record TalentClickPayload(int page, int delta, boolean reset) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "talent_click");

	public static TalentClickPayload adjust(int page, int delta) {
		return new TalentClickPayload(page, delta, false);
	}

	public static TalentClickPayload resetAll() {
		return new TalentClickPayload(0, 0, true);
	}

	public static void write(FriendlyByteBuf buffer, TalentClickPayload payload) {
		buffer.writeVarInt(payload.page());
		buffer.writeVarInt(payload.delta() + 8);
		buffer.writeBoolean(payload.reset());
	}

	public static TalentClickPayload read(FriendlyByteBuf buffer) {
		return new TalentClickPayload(buffer.readVarInt(), buffer.readVarInt() - 8, buffer.readBoolean());
	}
}
