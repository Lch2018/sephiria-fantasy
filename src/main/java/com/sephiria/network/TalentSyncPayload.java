package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：天赋点与四条天赋页的加点数。
 *
 * <p>天赋界面上的所有数字都读这一份（客户端不自己算）：剩余点数 = 已获得 − 四条天赋页之和，
 * 每次加点 / 退点 / 重置 / 获得天赋点后服务端立刻重推一次。
 *
 * <p>{@code nearSpawn} 也一起推：客户端的出生点只有世界出生点，玩家自己用床 / 重生锚 /
 * {@code /spawnpoint} 设的那一份在服务端，所以「能不能加点」由服务端算好告诉客户端。
 */
public record TalentSyncPayload(int earned, int rage, int swift, int will, int quickWit,
		int survival, int patience, int wisdom, boolean nearSpawn)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TalentSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "talent_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TalentSyncPayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> {
				buf.writeVarInt(payload.earned());
				buf.writeVarInt(payload.rage());
				buf.writeVarInt(payload.swift());
				buf.writeVarInt(payload.will());
				buf.writeVarInt(payload.quickWit());
				buf.writeVarInt(payload.survival());
				buf.writeVarInt(payload.patience());
				buf.writeVarInt(payload.wisdom());
				buf.writeBoolean(payload.nearSpawn());
			}, buf -> new TalentSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readVarInt(), buf.readBoolean()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
