package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：天赋点与七条天赋的加点数。 */
public record TalentSyncPayload(int earned, int rage, int swift, int will, int quickWit,
		int survival, int patience, int wisdom, boolean nearSpawn) {
	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "talent_sync");

	public static void write(FriendlyByteBuf buffer, TalentSyncPayload payload) {
		buffer.writeVarInt(payload.earned());
		buffer.writeVarInt(payload.rage());
		buffer.writeVarInt(payload.swift());
		buffer.writeVarInt(payload.will());
		buffer.writeVarInt(payload.quickWit());
		buffer.writeVarInt(payload.survival());
		buffer.writeVarInt(payload.patience());
		buffer.writeVarInt(payload.wisdom());
		buffer.writeBoolean(payload.nearSpawn());
	}

	public static TalentSyncPayload read(FriendlyByteBuf buffer) {
		return new TalentSyncPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
				buffer.readVarInt(), buffer.readBoolean());
	}
}
