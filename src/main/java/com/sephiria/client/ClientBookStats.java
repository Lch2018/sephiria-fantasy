package com.sephiria.client;

import com.sephiria.network.MagicBookStatsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

/** 客户端镜像：服务端推来的魔法书实际冷却 / 蓝耗。 */
public final class ClientBookStats {
	private static List<MagicBookStatsPayload.Entry> books = List.of();

	private ClientBookStats() {
	}

	public static void accept(MagicBookStatsPayload payload) {
		books = payload.books();
	}

	public static void reset() {
		books = List.of();
	}

	/**
	 * 找这本书的实际数值：优先按「物品 + 等级 + 背包格」精确匹配，其次只按物品 + 等级。
	 * 找不到返回 null，调用方退回基础值 + 客户端现算。
	 */
	public static MagicBookStatsPayload.Entry find(Item item, int level, int slot) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);

		for (MagicBookStatsPayload.Entry entry : books) {
			if (entry.item().equals(id) && entry.level() == level && entry.slot() == slot) {
				return entry;
			}
		}

		for (MagicBookStatsPayload.Entry entry : books) {
			if (entry.item().equals(id) && entry.level() == level) {
				return entry;
			}
		}

		return null;
	}
}
