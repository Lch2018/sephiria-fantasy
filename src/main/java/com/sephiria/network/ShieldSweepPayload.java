package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：剑盾防御中按了左键，请求释放横扫（空包）。 */
public record ShieldSweepPayload() {
	public static final ShieldSweepPayload INSTANCE = new ShieldSweepPayload();
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "shield_sweep");
}
