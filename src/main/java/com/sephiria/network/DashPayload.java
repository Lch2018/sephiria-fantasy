package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：按一下冲刺。旧 channel 版只用来触发，不带参数。 */
public final class DashPayload {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "dash");

	private DashPayload() {
	}
}
