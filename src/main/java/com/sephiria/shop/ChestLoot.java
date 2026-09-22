package com.sephiria.shop;

import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
// 26.3 把整数/浮点两类数值提供者拆成了两个包，roll 数是整数版
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/**
 * 赛菲利亚货币进自然箱子：骰子与神器附魔币在所有 {@code minecraft:chests/*} 的表里
 * 各加一个 10% 概率的独立池子。
 *
 * <p>用 {@code LootTableEvents.MODIFY} 在运行时改表，所以村庄、雪屋、废弃矿井、林地府邸、
 * 末地城、远古城市、试炼密室这些箱子全都覆盖到，不用逐个写 JSON；而且对数据包加的表也生效。
 *
 * <p>两样是<b>各自独立</b>的池子（概率都是 10%，互不影响），所以一个箱子可能同时刷出两样——
 * 这比共用一个池子再 50/50 分配更贴近「和骰子一致」的原意：附魔币的出现率跟骰子完全相同。
 *
 * <p>胡萝卜只掉骰子、不掉附魔币，那是 {@code data/minecraft/loot_table/blocks/carrots.json}
 * 里单独写的，本类不管；附魔币的获取渠道刻意收窄到「开箱」，别把它加进任何采集掉落。
 */
public final class ChestLoot {
	/** 每种货币在自然箱子里的掉落概率：10%（骰子与附魔币共用这一个常量）。 */
	private static final float CHANCE = 0.1F;

	private ChestLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin() || !isChest(key)) {
				return;
			}

			tableBuilder.withPool(currencyPool(ModItems.DICE));
			tableBuilder.withPool(currencyPool(ModItems.ENCHANT_COIN));
		});
	}

	/** 一种货币一个池子：1 件 + 10% 概率门槛。 */
	private static LootPool.Builder currencyPool(Item item) {
		return LootPool.lootPool()
				// 26.3 的 setRolls 要 Holder<ContextIntProvider>
				.setRolls(Holder.direct(new ConstantValue(1)))
				.when(LootItemRandomChanceCondition.randomChance(CHANCE))
				.add(LootItem.lootTableItem(item));
	}

	/** 箱子表：id 以 minecraft:chests/ 开头（含试炼密室、远古城市等全部自然箱）。 */
	private static boolean isChest(ResourceKey<LootTable> key) {
		Identifier id = key.identifier();

		return id.getNamespace().equals("minecraft") && id.getPath().startsWith("chests/");
	}
}
