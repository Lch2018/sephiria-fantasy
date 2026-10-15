package com.sephiria.client.wishpool;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 客户端侧的许愿池结果镜像：服务端抽完（或神铸完）把清单发过来，界面取走播动画。
 *
 * <p>只是一份「还没播的结果」——界面每帧取一次，取走就清空（所以不会重复播）。
 */
public final class ClientWishPool {
	private static List<ItemStack> pending;

	private ClientWishPool() {
	}

	/** 服务端发来一批结果（由 {@code SephiriaClient} 的包处理器调用）。 */
	public static void accept(List<ItemStack> items) {
		pending = List.copyOf(items);
	}

	/** 取走待播的结果；没有就返回 null。 */
	public static List<ItemStack> takePending() {
		List<ItemStack> taken = pending;
		pending = null;
		return taken;
	}
}
