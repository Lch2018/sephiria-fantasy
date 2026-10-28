package com.sephiria.network;

import com.sephiria.Sephiria;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 服务端 → 客户端：技能页的可用技能与 6 个技能栏。 */
public record ArtifactSkillsPayload(List<Entry> skills, List<String> slots) {
	public record Entry(ResourceLocation item, int level) {
	}

	public static final ResourceLocation CHANNEL = new ResourceLocation(Sephiria.MOD_ID, "artifact_skills");

	public static void write(FriendlyByteBuf buffer, ArtifactSkillsPayload payload) {
		buffer.writeVarInt(payload.skills().size());

		for (Entry entry : payload.skills()) {
			buffer.writeResourceLocation(entry.item());
			buffer.writeVarInt(entry.level());
		}

		buffer.writeVarInt(payload.slots().size());

		for (String slot : payload.slots()) {
			buffer.writeUtf(slot);
		}
	}

	public static ArtifactSkillsPayload read(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<Entry> skills = new ArrayList<>(count);

		for (int index = 0; index < count; index++) {
			skills.add(new Entry(buffer.readResourceLocation(), buffer.readVarInt()));
		}

		int slotCount = buffer.readVarInt();
		List<String> slots = new ArrayList<>(slotCount);

		for (int index = 0; index < slotCount; index++) {
			slots.add(buffer.readUtf());
		}

		return new ArtifactSkillsPayload(skills, slots);
	}
}
