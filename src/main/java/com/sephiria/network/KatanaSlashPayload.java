package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：刀在入鞘、剑意满层时按了左键，请求释放「强力斩击」（空包）。
 *
 * <p>为什么需要单独发包：原版攻击只有<b>打到实体</b>才会走 {@code AttackEntityCallback}，
 * 而用户 2026-11-15 要求强力斩击像匕首的狂怒一样可以空放（朝准星方向突进），
 * 所以由客户端抢下这次点击，服务端再自己验状态、层数与冷却。
 */
public record KatanaSlashPayload() {
	public static final KatanaSlashPayload INSTANCE = new KatanaSlashPayload();

	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "katana_slash");
}
