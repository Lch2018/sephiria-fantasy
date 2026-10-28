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
 * <p>合并规则（用户 2026-10-24 重做、2026-10-25 收窄、2026-10-27 改目标规则）：
 * <ul>
 *   <li>只有背包里存在<b>还有成长空间</b>的同种神器时才合并：等级未满，<b>或者</b>连击还不是 +2。
 *       等级与连击都满了才算“没有成长空间”，不再参与合并（2026-10-27 用户改的口径：
 *       之前是“等级满就不合并”，会把满级但连击 +1 的那件也排除掉）；</li>
 *   <li><b>放进去的那件如果等级已满、连击也已是 +2</b>，也直接正常放入背包、不参与合并——
 *       否则会消耗背包里那件却什么都涨不了；</li>
 *   <li><b>等级低的那件被消耗</b>（等级相同时消耗放进去的那件，保留背包里原有的、位置不变）；</li>
 *   <li>留下的那件：<b>等级 += 被消耗那件的等级 + 1</b>（不超过该神器的最高等级），
 *       并且它提供的<b>连击数量 +1</b>——每件神器本来就有 1 级，合并一次变 2 级
 *       （{@link ArtifactItem#MAX_COMBO_COUNT} 封顶）；</li>
 *   <li>合并后的神器就是<b>普通神器</b>——可以移出赛菲莉娅背包、可以丢出，
 *       二阶段熄灭后也不还原、不失效（等级与连击都是物品栈自己的组件了）；</li>
 *   <li>二阶段点亮时不再把背包里的重复神器丢出去；也不再把这些重复神器强制按【独特】结算
 *       （2026-10-26 用户再次确认：重复神器恢复为各算一份）。</li>
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
	 * 尝试把 {@code incoming} 与背包里还有成长空间的同种神器合并。
	 *
	 * <p>返回 true 表示 {@code incoming} 已经被消耗（调用方不要再把它放进格子）。
	 * 返回 false 表示调用方按正常放置处理：通常是没找到可合并的目标（包括放进去的这件
	 * 已经满级 +2，见上面的限制）；例外是「放进去的那件等级更高」——那时背包里原有的那件
	 * 已经被消耗、{@code incoming} 的等级 / 连击已经加过了，调用方照常把它放进目标格即可，
	 * 位置就是玩家放它的位置。
	 */
	public static boolean tryMerge(ServerPlayer player, ItemStack incoming) {
		if (!mergeActive(player) || incoming.isEmpty()
				|| !(incoming.getItem() instanceof SephiriaArtifact artifact)) {
			return false;
		}

		// 放进去的这件已经没有任何成长空间：等级满、连击也满。
		// 此时再合并只会消耗背包里的同种神器，留下的还是这件满级 +2，纯亏一件，所以按普通放置处理。
		if (isFullyMaxed(incoming, artifact)) {
			return false;
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		int slot = findMergeable(backpack, incoming, artifact);

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

	/** 等级与连击都到顶：这件物品已经没有任何成长空间（不能再当合并目标，放进去也不该再合并）。 */
	private static boolean isFullyMaxed(ItemStack stack, SephiriaArtifact artifact) {
		return ArtifactItem.levelOf(stack) >= artifact.maxLevel()
				&& ArtifactItem.comboCountOf(stack) >= ArtifactItem.MAX_COMBO_COUNT;
	}

	/**
	 * 某件物品在背包里还有成长空间的格子：等级未满，或连击还不是 +2。
	 * 找不到返回 -1（同种神器都满级 +2 = 不合并、正常放入）。
	 */
	private static int findMergeable(ArtifactBackpack backpack, ItemStack incoming, SephiriaArtifact artifact) {
		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (stack.getItem() == incoming.getItem() && !isFullyMaxed(stack, artifact)) {
				return slot;
			}
		}

		return -1;
	}
}
