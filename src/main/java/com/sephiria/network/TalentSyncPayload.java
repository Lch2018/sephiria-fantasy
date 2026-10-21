package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：天赋点与四条天赋页的加点数。
 *
 * <p>天赋界面上的所有数字都读这一份（客户端不自己算）：剩余点数 = 已获得 − 四条天赋页之和，
 * 每次加点 / 退点 / 重置 / 获得天赋点后服务端立刻重推一次。
 */
public record TalentSyncPayload(int earned, int rage, int swift, int will, int quickWit,
		int survival, int patience, int wisdom)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TalentSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "talent_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TalentSyncPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TalentSyncPayload::earned,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::rage,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::swift,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::will,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::quickWit,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::survival,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::patience,
			ByteBufCodecs.VAR_INT, TalentSyncPayload::wisdom,
			TalentSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
