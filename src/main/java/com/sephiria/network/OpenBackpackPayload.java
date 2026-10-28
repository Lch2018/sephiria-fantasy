package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：打开赛菲莉娅背包。 */
public final class OpenBackpackPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "open_backpack");

	private OpenBackpackPayload() {
	}
}
