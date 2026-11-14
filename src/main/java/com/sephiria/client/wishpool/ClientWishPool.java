package com.sephiria.client.wishpool;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 客户端侧的许愿池结果镜像：服务端抽完（或神铸完）把清单发过来，界面取走播动画。
 *
 * <p>只是一份「还没播的结果」——界面每帧取一次，取走就清空（所以不会重复播）。
 * 愿力数字也一起带过来：锚定加成的结果只有服务端知道，客户端按基础表重算会偏小。
 */
public final class ClientWishPool {
	/** 待播的一批结果：物品 + 实际到账的愿力（神铸为 0）。 */
	public record Result(List<ItemStack> items, int energyGained) {
	}

	private static Result pending;

	private ClientWishPool() {
	}

	/** 服务端发来一批结果（由 {@code SephiriaClient} 的包处理器调用）。 */
	public static void accept(List<ItemStack> items, int energyGained) {
		pending = new Result(List.copyOf(items), energyGained);
	}

	/** 取走待播的结果；没有就返回 null。 */
	public static Result takePending() {
		Result taken = pending;
		pending = null;
		return taken;
	}
}
