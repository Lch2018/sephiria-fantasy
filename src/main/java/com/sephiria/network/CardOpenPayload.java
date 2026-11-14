package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：请求打开选卡界面（空包）。 */
public record CardOpenPayload() {
	public static final CardOpenPayload INSTANCE = new CardOpenPayload();
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "card_open");
}
