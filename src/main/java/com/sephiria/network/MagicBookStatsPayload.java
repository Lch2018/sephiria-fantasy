package com.sephiria.network;

import com.sephiria.Sephiria;
import com.sephiria.artifact.MagicBook;
import com.sephiria.artifact.skill.ArtifactSkills;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：所有生效魔法书的**实际**冷却与蓝耗。
 *
 * <p>书类神器的提示框在背包 / 创造栏 / 商店等界面都要显示结算后的数值，客户端手里没有
 * 完整的赛菲莉娅背包，所以由服务端随属性同步一起把这份快照推过来。按「物品 + 等级 + 背包格」
 * 匹配，这样同一本书放在不同格子、吃到不同沙漏 / 碎片时也能区分。
 */
public record MagicBookStatsPayload(List<Entry> books) implements CustomPacketPayload {
	public record Entry(Identifier item, int level, int slot, int cooldownTicks, double mpCost) {
	}

	public static final CustomPacketPayload.Type<MagicBookStatsPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "magic_book_stats"));

	public static final StreamCodec<FriendlyByteBuf, MagicBookStatsPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, MagicBookStatsPayload payload) -> {
				buffer.writeVarInt(payload.books().size());

				for (Entry entry : payload.books()) {
					Identifier.STREAM_CODEC.encode(buffer, entry.item());
					buffer.writeVarInt(entry.level());
					buffer.writeVarInt(entry.slot());
					buffer.writeVarInt(entry.cooldownTicks());
					buffer.writeDouble(entry.mpCost());
				}
			},
			(FriendlyByteBuf buffer) -> {
				int count = buffer.readVarInt();
				List<Entry> books = new ArrayList<>(count);

				for (int index = 0; index < count; index++) {
					books.add(new Entry(Identifier.STREAM_CODEC.decode(buffer), buffer.readVarInt(),
							buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble()));
				}

				return new MagicBookStatsPayload(books);
			});

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

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
