package com.sephiria.shop;

import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 骰子进自然箱子：所有 {@code minecraft:chests/*} 的表里加一个池子，10% 概率掉 1 个骰子。
 *
 * <p>用 {@code LootTableEvents.MODIFY} 在运行时改表，所以村庄、雪屋、废弃矿井、林地府邸、
 * 末地城、远古城市、试炼密室这些箱子全都覆盖到，不用逐个写 JSON；而且对数据包加的表也生效。
 */
public final class DiceLoot {
	/** 掉落概率：10%。 */
	private static final float CHANCE = 0.1F;

	private DiceLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin() || !isChest(key)) {
				return;
			}

			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.when(LootItemRandomChanceCondition.randomChance(CHANCE))
					.add(LootItem.lootTableItem(ModItems.DICE)));
		});
	}

	/** 箱子表：id 以 minecraft:chests/ 开头（含试炼密室、远古城市等全部自然箱）。 */
	private static boolean isChest(ResourceKey<LootTable> key) {
		Identifier id = key.identifier();

		return id.getNamespace().equals("minecraft") && id.getPath().startsWith("chests/");
	}
}
