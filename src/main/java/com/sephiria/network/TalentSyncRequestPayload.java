package com.sephiria.network;

import com.sephiria.Sephiria;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求重推一次天赋同步包。
 *
 * <p>天赋页的按钮是否可点取决于「玩家是否在出生点附近」，而客户端的出生点信息只有世界出生点、
 * 没有玩家自己设置的床 / 重生锚。所以打开界面时向服务端要一次最新状态（{@link TalentSyncPayload}
 * 里的 {@code nearSpawn}），服务端用 {@code Talents.nearSpawn} 现算后再推回来。
 */
public record TalentSyncRequestPayload() implements CustomPacketPayload {
	public static final TalentSyncRequestPayload INSTANCE = new TalentSyncRequestPayload();

	public static final CustomPacketPayload.Type<TalentSyncRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "talent_sync_request"));

	public static final StreamCodec<ByteBuf, TalentSyncRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
