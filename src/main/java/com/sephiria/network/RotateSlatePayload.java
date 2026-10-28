package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：按 R 旋转背包格区里的某块石板（只带菜单槽位号）。 */
public record RotateSlatePayload(int slot) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "rotate_slate");

	public static void write(FriendlyByteBuf buffer, RotateSlatePayload payload) {
		buffer.writeVarInt(payload.slot() + 1);
	}

	public static RotateSlatePayload read(FriendlyByteBuf buffer) {
		return new RotateSlatePayload(buffer.readVarInt() - 1);
	}
}
