package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.MagicBook;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
 * <p>「有哪些技能可用」是<b>算出来的</b>，不另存一份：遍历赛菲莉娅背包，每件生效（等级不为负）
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
	/** 乔洛的浇水壶延迟 0.5 秒的额外施放。 */
	private static final List<ExtraCast> EXTRA_CASTS = new ArrayList<>();
	private record ExtraCast(UUID ownerId, ArtifactSkill skill, int level, long dueTick) {
	}

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
		register(IceBowSkill.INSTANCE);
		register(MagicBoltSkill.FIRE);
		register(MagicBoltSkill.ICE);
		register(MagicBoltSkill.LIGHTNING);
		register(FlameArrowSkill.INSTANCE);
		register(FrostKnifeSkill.INSTANCE);
		register(LightningBoomerangSkill.INSTANCE);
		register(FireCircusSkill.INSTANCE);
		register(ArrowRainSkill.INSTANCE);
		register(ThunderArmorSkill.INSTANCE);
		register(MeteorShowerSkill.INSTANCE);
		register(IceStormSkill.INSTANCE);
		register(FullFruitBucketSkill.INSTANCE);
		register(WaterGrabSkill.INSTANCE);
		register(ShieldSkill.INSTANCE);
		register(RockElephantSkill.INSTANCE);
		ServerTickEvents.END_SERVER_TICK.register(ArtifactSkills::tickExtraCasts);
	}

	public static ArtifactSkill byId(String id) {
		return BY_ID.get(id);
	}

	/** 一件神器在某个等级下给的技能（没有就是 null）。 */
	public static ArtifactSkill of(ItemStack stack) {
		return stack.getItem() instanceof com.sephiria.artifact.SephiriaArtifact artifact ? artifact.skill() : null;
	}

	/** 一条可用技能：神器物品（图标与名字都用它）+ 实际等级 + 技能 + 所在背包格。 */
	public record Available(Item item, int level, ArtifactSkill skill, int slot) {
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

			// 技能等级不能超过神器自身的最高等级（格子等级可能把它顶上去；数值表本身会夹住，
			// 但技能页与技能栏存的是这个数字，必须在这里统一夹住）。
			level = Math.min(level, artifact.maxLevel());
			list.add(new Available(stack.getItem(), level, artifact.skill(), slot));
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

		// 旧技能栏可能存着超过神器上限的等级（格子等级曾经把它顶上去）；按上限匹配，
		// 这样旧栏位仍然能解析到那件神器，只是结算和显示都用封顶后的等级。
		int maxLevel = item instanceof com.sephiria.artifact.SephiriaArtifact artifact
				? artifact.maxLevel() : Integer.MAX_VALUE;
		int wanted = Math.min(level, maxLevel);

		for (Available available : available(player)) {
			if (available.item() == item && available.level() == wanted) {
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

	/** 这一条技能的实际冷却（魔法书吃「魔法书加速」：速度 +x% → 时间 ÷(1+x%)）。 */
	public static int cooldownTicksFor(ServerPlayer player, Available resolved) {
		int base = resolved.skill().cooldownTicks(player, resolved.level());

		if (resolved.item() instanceof MagicBook) {
			double haste = ArtifactEffects.magicBookHastePercent(player, resolved.slot());

			if (haste > 0.0D) {
				base = Math.max(1, (int) Math.round(base / (1.0D + haste / 100.0D)));
			}
		}

		return base;
	}

	/** 这一条技能的实际蓝耗（魔法书吃学院 / 雷伊星碎片 / 怀表的减耗，魔弹再吃双角管的增加）。 */
	public static double mpCostFor(ServerPlayer player, Available resolved) {
		double base = resolved.skill().mpCost(player, resolved.level());
		double reduction = ArtifactEffects.magicBookMpReductionPercent(player, resolved.slot());

		if (reduction > 0.0D) {
			base = Math.max(0.0D, base * (1.0D - reduction / 100.0D));
		}

		if (resolved.skill() instanceof MagicBoltSkill) {
			double increase = ArtifactEffects.magicBoltMpCostIncreasePercent(player);

			if (increase > 0.0D) {
				base *= 1.0D + increase / 100.0D;
			}
		}

		return base;
	}

	/**
	 * 闪避成功时让所有魔法书的剩余冷却加速（学院钢笔）。
	 *
	 * <p>减的是「剩余时间」的百分比，只对还在冷却中的魔法书栏位生效。
	 */
	public static void accelerateMagicBooks(ServerPlayer player, double percent) {
		if (percent <= 0.0D) {
			return;
		}

		long[] slots = COOLDOWNS.get(player.getUUID());

		if (slots == null) {
			return;
		}

		List<String> entries = SkillSlots.of(player);
		long now = player.level().getGameTime();
		// 「加速 x%」= 冷却速度 ×(1+x%)，所以剩余时间 ÷(1+x%)
		double factor = 1.0D / (1.0D + Math.max(0.0D, percent) / 100.0D);
		boolean changed = false;

		for (int slot = 0; slot < SkillSlots.COUNT && slot < slots.length; slot++) {
			if (slots[slot] <= now) {
				continue;
			}

			Available resolved = resolve(player, entries.get(slot));

			if (resolved != null && resolved.item() instanceof MagicBook) {
				long remaining = slots[slot] - now;
				slots[slot] = now + Math.max(0L, Math.round(remaining * factor));
				changed = true;
			}
		}

		if (changed) {
			com.sephiria.artifact.ArtifactHud.send(player);
		}
	}

	/**
	 * 给玩家所有装着指定技能的栏位减冷却（雷电回旋镖接住时用）。
	 *
	 * <p>减的是固定 tick 数，不是按当前剩余比例；传 {@code cooldownTicks / 2} 就是「减少 50% 冷却」。
	 */
	public static void reduceCooldown(ServerPlayer player, ArtifactSkill skill, int ticks) {
		long[] slots = COOLDOWNS.get(player.getUUID());

		if (slots == null || ticks <= 0) {
			return;
		}

		List<String> entries = SkillSlots.of(player);
		long now = player.level().getGameTime();
		boolean changed = false;

		for (int slot = 0; slot < SkillSlots.COUNT && slot < slots.length; slot++) {
			if (slots[slot] <= now) {
				continue;
			}

			Available resolved = resolve(player, entries.get(slot));

			if (resolved != null && resolved.skill() == skill) {
				slots[slot] = Math.max(now, slots[slot] - ticks);
				changed = true;
			}
		}

		if (changed) {
			com.sephiria.artifact.ArtifactHud.send(player);
		}
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

		if (!resolved.skill().canCast(player, resolved.level())) {
			refuse(player);
			return;
		}

		if (!PlayerStats.spendMp(player, mpCostFor(player, resolved))) {
			refuse(player);
			return;
		}

		resolved.skill().cast(player, resolved.level());

		// 乔洛的浇水壶：最大 MP 达到门槛时，0.5 秒后额外无蓝耗再放一次（伤害独立）
		if (resolved.item() instanceof MagicBook && ArtifactEffects.wateringCanExtraCast(player)) {
			EXTRA_CASTS.add(new ExtraCast(player.getUUID(), resolved.skill(), resolved.level(),
					player.level().getGameTime() + 10L));
		}

		// 音叉：成功施放魔法书后攒一层「声波」
		if (resolved.item() instanceof MagicBook) {
			com.sephiria.artifact.TuningFork.onMagicBookCast(player);
		}

		COOLDOWNS.computeIfAbsent(player.getUUID(), uuid -> new long[SkillSlots.COUNT])[slot] =
				player.level().getGameTime() + cooldownTicksFor(player, resolved);
		// 冷却刚上：立刻推一次 HUD，别等下一个 10 刻周期
		com.sephiria.artifact.ArtifactHud.send(player);
	}

	private static void tickExtraCasts(MinecraftServer server) {
		if (EXTRA_CASTS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		List<ExtraCast> pending = new ArrayList<>(EXTRA_CASTS);
		EXTRA_CASTS.clear();

		for (ExtraCast extra : pending) {
			if (extra.dueTick() > now) {
				EXTRA_CASTS.add(extra);
				continue;
			}

			ServerPlayer player = server.getPlayerList().getPlayer(extra.ownerId());

			if (player == null || !player.isAlive()) {
				continue;
			}

			extra.skill().cast(player, extra.level());
		}
	}

	/** 玩家退出时清掉冷却表。 */
	public static void forget(ServerPlayer player) {
		COOLDOWNS.remove(player.getUUID());
		EXTRA_CASTS.removeIf(extra -> extra.ownerId().equals(player.getUUID()));
	}

	private static void refuse(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
	}
}
