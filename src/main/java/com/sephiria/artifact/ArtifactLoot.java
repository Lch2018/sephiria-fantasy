package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.slate.SlateItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器 / 石板的随机产出：商店的商品格与宝箱的奖励格都用它。
 *
 * <p>按品质加权抽：先抽品质（权重见 {@link Weights}），再在该品质里随机挑一件；同一批里
 * <b>不出现重复</b>（商店 6 格、宝箱 5 格都不允许两个一样）。
 *
 * <p>目前神器与石板各只有一两件，所以抽出来的数量会少于格位数，剩下的格子先空着。
 */
public final class ArtifactLoot {
	/** 一批里最多能出几件（受现有物品数量限制）。 */
	public static final int MAX_ROLL = 5;

	/** 品质权重：默认（商店 / 神器宝箱）与升级宝箱各一套。 */
	public record Weights(int common, int advanced, int rare, int legendary, int bond) {
		/** 商店与神器 / 石板宝箱：白/蓝/黄/红/彩 = 30/40/10/2/1。 */
		public static final Weights DEFAULT = new Weights(30, 40, 10, 2, 1);
		/** 升级宝箱：80/100/10/2/1（好东西更少）。 */
		public static final Weights UPGRADE = new Weights(80, 100, 10, 2, 1);

		int weightOf(ArtifactRarity rarity) {
			return switch (rarity) {
				case COMMON -> this.common;
				case ADVANCED -> this.advanced;
				case RARE -> this.rare;
				case LEGENDARY -> this.legendary;
				// 石板最高是永恒，抽到「羁绊」那一档时按永恒算
				case BOND, ETERNAL -> this.bond;
			};
		}
	}

	private ArtifactLoot() {
	}

	/** 神器池（可按需要排除石板）。 */
	private static List<Item> artifacts() {
		return List.of(ModItems.CHARM_OF_STRENGTH, ModItems.WARRIORS_PROOF, ModItems.SHIELD_TEXTBOOK,
				ModItems.SWORD_TEXTBOOK, ModItems.WIND_SCORE, ModItems.PRESSURE_BANDAGE, ModItems.GOLDEN_CLOAK,
				ModItems.WANDERER_NECKLACE, ModItems.PROJECTION_SWORD);
	}

	private static List<Item> slates() {
		return List.of(ModItems.SLATE_OF_FUTURE, ModItems.SLATE_OF_OATH, ModItems.SLATE_OF_BELIEF,
				ModItems.SLATE_OF_ENTRANCE, ModItems.SLATE_OF_COMPETITION);
	}

	/** 抽 {@code count} 件互不重复的神器（{@code withSlates} 时把石板也放进池子）。 */
	public static List<ItemStack> roll(RandomSource random, int count, boolean withSlates, Weights weights) {
		List<Item> pool = new ArrayList<>(artifacts());

		if (withSlates) {
			pool.addAll(slates());
		}

		List<ItemStack> result = new ArrayList<>();
		List<Item> remaining = new ArrayList<>(pool);

		while (result.size() < count && !remaining.isEmpty()) {
			Item picked = pickByRarity(random, remaining, weights);

			if (picked == null) {
				break;
			}

			remaining.remove(picked);
			result.add(new ItemStack(picked));
		}

		return result;
	}

	/** 先按品质权重抽一档，再从这一档里随机挑一件；这一档没有货就整体降级重试。 */
	private static Item pickByRarity(RandomSource random, List<Item> candidates, Weights weights) {
		// 只有「有品质」的物品参与抽签，权重按各自的品质取
		List<Item> pool = new ArrayList<>();
		int poolWeight = 0;

		for (Item item : candidates) {
			if (item instanceof SephiriaArtifact artifact) {
				poolWeight += weights.weightOf(artifact.rarity());
				pool.add(item);
			}
		}

		if (poolWeight <= 0) {
			return candidates.isEmpty() ? null : candidates.get(0);
		}

		int roll = random.nextInt(poolWeight);
		int cursor = 0;

		for (Item item : pool) {
			cursor += weights.weightOf(((SephiriaArtifact) item).rarity());

			if (roll < cursor) {
				return item;
			}
		}

		return pool.get(pool.size() - 1);
	}

	/** 物品的品质（没有品质的东西按普通算）。 */
	public static ArtifactRarity rarityOf(Item item) {
		if (item instanceof SephiriaArtifact artifact) {
			return artifact.rarity();
		}

		if (item instanceof SlateItem slate) {
			return slate.rarity();
		}

		return ArtifactRarity.COMMON;
	}
}
