package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：打开商店页面。 */
public final class OpenShopPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "open_shop");

	private OpenShopPayload() {
	}
}
