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

	/**
	 * 品质权重：默认（神器 / 石板宝箱）、商店、升级宝箱、药水各一套。
	 *
	 * <p>权重是<b>按件</b>算的，所以某一档的实际概率 = 该档件数 × 它的权重。羁绊只有几件
	 * （乐谱《台风》/ 封印的特加斯 / 永恒蚀），却要落在「稀有」与「传说」之间，于是它的权重不能照
	 * 「比传说大一点」来填：商店那套要按件数重新配（见 {@link #SHOP}）。
	 *
	 * @param bond     羁绊（神器最高品质）
	 * @param eternal  永恒（石板最高品质）——与羁绊分开，免得调商店的羁绊概率时把石板也带上
	 */
	public record Weights(int common, int advanced, int rare, int legendary, int bond, int eternal) {
		/** 神器 / 石板宝箱：白/蓝/黄/红/彩 = 30/40/10/2/1。 */
		public static final Weights DEFAULT = new Weights(30, 40, 10, 2, 1, 1);
		/**
		 * 商店：白/蓝/黄/红 = 30/40/10/2 与宝箱相同，只有<b>羁绊</b>提到 30
		 * （2026-10 用户要求「羁绊的出现概率调为稀有与传说之间」）。
		 *
		 * <p>算法按<b>件</b>展开就很直观：某一档的总权重 = 件数 × 它的权重。羁绊现在 3 件
		 * （乐谱《台风》/ 封印的特加斯 / 永恒蚀），要落在传说（11 件 × 2 = 22）与稀有（18 件 × 10 = 180）
		 * 之间，3 × 30 = 90 正好；想再调只动这一个数字（3 × 权重 要留在 22 与 180 之间）。
		 * ——件数会随后续批次变（这几行里的数字是 2026-10-14 那批的数），改档次时重新数一遍。
		 */
		public static final Weights SHOP = new Weights(30, 40, 10, 2, 30, 1);
		/** 升级宝箱：80/100/10/2/1（好东西更少）。 */
		public static final Weights UPGRADE = new Weights(80, 100, 10, 2, 1, 1);
		/** 药水（商店的两个药水格）：1000/10/5/1，白药水满地都是、红的极稀有。 */
		public static final Weights POTION = new Weights(1000, 10, 5, 1, 1, 1);

		int weightOf(ArtifactRarity rarity) {
			return switch (rarity) {
				case COMMON -> this.common;
				case ADVANCED -> this.advanced;
				case RARE -> this.rare;
				case LEGENDARY -> this.legendary;
				case BOND -> this.bond;
				case ETERNAL -> this.eternal;
			};
		}
	}

	private ArtifactLoot() {
	}

	/** 神器池（公开版：许愿池的卡池、锚定与神铸的清单都要用它）。 */
	public static List<Item> artifactPool() {
		return artifacts();
	}

	/** 石板池（公开版，同上）。 */
	public static List<Item> slatePool() {
		return slates();
	}

	/**
	 * 池子里某一档品质的物品（许愿池的「锚定 / 神铸」按档列清单用）。
	 *
	 * <p>顺序 = 池子本身的顺序，客户端与服务端看到的是同一份（同一个静态清单）。
	 */
	public static List<Item> ofRarity(List<Item> pool, ArtifactRarity rarity) {
		List<Item> tier = new ArrayList<>();

		for (Item item : pool) {
			if (item instanceof Quality quality && quality.rarity() == rarity) {
				tier.add(item);
			}
		}

		return tier;
	}

	/**
	 * 按品质权重掷一档（许愿池要先知道掷到哪一档，才好套「锚定」）。
	 *
	 * <p>权重口径与商店完全一致（按<b>件</b>算：某一档的总权重 = 件数 × 该档权重），
	 * 所以「概率与商店一致」这条就是靠它保证的。
	 */
	public static ArtifactRarity rollRarity(RandomSource random, List<Item> pool, Weights weights) {
		int total = 0;

		for (Item item : pool) {
			if (item instanceof Quality quality) {
				total += weights.weightOf(quality.rarity());
			}
		}

		if (total <= 0) {
			return null;
		}

		int roll = random.nextInt(total);
		int cursor = 0;

		for (Item item : pool) {
			if (!(item instanceof Quality quality)) {
				continue;
			}

			cursor += weights.weightOf(quality.rarity());

			if (roll < cursor) {
				return quality.rarity();
			}
		}

		return null;
	}

	/** 神器池（可按需要排除石板）。 */
	private static List<Item> artifacts() {
		return List.of(ModItems.CHARM_OF_STRENGTH, ModItems.WARRIORS_PROOF, ModItems.SHIELD_TEXTBOOK,
				ModItems.SWORD_TEXTBOOK, ModItems.WIND_SCORE, ModItems.PRESSURE_BANDAGE, ModItems.GOLDEN_CLOAK,
				ModItems.WANDERER_NECKLACE, ModItems.PROJECTION_SWORD, ModItems.COLORLESS_CUBE, ModItems.SILVER_PLATE, ModItems.ENCOURAGEMENT_BANNER, ModItems.HASTE_GRIMOIRE, ModItems.RED_DEW, ModItems.LONGING_AMULET, ModItems.FAULT_PROBE, ModItems.DEFT_AMULET, ModItems.PINWHEEL, ModItems.SPECIMEN_BEAK, ModItems.EVERGREEN_CLOAK, ModItems.RESISTANCE_BAND, ModItems.WARM_STONE,
				ModItems.CRITTONS_SEAL, ModItems.LUCKY_MEDAL, ModItems.GOLDEN_MAPLE_LEAF,
				ModItems.SWORD_EARRING, ModItems.KEEN_EYE,
				ModItems.MAGIC_CARROT, ModItems.SHARP_FLINT, ModItems.RESONANCE_STONE,
				ModItems.LIGHTNING_STRUCK_BRANCH, ModItems.ELECTRIC_BUG, ModItems.QILIN_HORN,
				ModItems.ELECTRIC_AMULET, ModItems.SANDE_EARRINGS, ModItems.THUNDER_VERDICT,
				ModItems.STORM_COMPASS, ModItems.FIREFLY, ModItems.TYPHOON_SCORE,
				ModItems.STONE_FLOWER, ModItems.SAPOTE_FRUIT, ModItems.LIGHTNING_ROD,
				ModItems.POINTY_ACORN, ModItems.THUNDER_STONE, ModItems.CLOUDSEED_ARROW,
				ModItems.MAST_MODEL, ModItems.RAVEN_TABLET,
				ModItems.SOLIS_FRACTO, ModItems.SOLIS_PARVO,
				ModItems.CRIMSON_SUNSET, ModItems.ETERNAL_FURNACE, ModItems.SOLIS_DECUSA,
				ModItems.METEORIC_EARRING, ModItems.SOLIS_DEKURI, ModItems.NOON_WHETSTONE,
				ModItems.METEORIC_MIRROR,
				ModItems.RED_SNAKE_EYE, ModItems.AMBERGRIS, ModItems.RED_YARN_BALL,
				ModItems.OAK_CHARCOAL, ModItems.FIRE_BUG, ModItems.LAVA_BEAD,
				ModItems.WISDOM_AMULET, ModItems.BLUE_CLAW, ModItems.PROSHIUM_RING,
				ModItems.SEALED_TEGAS,
				ModItems.ETERNAL_ECLIPSE,
				ModItems.EXORCISM_SCABBARD, ModItems.BLIZZARD_HAMMER, ModItems.MAGIC_CONCH,
				ModItems.ICE_WINGS, ModItems.FROZEN_HEART, ModItems.VOLSPA, ModItems.ICE_BOW);
	}

	private static List<Item> slates() {
		return List.of(ModItems.SLATE_OF_FUTURE, ModItems.SLATE_OF_OATH, ModItems.SLATE_OF_BELIEF,
				ModItems.SLATE_OF_ENTRANCE, ModItems.SLATE_OF_COMPETITION, ModItems.SLATE_OF_RALLY, ModItems.SLATE_OF_WAVE, ModItems.SLATE_OF_DOUBLE_STAR, ModItems.SLATE_OF_HANDSHAKE);
	}

	/** 抽奖池：神器宝箱只出神器、石板宝箱只出石板、升级宝箱与商店两者都出。 */
	public enum Pool {
		ARTIFACTS, SLATES, BOTH
	}

	/** 抽 {@code count} 件互不重复的东西（{@code pool} 决定池子，{@code weights} 决定品质权重）。 */
	public static List<ItemStack> roll(RandomSource random, int count, Pool pool, Weights weights) {
		List<Item> items = switch (pool) {
			case ARTIFACTS -> artifacts();
			case SLATES -> slates();
			case BOTH -> {
				List<Item> both = new ArrayList<>(artifacts());
				both.addAll(slates());
				yield both;
			}
		};

		return rollFrom(random, count, items, weights);
	}

	/** 抽 {@code count} 瓶互不重复的药水。 */
	public static List<ItemStack> rollPotions(RandomSource random, int count) {
		return rollFrom(random, count, ModItems.POTIONS, Weights.POTION);
	}

	/** 从给定池子里按品质加权抽 {@code count} 件，互不重复。 */
	public static List<ItemStack> rollFrom(RandomSource random, int count, List<Item> pool, Weights weights) {
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
			if (item instanceof Quality quality) {
				poolWeight += weights.weightOf(quality.rarity());
				pool.add(item);
			}
		}

		if (poolWeight <= 0) {
			return candidates.isEmpty() ? null : candidates.get(0);
		}

		int roll = random.nextInt(poolWeight);
		int cursor = 0;

		for (Item item : pool) {
			cursor += weights.weightOf(((Quality) item).rarity());

			if (roll < cursor) {
				return item;
			}
		}

		return pool.get(pool.size() - 1);
	}

	/** 物品的品质（没有品质的东西按普通算）。 */
	public static ArtifactRarity rarityOf(Item item) {
		return item instanceof Quality quality ? quality.rarity() : ArtifactRarity.COMMON;
	}
}
