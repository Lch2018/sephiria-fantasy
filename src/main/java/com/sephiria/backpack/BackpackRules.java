package com.sephiria.backpack;

import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.talent.PatienceTalent;
import com.sephiria.talent.WisdomTalent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 背包里「可以移动、但不能离开赛菲莉娅背包」的几类东西：
 *
 * <ul>
 *   <li><b>石板基础</b>（天赋「耐心」一阶段）：能在背包格之间拖 / 换位，但不能丢到地上、
 *       不能放进普通背包，也不能被石板刻印消耗；</li>
 *   <li><b>合并过的神器</b>（天赋「智慧」二阶段）：同上，但额外允许放进附魔界面的神器槽升级。</li>
 * </ul>
 *
 * <p>为什么不能只靠 {@code Slot.mayPickup} 拦：那样会把「在背包里换格」也一起禁掉。
 * 现在的口径是<b>允许拿起</b>，由菜单在「放到哪里」这一步判断：放到背包格（或附魔神器槽）放行，
 * 放到玩家背包 / 丢出 / 被 Q 丢出则取消；关界面时如果还拿在光标上，由
 * {@link #returnToBackpack} 放回背包。
 */
public final class BackpackRules {
	private BackpackRules() {
	}

	/** 这两类东西不能被带出赛菲莉娅背包。 */
	public static boolean isProtected(Player player, ItemStack stack) {
		return PatienceTalent.isBase(stack) || WisdomTalent.isLocked(player, stack);
	}

	/** 是否是合并过、被锁住（但仍可放进附魔神器槽）的神器。 */
	public static boolean isLockedArtifact(Player player, ItemStack stack) {
		return stack.getItem() instanceof SephiriaArtifact && WisdomTalent.isLocked(player, stack);
	}

	/** 关界面时光标上还剩一件受保护物品：放回背包空格；背包满就把第一格挪出去、它放第一格。 */
	public static void returnToBackpack(ServerPlayer player, ItemStack stack) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			if (backpack.getItem(slot).isEmpty()) {
				backpack.setItem(slot, stack.copy());
				backpack.setChanged();
				return;
			}
		}

		// 没有空格：把第一格挪到脚下（与「耐心」石板基础的口径一致），受保护物品留在第一格
		ItemStack first = backpack.getItem(0);

		if (!first.isEmpty()) {
			player.drop(first.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		backpack.setItem(0, stack.copy());
		backpack.setChanged();
	}
}
