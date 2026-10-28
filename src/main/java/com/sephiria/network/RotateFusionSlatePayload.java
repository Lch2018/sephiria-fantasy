package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：在石板合成界面按 R 旋转第 input 个输入格（0 / 1）。 */
public record RotateFusionSlatePayload(int input) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "rotate_fusion_slate");

	public static void write(FriendlyByteBuf buffer, RotateFusionSlatePayload payload) {
		buffer.writeVarInt(payload.input() + 1);
	}

	public static RotateFusionSlatePayload read(FriendlyByteBuf buffer) {
		return new RotateFusionSlatePayload(buffer.readVarInt() - 1);
	}
}
