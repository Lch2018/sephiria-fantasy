package com.sephiria.slate;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

import java.util.List;
import java.util.Map;

/**
 * 挖矿额外掉石板（用户 2026-10-17）：**不带精准采集**挖到指定矿石时，有小概率额外掉一块
 * 对应品质的石板（这一档里随机挑哪一块）。
 *
 * <p>实现与 {@code shop/ChestLoot} 同一套：{@code LootTableEvents.MODIFY} 往原版矿石的
 * 方块战利品表里加**独立的池子**，概率用 random_chance 门槛——对数据包改过的表也生效，
 * 而且不用整张覆盖原版表（那会跟着版本碎）。
 *
 * <p>「非精准采集」用条件表达：**带上精准采集就整个池子不掷**——与原版矿物表
 * 「精准给矿石本体 / 不精准给碎矿」的判定同源（can_silk_touch 谓词的反向）。
 *
 * <p>每个矿石的几档是**各自独立的池子**：所以铁矿（白 0.5% / 蓝 0.5%）理论上一次能同时
 * 多掉两块，与用户列的「白 0.5% / 蓝 0.5%」口径一致。
 */
public final class OreSlateDrops {
	/**
	 * 矿石（{@code minecraft:blocks/<方块>} 表的路径）→ 能额外掉的（品质，概率）清单。
	 * 用户 2026-10-17 的口径：铜 白1%；铁 白0.5%/蓝0.5%；金 蓝1%；钻石、绿宝石 黄5%/红1%。
	 */
	private static final Map<String, List<Tier>> ORES = Map.ofEntries(
			Map.entry("blocks/copper_ore", List.of(new Tier(ArtifactRarity.COMMON, 0.01F))),
			Map.entry("blocks/deepslate_copper_ore", List.of(new Tier(ArtifactRarity.COMMON, 0.01F))),
			Map.entry("blocks/iron_ore", List.of(
					new Tier(ArtifactRarity.COMMON, 0.005F), new Tier(ArtifactRarity.ADVANCED, 0.005F))),
			Map.entry("blocks/deepslate_iron_ore", List.of(
					new Tier(ArtifactRarity.COMMON, 0.005F), new Tier(ArtifactRarity.ADVANCED, 0.005F))),
			Map.entry("blocks/gold_ore", List.of(new Tier(ArtifactRarity.ADVANCED, 0.01F))),
			Map.entry("blocks/deepslate_gold_ore", List.of(new Tier(ArtifactRarity.ADVANCED, 0.01F))),
			Map.entry("blocks/diamond_ore", List.of(
					new Tier(ArtifactRarity.RARE, 0.05F), new Tier(ArtifactRarity.LEGENDARY, 0.01F))),
			Map.entry("blocks/deepslate_diamond_ore", List.of(
					new Tier(ArtifactRarity.RARE, 0.05F), new Tier(ArtifactRarity.LEGENDARY, 0.01F))),
			Map.entry("blocks/emerald_ore", List.of(
					new Tier(ArtifactRarity.RARE, 0.05F), new Tier(ArtifactRarity.LEGENDARY, 0.01F))),
			Map.entry("blocks/deepslate_emerald_ore", List.of(
					new Tier(ArtifactRarity.RARE, 0.05F), new Tier(ArtifactRarity.LEGENDARY, 0.01F))));

	/** 某一档的概率与要掉的品质。 */
	private record Tier(ArtifactRarity rarity, float chance) {
	}

	private OreSlateDrops() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			Identifier id = key.identifier();

			if (!source.isBuiltin() || !id.getNamespace().equals("minecraft")) {
				return;
			}

			List<Tier> tiers = ORES.get(id.getPath());

			if (tiers == null) {
				return;
			}

			for (Tier tier : tiers) {
				tableBuilder.withPool(slatePool(tier, registries));
			}
		});
	}

	/**
	 * 这一档的池子：每块该档石板一个等权重条目（掷到这档就随机挑一块）。
	 * 石板按品质从石板池里现挑——以后新增的石板自动进池子。
	 */
	private static LootPool.Builder slatePool(Tier tier, HolderLookup.Provider registries) {
		LootPool.Builder pool = LootPool.lootPool()
				.setRolls(Holder.direct(new ConstantValue(1)))
				.when(withoutSilkTouch(registries))
				.when(LootItemRandomChanceCondition.randomChance(tier.chance()));

		for (Item slate : ArtifactLoot.ofRarity(ArtifactLoot.slatePool(), tier.rarity())) {
			pool.add(LootItem.lootTableItem(slate));
		}

		return pool;
	}

	/**
	 * 「这次挖掘**没带**精准采集」：照原版 {@code can_silk_touch} 谓词的形状反向拼出来
	 * （那个谓词是数据包里的命名谓词，代码里引用不到，只能自己组一遍同款条件）。
	 * 精准采集的 Holder 由事件带来的注册表查——26.3 里附魔已经不是 BuiltIn 注册表了。
	 */
	private static LootItemCondition.Builder withoutSilkTouch(HolderLookup.Provider registries) {
		EnchantmentPredicate silk = new EnchantmentPredicate(silkTouch(registries), MinMaxBounds.Ints.atLeast(1));
		ItemPredicate.Builder silkTool = ItemPredicate.Builder.item().withComponents(new DataComponentMatchers(
				DataComponentExactPredicate.EMPTY,
				Map.of(DataComponentPredicates.ENCHANTMENTS, EnchantmentsPredicate.enchantments(List.of(silk)))));

		return InvertedLootItemCondition.invert(MatchTool.toolMatches(silkTool));
	}

	/** 精准采集的 Holder（从事件带来的注册表查）。 */
	private static Holder<Enchantment> silkTouch(HolderLookup.Provider registries) {
		return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
	}
}
