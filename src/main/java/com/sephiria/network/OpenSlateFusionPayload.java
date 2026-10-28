package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：在商人界面点「石板合成」。 */
public final class OpenSlateFusionPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "open_slate_fusion");

	private OpenSlateFusionPayload() {
	}
}
