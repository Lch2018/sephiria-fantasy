package com.sephiria.client.skill;

import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.artifact.skill.ArtifactSkill;
import com.sephiria.network.ArtifactSkillsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器技能页的客户端镜像：服务端推过来的「可用技能」与「6 个栏位」。
 *
 * <p>技能名字与图标都从神器物品上取（神器类在两边都有），所以包里只需要物品 id 与等级。
 */
public final class ClientSkills {
	private static List<ArtifactSkillsPayload.Entry> skills = List.of();
	private static List<String> slots = List.of("", "", "", "", "", "");

	private ClientSkills() {
	}

	public static void accept(ArtifactSkillsPayload payload) {
		skills = payload.skills();
		slots = payload.slots();
	}

	/** 当前可用的技能（背包里生效、带技能词条的神器各一条）。 */
	public static List<ArtifactSkillsPayload.Entry> skills() {
		return skills;
	}

	/** 6 个栏位里存的字符串（空串 = 空栏位）。 */
	public static List<String> slots() {
		return slots;
	}

	/** 一条可用技能 → 技能栏里存的字符串（物品 id @ 等级）。 */
	public static String encode(ArtifactSkillsPayload.Entry entry) {
		return entry.item() + "@" + entry.level();
	}

	/** 神器物品的图标贴图路径（神器的图标就是 textures/item/<id>.png）。 */
	public static Identifier iconOf(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png");
	}

	/** 技能栏字符串对应的神器（客户端按注册表反查；找不到返回 null）。 */
	public static Item itemOf(String entry) {
		int at = entry == null ? -1 : entry.lastIndexOf('@');

		if (at <= 0) {
			return null;
		}

		Identifier id = Identifier.tryParse(entry.substring(0, at));
		return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
	}

	/** 技能栏字符串里记的等级（解析失败返回 0）。 */
	public static int levelOf(String entry) {
		int at = entry == null ? -1 : entry.lastIndexOf('@');

		if (at <= 0) {
			return 0;
		}

		try {
			return Integer.parseInt(entry.substring(at + 1));
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	/** 这件神器给的技能（没有返回 null）。 */
	public static ArtifactSkill skillOf(Item item) {
		return item instanceof SephiriaArtifact artifact ? artifact.skill() : null;
	}

	/** 技能名：{@code sephiria.artifact_skill.<id>}。 */
	public static Component skillName(ArtifactSkill skill) {
		return Component.translatable("sephiria_fantasy.artifact_skill." + skill.id());
	}

	/** 神器名 + 等级，技能页里每条技能都带上它（两件同种神器要靠等级区分）。 */
	public static Component label(Item item, int level) {
		int maxLevel = item instanceof SephiriaArtifact artifact ? artifact.maxLevel() : level;
		return Component.translatable("screen.sephiria_fantasy.artifact_skills.entry",
				item.getName(item.getDefaultInstance()), Math.min(level, maxLevel));
	}

	/** 技能页里这条技能对应的服务端条目（物品 + 封顶等级；找不到返回 null）。 */
	private static ArtifactSkillsPayload.Entry find(Item item, int level) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);

		for (ArtifactSkillsPayload.Entry entry : skills) {
			if (entry.item().equals(id) && entry.level() == level) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * 技能页悬停时显示的技能说明：标题 + 技能描述 + 等级。
	 *
	 * <p>等级按神器最高等级夹住——格子等级可能把实际等级顶到上限以上，但提示框只该显示 x/最高等级。
	 *
	 * <p>标题里的技能名按神器品质上色；冷却 / 蓝耗优先用服务端随技能列表推来的实际值
	 * （已计入魔法书加速与这格书的 MP 减耗）。
	 */
	public static List<Component> tooltip(String entry) {
		Item item = itemOf(entry);

		if (!(item instanceof SephiriaArtifact artifact) || artifact.skill() == null) {
			return List.of();
		}

		int level = Math.min(levelOf(entry), artifact.maxLevel());
		List<Component> lines = new ArrayList<>();
		Component name = skillName(artifact.skill());
		name = artifact.rarity() == com.sephiria.artifact.ArtifactRarity.BOND
				? com.sephiria.util.Colours.rainbow(name.getString())
				: name.copy().withColor(artifact.rarity().color());
		lines.add(Component.translatable("screen.sephiria_fantasy.artifact_skills.skill_title", name));
		com.sephiria.artifact.skill.SkillTooltipContext.clearBook();

		ArtifactSkillsPayload.Entry actual = find(item, level);

		if (actual != null) {
			com.sephiria.artifact.skill.SkillTooltipContext.setActual(actual.cooldownTicks(), actual.mpCost());
		} else {
			// 技能列表里的等级对不上（旧栏位 / 刚升级）时，退回随属性同步的书本快照
			com.sephiria.network.MagicBookStatsPayload.Entry cached =
					com.sephiria.client.ClientBookStats.find(item, level, -1);

			if (cached != null) {
				com.sephiria.artifact.skill.SkillTooltipContext.setActual(cached.cooldownTicks(), cached.mpCost());
			}
		}

		try {
			lines.addAll(artifact.skillDescriptionLines(level));
		} finally {
			com.sephiria.artifact.skill.SkillTooltipContext.clearBook();
		}

		lines.add(Component.translatable("screen.sephiria_fantasy.artifact_skills.skill_level",
				level, artifact.maxLevel()));
		return lines;
	}

	/** 栏位字符串的可读名字；解析不出来时返回空。 */
	public static Component slotLabel(String entry) {
		Item item = itemOf(entry);

		if (item == null) {
			return Component.translatable("screen.sephiria_fantasy.artifact_skills.empty");
		}

		return label(item, levelOf(entry));
	}

	/**
	 * 两条技能栏字符串是不是同一件神器、同一个「封顶后」的等级。
	 *
	 * <p>旧栏位可能存着超过神器上限的等级（格子等级曾经把它顶上去），左边列表却是封顶后的等级；
	 * 按原始字符串比对会把同一技能同时显示在栏位和可用列表里，所以这里按物品 + 封顶等级比较。
	 */
	public static boolean sameEntry(String left, String right) {
		Item leftItem = itemOf(left);
		Item rightItem = itemOf(right);

		if (leftItem == null || leftItem != rightItem) {
			return false;
		}

		int maxLevel = leftItem instanceof SephiriaArtifact artifact ? artifact.maxLevel() : Integer.MAX_VALUE;
		return Math.min(levelOf(left), maxLevel) == Math.min(levelOf(right), maxLevel);
	}

	/** 当前栏位里已经放了哪些技能（用来在技能列表里标出「已装备」）。 */
	public static List<String> assignedEntries() {
		List<String> entries = new ArrayList<>();

		for (String slot : slots) {
			if (slot != null && !slot.isEmpty()) {
				entries.add(slot);
			}
		}

		return entries;
	}
}
