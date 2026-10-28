package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：在石板合成界面点「合成」。 */
public final class FuseSlatesPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "fuse_slates");

	private FuseSlatesPayload() {
	}
}
