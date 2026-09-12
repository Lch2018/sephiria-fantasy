package com.sephiria.client;

import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

/**
 * 客户端入口：只负责在武器提示框里补上「分支 + 命中倍率」。
 *
 * <p>放在客户端是因为提示框本来就只存在于客户端，也因为 26.2 的原版 {@code appendHoverText}
 * 已经过时——原版改成由实现了 {@code TooltipProvider} 的数据组件提供提示行，
 * 模组这边用 Fabric 的 {@link ItemTooltipCallback} 更直接。
 */
public class SephiriaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof SephiriaWeapon weapon) {
				lines.add(weapon.tooltipLine());
			}
		});
	}
}
