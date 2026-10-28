package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：点商店的「刷新」。 */
public final class RefreshShopPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "refresh_shop");

	private RefreshShopPayload() {
	}
}
