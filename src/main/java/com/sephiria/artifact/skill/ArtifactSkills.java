package com.sephiria.artifact.skill;

import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.stats.PlayerStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 神器技能的注册表与释放流程。
 *
 * <p>「有哪些技能可用」是<b>算出来的</b>，不另存一份：遍历赛菲利亚背包，每件生效（等级不为负）
 * 且带技能词条的神器各出一条——所以把神器卸下，技能就自动从页面里消失；带两件就出现两条。
 *
 * <p>技能栏里存的是「神器物品 id @ 等级」这样的字符串，释放时按它回背包里找对应的神器；
 * 找不到（卸下了、或者附魔币升过级导致等级对不上）就当作失效，页面里会显示成空的。
 *
 * <p>冷却按<b>技能栏</b>记：同一件神器放进两个栏位就是两份独立的冷却。
 */
public final class ArtifactSkills {
	/** 全部技能（id → 技能）。 */
	private static final Map<String, ArtifactSkill> BY_ID = new LinkedHashMap<>();
	/** 每个玩家 6 个栏位的冷却结束时间（tick）。 */
	private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

	private ArtifactSkills() {
	}

	/** 注册技能（由 {@link #initialize()} 调用；技能都是无状态单例）。 */
	private static void register(ArtifactSkill skill) {
		BY_ID.put(skill.id(), skill);
	}

	/** 由 {@code Sephiria#onInitialize()} 调用。 */
	public static void initialize() {
		register(EncouragementBannerSkill.INSTANCE);
		register(HasteSkill.INSTANCE);
		register(ThunderVerdictSkill.INSTANCE);
	}

	public static ArtifactSkill byId(String id) {
		return BY_ID.get(id);
	}

	/** 一件神器在某个等级下给的技能（没有就是 null）。 */
	public static ArtifactSkill of(ItemStack stack) {
		return stack.getItem() instanceof com.sephiria.artifact.SephiriaArtifact artifact ? artifact.skill() : null;
	}

	/** 一条可用技能：神器物品（图标与名字都用它）+ 实际等级 + 技能。 */
	public record Available(Item item, int level, ArtifactSkill skill) {
	}

	/**
	 * 玩家此刻可用的技能列表：背包里每件生效、带技能词条的神器各一条，顺序即页面里的展示顺序。
	 */
	public static List<Available> available(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		List<Available> list = new ArrayList<>();

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof com.sephiria.artifact.SephiriaArtifact artifact) || artifact.skill() == null) {
				continue;
			}

			int level = com.sephiria.artifact.ArtifactEffects.effectiveLevel(stack, backpack.slotLevel(slot));

			if (level < 0) {
				continue;
			}

			list.add(new Available(stack.getItem(), level, artifact.skill()));
		}

		return list;
	}

	/** 把「神器物品 id @ 等级」打包成技能栏里存的字符串；空栏位是空串。 */
	public static String encode(Item item, int level) {
		return BuiltInRegistries.ITEM.getKey(item) + "@" + level;
	}

	/** 技能栏字符串 → 背包里对应的神器与等级；找不到（卸下了 / 等级对不上）返回 null。 */
	public static Available resolve(ServerPlayer player, String entry) {
		if (entry == null || entry.isEmpty()) {
			return null;
		}

		int at = entry.lastIndexOf('@');

		if (at <= 0) {
			return null;
		}

		Identifier id = Identifier.tryParse(entry.substring(0, at));
		Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);

		if (item == null) {
			return null;
		}

		int level;

		try {
			level = Integer.parseInt(entry.substring(at + 1));
		} catch (NumberFormatException exception) {
			return null;
		}

		for (Available available : available(player)) {
			if (available.item() == item && available.level() == level) {
				return available;
			}
		}

		return null;
	}

	/** 某个栏位还要等多少 tick 才能再放（0 = 可以放）。 */
	public static int cooldownLeft(ServerPlayer player, int slot) {
		long[] slots = COOLDOWNS.get(player.getUUID());

		if (slots == null) {
			return 0;
		}

		return (int) Math.max(0L, slots[slot] - player.level().getGameTime());
	}

	/**
	 * 按下第 {@code slot} 号技能栏。
	 *
	 * <p>顺序：栏位里的神器还在不在 → 冷却 → 蓝量 → 放技能 → 上冷却。任何一步不满足都只响一声
	 * 「不行」，不改任何状态（免得白白扣蓝）。
	 */
	public static void cast(ServerPlayer player, int slot) {
		if (slot < 0 || slot >= SkillSlots.COUNT) {
			return;
		}

		Available resolved = resolve(player, SkillSlots.of(player).get(slot));

		if (resolved == null) {
			refuse(player);
			return;
		}

		if (cooldownLeft(player, slot) > 0) {
			refuse(player);
			return;
		}

		if (!PlayerStats.spendMp(player, resolved.skill().mpCost(resolved.level()))) {
			refuse(player);
			return;
		}

		resolved.skill().cast(player, resolved.level());
		COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new long[SkillSlots.COUNT])[slot] =
				player.level().getGameTime() + resolved.skill().cooldownTicks();
	}

	/** 玩家退出时清掉冷却表。 */
	public static void forget(ServerPlayer player) {
		COOLDOWNS.remove(player.getUUID());
	}

	private static void refuse(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
	}
}
