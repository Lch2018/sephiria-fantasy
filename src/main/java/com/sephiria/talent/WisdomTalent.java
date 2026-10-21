package com.sephiria.talent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.backpack.ArtifactBackpack;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 天赋「智慧」二阶段：所有神器强制【独特】，相同神器放进背包时合并。
 *
 * <p>合并规则（用户 2026-10-21）：
 * <ul>
 *   <li>把一件神器放进赛菲莉娅背包时，如果背包里已有同种神器，两件合并：</li>
 *   <li>原有的那件等级 +1（不超过该神器的最高等级），放入的那件被消耗；</li>
 *   <li>第一次合并额外让该神器的连击 +1（合并提供的连击最多 +1，继续合并只加等级）；</li>
 *   <li>被消耗的那件记在存档里：二阶段熄灭时按记录还原等级，并把它从玩家身上掉出来；</li>
 *   <li>经过合并的神器（原持有那件）在二阶段期间<b>不能从背包移出</b>。</li>
 * </ul>
 *
 * <p>二阶段点亮时先整理一次背包：同种神器只留等级最高的那一件，其余的从玩家身上掉出。
 */
public final class WisdomTalent {
	/** 一条合并记录：原物品 id、原格子、被消耗的那件、等级增量、是否给过连击。 */
	public record Merge(String item, int slot, ItemStack absorbed, int levelDelta, boolean comboGranted) {
		static final Codec<Merge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("item").forGetter(Merge::item),
				Codec.INT.fieldOf("slot").forGetter(Merge::slot),
				ItemStack.CODEC.fieldOf("absorbed").forGetter(Merge::absorbed),
				Codec.INT.fieldOf("level_delta").forGetter(Merge::levelDelta),
				Codec.BOOL.optionalFieldOf("combo", false).forGetter(Merge::comboGranted)
		).apply(instance, Merge::new));
	}

	public record Saved(List<Merge> merges) {
		static final Saved EMPTY = new Saved(List.of());

		static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Merge.CODEC.listOf().optionalFieldOf("merges", List.of()).forGetter(Saved::merges)
		).apply(instance, Saved::new));
	}

	private static final AttachmentType<Saved> SAVED = AttachmentRegistry.<Saved>builder()
			.persistent(Saved.CODEC)
			.copyOnDeath()
			.buildAndRegister(Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wisdom_merges"));

	private WisdomTalent() {
	}

	/** 二阶段是否点亮（所有神器按【独特】结算）。 */
	public static boolean forceUnique(ServerPlayer player) {
		return Talents.forceArtifactUnique(player);
	}

	/**
	 * 尝试把 {@code incoming} 与背包里的同种神器合并。
	 *
	 * <p>调用方负责在返回 true 后把 {@code incoming} 从原处扣掉（这里已经 shrink 过 1，
	 * 但菜单的光标/槽位要由调用方清空）。返回 false 表示没有可合并的目标，按正常放置处理。
	 */
	public static boolean tryMerge(ServerPlayer player, ItemStack incoming) {
		if (!forceUnique(player) || incoming.isEmpty()
				|| !(incoming.getItem() instanceof SephiriaArtifact artifact)) {
			return false;
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		int slot = findSame(backpack, incoming.getItem(), -1);

		if (slot < 0) {
			return false;
		}

		ItemStack original = backpack.getItem(slot);
		int before = ArtifactItem.levelOf(original);
		int after = Math.min(artifact.maxLevel(), before + 1);
		ArtifactItem.setLevel(original, after);

		String itemId = itemId(incoming);
		boolean comboGranted = !hasComboGrant(player, itemId);
		List<Merge> list = new ArrayList<>(merges(player));
		list.add(new Merge(itemId, slot, incoming.copyWithCount(1), after - before, comboGranted));
		player.setAttached(SAVED, new Saved(List.copyOf(list)));

		incoming.shrink(1);
		backpack.setChanged();

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.5F);
		player.sendSystemMessage(Component.translatable("message.sephiria_fantasy.artifact_merged",
				original.getHoverName()), true);
		return true;
	}

	/**
	 * 合并给某一路连击的额外等级（+1）；二阶段没点亮时恒为 0。
	 *
	 * <p>「合并后的神器会增加 2 次连击数」的口径：神器自己算 1 级，合并再补 1 级，合计 2；
	 * 继续合并只加神器等级、不再加连击（用户后面补的「最多 +1」）。
	 */
	public static int comboBonus(ServerPlayer player, ArtifactCombo combo) {
		if (!forceUnique(player)) {
			return 0;
		}

		int bonus = 0;

		for (Merge merge : merges(player)) {
			if (!merge.comboGranted()) {
				continue;
			}

			Item item = itemOf(merge.item());

			if (item instanceof SephiriaArtifact artifact && artifact.combos().contains(combo)) {
				bonus++;
			}
		}

		return bonus;
	}

	/** 这件神器是不是「经过合并的原件」：二阶段期间不能从背包移出。 */
	public static boolean isLocked(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer) || !forceUnique(serverPlayer)
				|| stack.isEmpty()) {
			return false;
		}

		String id = itemId(stack);

		for (Merge merge : merges(serverPlayer)) {
			if (merge.item().equals(id)) {
				return true;
			}
		}

		return false;
	}

	/** 阶段变化：点亮时整理重复，熄灭时还原合并并掉出被消耗的那件。 */
	public static void onStageChanged(ServerPlayer player, int oldStage, int newStage) {
		if (newStage >= 2 && oldStage < 2) {
			cleanDuplicates(player);
		} else if (newStage < 2 && oldStage >= 2) {
			releaseMerges(player);
		}
	}

	/** 进服对账：二阶段点亮时整理一次重复；没点亮但还有合并记录时还原并掉出。 */
	public static void onJoin(ServerPlayer player) {
		if (forceUnique(player)) {
			cleanDuplicates(player);
		} else if (!merges(player).isEmpty()) {
			releaseMerges(player);
		}
	}

	/** 点亮二阶段：同种神器只留等级最高的那件，其余从玩家身上掉出。 */
	public static void cleanDuplicates(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		Map<String, int[]> best = new LinkedHashMap<>();
		boolean dropped = false;

		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof SephiriaArtifact)) {
				continue;
			}

			String id = itemId(stack);
			int level = ArtifactItem.levelOf(stack);
			int[] current = best.get(id);

			if (current == null) {
				best.put(id, new int[] { slot, level });
				continue;
			}

			// 留等级高的；同级留先看到的
			if (level > current[1]) {
				ItemStack old = backpack.getItem(current[0]);
				backpack.setItem(current[0], ItemStack.EMPTY);
				player.drop(old.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
				best.put(id, new int[] { slot, level });
				dropped = true;
			} else {
				backpack.setItem(slot, ItemStack.EMPTY);
				player.drop(stack.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
				dropped = true;
			}
		}

		if (dropped) {
			backpack.setChanged();
			player.sendSystemMessage(
					Component.translatable("message.sephiria_fantasy.artifact_merge_dropped"), true);
		}
	}

	/** 熄灭二阶段：按记录还原等级，并把被消耗的那件掉出来。 */
	public static void releaseMerges(ServerPlayer player) {
		List<Merge> list = merges(player);

		if (list.isEmpty()) {
			return;
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);

		for (Merge merge : list) {
			int slot = findSame(backpack, itemOf(merge.item()), merge.slot());

			if (slot >= 0) {
				ItemStack original = backpack.getItem(slot);
				int level = ArtifactItem.levelOf(original);
				ArtifactItem.setLevel(original, Math.max(0, level - merge.levelDelta()));
			}

			player.drop(merge.absorbed().copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		player.setAttached(SAVED, Saved.EMPTY);
		backpack.setChanged();
		player.sendSystemMessage(
				Component.translatable("message.sephiria_fantasy.artifact_merge_dropped"), true);
	}

	/** 某件物品（按 id）在背包里的格子；优先用 preferredSlot。 */
	private static int findSame(ArtifactBackpack backpack, Item item, int preferredSlot) {
		if (preferredSlot >= 0 && preferredSlot < ArtifactBackpack.MAX_SLOTS
				&& backpack.getItem(preferredSlot).getItem() == item) {
			return preferredSlot;
		}

		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			if (backpack.getItem(slot).getItem() == item) {
				return slot;
			}
		}

		return -1;
	}

	private static boolean hasComboGrant(ServerPlayer player, String itemId) {
		for (Merge merge : merges(player)) {
			if (merge.comboGranted() && merge.item().equals(itemId)) {
				return true;
			}
		}

		return false;
	}

	private static List<Merge> merges(ServerPlayer player) {
		Saved saved = player.getAttached(SAVED);
		return saved == null ? List.of() : saved.merges();
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static Item itemOf(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? null : BuiltInRegistries.ITEM.get(key).map(ref -> ref.value()).orElse(null);
	}
}
