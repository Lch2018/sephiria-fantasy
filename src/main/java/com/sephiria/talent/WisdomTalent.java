package com.sephiria.talent;

import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.backpack.ArtifactBackpack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * 天赋「智慧」二阶段：相同神器放进背包时合并。
 *
 * <p>合并规则（用户 2026-10-24 重做、2026-10-25 收窄）：
 * <ul>
 *   <li>只有背包里存在<b>未满级</b>的同种神器时才合并；同种神器全部满级时，新放进去的那件
 *       正常占格、不合并（例如背包里已有满级的 a1，再放 0 级的 a2 会正常放下；再放 a3 才与
 *       a2 合并，a2 变成满级；之后的 a4 又正常放下）；</li>
 *   <li><b>等级低的那件被消耗</b>（等级相同时消耗放进去的那件，保留背包里原有的、位置不变）；</li>
 *   <li>留下的那件：<b>等级 += 被消耗那件的等级 + 1</b>（不超过该神器的最高等级），
 *       并且它提供的<b>连击数量 +1</b>——每件神器本来就有 1 级，合并一次变 2 级
 *       （{@link ArtifactItem#MAX_COMBO_COUNT} 封顶）；</li>
 *   <li>合并后的神器就是<b>普通神器</b>——可以移出赛菲莉娅背包、可以丢出，
 *       二阶段熄灭后也不还原、不失效（等级与连击都是物品栈自己的组件了）；</li>
 *   <li>二阶段点亮时不再把背包里的重复神器丢出去；也不再把这些重复神器强制按【独特】结算
 *       （2026-10-25 去掉，重复神器恢复为各算一份）。</li>
 * </ul>
 */
public final class WisdomTalent {
	private WisdomTalent() {
	}

	/** 二阶段是否点亮（相同神器放进背包时合并）。 */
	public static boolean mergeActive(ServerPlayer player) {
		return Talents.wisdomMergeActive(player);
	}

	/**
	 * 尝试把 {@code incoming} 与背包里未满级的同种神器合并。
	 *
	 * <p>返回 true 表示 {@code incoming} 已经被消耗（调用方不要再把它放进格子）；
	 * 返回 false 表示按正常放置处理——注意「放进去的那件等级更高」时也返回 false，
	 * 但那时背包里原有的那件已经被消耗、{@code incoming} 的等级 / 连击已经加过了，
	 * 调用方照常把它放进目标格即可（位置就是玩家放它的位置）。
	 */
	public static boolean tryMerge(ServerPlayer player, ItemStack incoming) {
		if (!mergeActive(player) || incoming.isEmpty()
				|| !(incoming.getItem() instanceof SephiriaArtifact artifact)) {
			return false;
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		int slot = findUnmaxed(backpack, incoming, artifact);

		if (slot < 0) {
			return false;
		}

		ItemStack existing = backpack.getItem(slot);
		int incomingLevel = ArtifactItem.levelOf(incoming);
		int existingLevel = ArtifactItem.levelOf(existing);
		boolean incomingWins = incomingLevel > existingLevel;
		ItemStack survivor = incomingWins ? incoming : existing;
		int consumedLevel = incomingWins ? existingLevel : incomingLevel;

		// 等级：留下的那件 += 被消耗那件的等级 **+1**（用户 2026-10-24 定的最终口径：
		// a1 1 级被 a2 2 级吃掉 → a2 变 4 级），再夹到该神器的上限。
		// 连击数量：合并一次 +1 级（每件神器本来就提供 1 级，所以合并后是 2 级、封顶）。
		int levelBefore = ArtifactItem.levelOf(survivor);
		int comboBefore = ArtifactItem.comboCountOf(survivor);
		ArtifactItem.setLevel(survivor, Math.min(artifact.maxLevel(),
				levelBefore + consumedLevel + 1));
		ArtifactItem.setComboBonus(survivor, ArtifactItem.comboBonusOf(survivor) + 1);
		// 实际涨了多少（封顶时是 0）：动作栏按真实数字报，别写死
		int levelGain = ArtifactItem.levelOf(survivor) - levelBefore;
		int comboGain = ArtifactItem.comboCountOf(survivor) - comboBefore;

		if (incomingWins) {
			// 背包里那件等级更低：它被消耗，放进去的那件照常落进玩家点的格子
			backpack.setItem(slot, ItemStack.EMPTY);
		} else {
			// 放进去的那件等级不高于原有的：它被消耗，原有的留在原格子
			incoming.shrink(1);
		}

		backpack.setChanged();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.5F);
		player.sendSystemMessage(Component.translatable("message.sephiria_fantasy.artifact_merged",
				survivor.getHoverName(),
				Component.literal("+" + levelGain),
				Component.literal("+" + comboGain)), true);
		return !incomingWins;
	}

	/** 某件物品在背包里<b>未满级</b>的格子（找不到返回 -1；全部满级 = 不合并、正常放入）。 */
	private static int findUnmaxed(ArtifactBackpack backpack, ItemStack incoming, SephiriaArtifact artifact) {
		int max = artifact.maxLevel();

		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (stack.getItem() == incoming.getItem() && ArtifactItem.levelOf(stack) < max) {
				return slot;
			}
		}

		return -1;
	}
}
