package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：请求重推一次天赋同步包。
 *
 * <p>天赋页的按钮是否可点取决于「玩家是否在出生点附近」，而客户端的出生点信息只有世界出生点、
 * 没有玩家自己设置的床 / 重生锚。所以打开界面时向服务端要一次最新状态（{@link TalentSyncPayload}
 * 里的 {@code nearSpawn}），服务端用 {@code Talents.nearSpawn} 现算后再推回来。
 *
 * <p>包里没有字段：1.20.1 的网络包按 channel + 空 buf 发，收端直接读空 buf 即可。
 */
public final class TalentSyncRequestPayload {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "talent_sync_request");

	private TalentSyncRequestPayload() {
	}
}
