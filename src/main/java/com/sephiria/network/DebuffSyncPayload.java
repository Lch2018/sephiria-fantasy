package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：某个实体身上某个减益的层数变化（0 = 该减益消失）。
 *
 * <p>用途是<b>视觉</b>——减益的层数要显示在敌人脚下的标签上（「触电：x」），而层数只存在
 * 服务端的实体附件里，所以每次变化向附近的玩家广播一次。减益 id 用 Identifier 传，
 * 以后加新减益不用再动协议；层数 0 表示收掉标签。
 */
public record DebuffSyncPayload(Identifier debuff, int entityId, int stacks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DebuffSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "debuff_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DebuffSyncPayload> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, DebuffSyncPayload::debuff,
			ByteBufCodecs.VAR_INT, DebuffSyncPayload::entityId,
			ByteBufCodecs.VAR_INT, DebuffSyncPayload::stacks,
			DebuffSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
