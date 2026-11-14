package com.sephiria.network;

import com.sephiria.Sephiria;
import com.sephiria.artifact.MagicBook;
import com.sephiria.artifact.skill.ArtifactSkills;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** 服务端 → 客户端：所有生效魔法书的实际冷却与蓝耗。 */
public record MagicBookStatsPayload(List<Entry> books) {
	public record Entry(ResourceLocation item, int level, int slot, int cooldownTicks, double mpCost) {
	}

	public static final ResourceLocation CHANNEL =
			new ResourceLocation(Sephiria.MOD_ID, "magic_book_stats");

	public static void write(FriendlyByteBuf buffer, MagicBookStatsPayload payload) {
		buffer.writeVarInt(payload.books().size());

		for (Entry entry : payload.books()) {
			buffer.writeResourceLocation(entry.item());
			buffer.writeVarInt(entry.level());
			buffer.writeVarInt(entry.slot());
			buffer.writeVarInt(entry.cooldownTicks());
			buffer.writeDouble(entry.mpCost());
		}
	}

	public static MagicBookStatsPayload read(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<Entry> books = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			books.add(new Entry(buffer.readResourceLocation(), buffer.readVarInt(),
					buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble()));
		}

		return new MagicBookStatsPayload(List.copyOf(books));
	}

	/** 服务端现算：只包含生效、带技能的魔法书。 */
	public static MagicBookStatsPayload of(ServerPlayer player) {
		List<Entry> books = new ArrayList<>();

		for (ArtifactSkills.Available available : ArtifactSkills.available(player)) {
			if (!(available.item() instanceof MagicBook)) {
				continue;
			}

			books.add(new Entry(BuiltInRegistries.ITEM.getKey(available.item()), available.level(),
					available.slot(), ArtifactSkills.cooldownTicksFor(player, available),
					ArtifactSkills.mpCostFor(player, available)));
		}

		return new MagicBookStatsPayload(List.copyOf(books));
	}
}
