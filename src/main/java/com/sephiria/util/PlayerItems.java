package com.sephiria.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

/**
 * 把物品交付给玩家：装得下的进背包，装不下的从玩家身上掉出来。
 *
 * <p>为什么不写成「{@code if (!player.getInventory().add(stack)) player.drop(stack, false, ...)}」——
 * {@code Inventory#add} 的返回值只回答「有没有装进去一点」，不是「全装进去了」：
 * <ul>
 *   <li><b>部分装下</b>时它也回 true（5 颗骰子只塞进 3 颗、剩下 2 颗没地方放），调用方以为
 *       已经交付完，剩下那部分就被吞掉；</li>
 *   <li>背包真的满时，<b>创造模式</b>下它会把整份清空并回 true（原版认为创造玩家可以自己刷），
 *       于是物品谁也没拿到——玩家 2026-10-16 报的「背包满时获得的物品会被吞掉（例如升级宝箱）」
 *       就是这个。</li>
 * </ul>
 *
 * <p>所以改走原版自己那条路：{@code placeItemBackInInventory} 逐个格子塞，塞不下就把剩下的整份
 * 掉在玩家身上（创造模式照掉）。以后凡是「给玩家物品」都从这里走，别再拿 {@code add} 的返回值判断。
 */
public final class PlayerItems {
	private PlayerItems() {
	}

	/** 交付一份物品；背包满（或只装得下一部分）时，装不下的那部分掉在玩家脚下。 */
	public static void give(ServerPlayer player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
	}
}
