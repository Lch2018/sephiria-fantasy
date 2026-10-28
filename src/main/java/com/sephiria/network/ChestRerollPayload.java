package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：点宝箱界面的「刷新」。 */
public final class ChestRerollPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "chest_reroll");

	private ChestRerollPayload() {
	}
}
