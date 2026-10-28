package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：技能 HUD 与神器 HUD 的状态（1.20.1 旧 channel 版）。
 *
 * <p>1.20.1 没有 CustomPacketPayload / StreamCodec，所以这里用固定的 channel id
 * 加一对 write/read；字段含义与 1.21.1 的 payload 完全相同。
 */
public record HudSyncPayload(List<Entry> skills, List<Entry> artifacts) {
	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "hud_sync");

	public record Entry(ResourceLocation item, int remaining, int total, int charge) {
		public boolean charging() {
			return this.charge >= 0;
		}
	}

	public static void write(FriendlyByteBuf buffer, HudSyncPayload payload) {
		writeEntries(buffer, payload.skills());
		writeEntries(buffer, payload.artifacts());
	}

	public static HudSyncPayload read(FriendlyByteBuf buffer) {
		return new HudSyncPayload(readEntries(buffer), readEntries(buffer));
	}

	private static void writeEntries(FriendlyByteBuf buffer, List<Entry> entries) {
		buffer.writeVarInt(entries.size());

		for (Entry entry : entries) {
			buffer.writeResourceLocation(entry.item());
			buffer.writeVarInt(entry.remaining() + 1);
			buffer.writeVarInt(entry.total() + 1);
			buffer.writeVarInt(entry.charge() + 1);
		}
	}

	private static List<Entry> readEntries(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<Entry> entries = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			entries.add(new Entry(buffer.readResourceLocation(),
					buffer.readVarInt() - 1, buffer.readVarInt() - 1, buffer.readVarInt() - 1));
		}

		return entries;
	}
}
