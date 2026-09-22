package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：神器技能页要显示的两样东西——「当前可用的技能」与「6 个技能栏里放了什么」。
 *
 * <p>技能是按背包内容算出来的（见 {@code ArtifactSkills.available}），而打开技能页时客户端手里
 * 并没有背包内容，所以由服务端算好推过来；每次改动（放入 / 清空 / 背包变化）都重推一次。
 *
 * <p>技能栏存的是「物品 id @ 等级」这样的字符串，技能页按它显示名字与图标；解析不出对应神器时
 * 显示成空的（神器卸下了、或等级因为升级对不上）。
 */
public record ArtifactSkillsPayload(List<Entry> skills, List<String> slots) implements CustomPacketPayload {
	/** 一条可用技能：神器物品 id + 实际等级（图标与名字都用这件神器）。 */
	public record Entry(Identifier item, int level) {
	}

	public static final CustomPacketPayload.Type<ArtifactSkillsPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_skills"));

	public static final StreamCodec<FriendlyByteBuf, ArtifactSkillsPayload> STREAM_CODEC = StreamCodec.of(
			(FriendlyByteBuf buffer, ArtifactSkillsPayload payload) -> {
				buffer.writeVarInt(payload.skills().size());

				for (Entry entry : payload.skills()) {
					Identifier.STREAM_CODEC.encode(buffer, entry.item());
					buffer.writeVarInt(entry.level());
				}

				buffer.writeVarInt(payload.slots().size());

				for (String slot : payload.slots()) {
					buffer.writeUtf(slot);
				}
			},
			(FriendlyByteBuf buffer) -> {
				int count = buffer.readVarInt();
				List<Entry> skills = new ArrayList<>(count);

				for (int index = 0; index < count; index++) {
					skills.add(new Entry(Identifier.STREAM_CODEC.decode(buffer), buffer.readVarInt()));
				}

				int slots = buffer.readVarInt();
				List<String> entries = new ArrayList<>(slots);

				for (int index = 0; index < slots; index++) {
					entries.add(buffer.readUtf());
				}

				return new ArtifactSkillsPayload(skills, entries);
			});

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
