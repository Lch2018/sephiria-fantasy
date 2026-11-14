package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：玩家用左键挥了一次武器（空包）。 */
public record FrostQiPayload() {
	public static final FrostQiPayload INSTANCE = new FrostQiPayload();
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "frost_qi");
}
