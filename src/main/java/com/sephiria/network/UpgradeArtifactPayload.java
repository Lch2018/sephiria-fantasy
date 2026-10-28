package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：想升几级；实际升多少由服务端按剩余等级与附魔币重算。 */
public record UpgradeArtifactPayload(int levels) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "upgrade_artifact");

	public static void write(FriendlyByteBuf buffer, UpgradeArtifactPayload payload) {
		buffer.writeVarInt(payload.levels());
	}

	public static UpgradeArtifactPayload read(FriendlyByteBuf buffer) {
		return new UpgradeArtifactPayload(buffer.readVarInt());
	}
}
