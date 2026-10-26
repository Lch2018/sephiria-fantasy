package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：技能 HUD 与神器 HUD 的状态。
 *
 * <p>技能 HUD 是 6 个技能栏里的图标与冷却；神器 HUD 是「有冷却 / 充能」的神器图标。
 * 冷却用 {@code remaining/total} 两段表示，客户端自己按刻数倒推进度；充能用 {@code charge}
 * （0..1000 的千分比，-1 表示这一条不是充能）。
 *
 * <p>每 5 刻重推一次：冷却在服务端是 gameTime 表，客户端没有同步源；5 刻一包很小，
 * 客户端在两包之间自己按客户端刻数平滑。
 */
public record HudSyncPayload(List<Entry> skills, List<Entry> artifacts) implements CustomPacketPayload {
	/**
	 * 一条 HUD 图标。
	 *
	 * @param item      图标物品
	 * @param remaining 冷却剩余 tick（充能条目为 -1）
	 * @param total     冷却总 tick（充能条目为 -1）
	 * @param charge    充能千分比 0..1000（冷却条目为 -1）
	 */
	public record Entry(Identifier item, int remaining, int total, int charge) {
		public boolean charging() {
			return this.charge >= 0;
		}
	}

	public static final CustomPacketPayload.Type<HudSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "hud_sync"));

	public static final StreamCodec<FriendlyByteBuf, HudSyncPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, HudSyncPayload payload) -> {
				writeEntries(buffer, payload.skills());
				writeEntries(buffer, payload.artifacts());
			},
			(FriendlyByteBuf buffer) -> new HudSyncPayload(readEntries(buffer), readEntries(buffer)));

	private static void writeEntries(FriendlyByteBuf buffer, List<Entry> entries) {
		buffer.writeVarInt(entries.size());

		for (Entry entry : entries) {
			Identifier.STREAM_CODEC.encode(buffer, entry.item());
			buffer.writeVarInt(entry.remaining() + 1);
			buffer.writeVarInt(entry.total() + 1);
			buffer.writeVarInt(entry.charge() + 1);
		}
	}

	private static List<Entry> readEntries(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<Entry> entries = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			entries.add(new Entry(Identifier.STREAM_CODEC.decode(buffer),
					buffer.readVarInt() - 1, buffer.readVarInt() - 1, buffer.readVarInt() - 1));
		}

		return entries;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
