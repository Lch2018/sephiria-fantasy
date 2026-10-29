package com.sephiria.client.wishpool;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 最近一次许愿结果（仅客户端，供翻牌动画显示）。 */
public final class ClientWishPool {
	private static List<ItemStack> items = List.of();

	private ClientWishPool() {
	}

	public static void accept(List<ItemStack> drawn) {
		items = drawn;
	}

	public static List<ItemStack> items() {
		return items;
	}

	public static void clear() {
		items = List.of();
	}
}
