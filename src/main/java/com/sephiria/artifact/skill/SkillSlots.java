package com.sephiria.artifact.skill;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器技能的 6 个技能栏：每格存「神器物品 id @ 等级」，空栏位是空串。
 *
 * <p>存字符串而不是物品引用：技能数值按神器的等级算，而等级会因为附魔币升级而变化——存下当时的
 * 等级，升级后那一格就对不上了（页面会显示成空的），玩家重新拖一次即可。这样存档格式简单，
 * 也不用担心神器被移除后留下悬空引用。
 *
 * <p>跟着玩家存档走（数据附件），重登/重启都不丢；死亡也保留（和属性一样用 copyOnDeath）。
 */
public final class SkillSlots {
	public static final int COUNT = 6;

	private static final AttachmentType<List<String>> SAVED = AttachmentRegistry.<List<String>>builder()
			.persistent(Codec.STRING.listOf())
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_skill_slots"));

	private SkillSlots() {
	}

	/** 由 {@code Sephiria#onInitialize()} 调用（静态字段本身就会注册，这里只是给个明确的初始化点）。 */
	public static void register() {
	}

	/** 6 个栏位的内容（可写列表；第一次访问时建一份空的）。 */
	public static List<String> of(ServerPlayer player) {
		List<String> slots = player.getAttached(SAVED);

		if (slots == null || slots.size() != COUNT) {
			slots = new ArrayList<>(List.of("", "", "", "", "", ""));
			player.setAttached(SAVED, slots);
			return slots;
		}

		// 从存档读回来的是编解码器给的**不可变**列表，直接 set() 会抛 UnsupportedOperationException
		// （重启前因为是我们自己 new 的 ArrayList 所以没事，重启后一动就炸——技能搬不动就是这么来的）。
		if (!(slots instanceof ArrayList)) {
			slots = new ArrayList<>(slots);
			player.setAttached(SAVED, slots);
		}

		return slots;
	}

	/** 改某一格（{@code entry} 为空串表示清空）。 */
	public static void set(ServerPlayer player, int slot, String entry) {
		List<String> slots = of(player);

		if (slot < 0 || slot >= COUNT) {
			return;
		}

		slots.set(slot, entry);
		player.setAttached(SAVED, slots);
	}
}
