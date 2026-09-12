package com.sephiria;

import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/**
 * 标准剑盾是"一物两形"：主手渲染剑、副手渲染盾（见物品定义里的 display_context 分流）。
 * 为了让这个整体概念站得住，主手拿着剑盾时副手物品暂时不可用——避免出现"剑盾之外还能用另一只手"
 * 的怪异状态，也避开双持交互的各种边角 bug。
 *
 * <p>副手物品只是被禁用，并没有被移动或丢弃，所以放下剑盾后一切照旧。
 */
public final class OffHandGuard {
	private OffHandGuard() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (hand != InteractionHand.OFF_HAND) {
				return InteractionResult.PASS;
			}
			return player.getMainHandItem().is(ModItems.DEFAULT_SWORD_AND_SHIELD)
					? InteractionResult.FAIL
					: InteractionResult.PASS;
		});
	}
}
