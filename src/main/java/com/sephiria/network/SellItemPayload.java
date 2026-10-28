package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：点商店的「出售」。 */
public final class SellItemPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "sell_item");

	private SellItemPayload() {
	}
}
