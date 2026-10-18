package com.sephiria.artifact;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sephiria.Sephiria;
import com.sephiria.registry.ModItems;
import com.sephiria.slate.SlateItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

	/**
	 * 神器 / 石板的随机产出：商店的商品格与宝箱的奖励格都用它。
	 *
	 * <p>按品质加权抽：先抽品质（权重见 {@link Weights}），再在该品质里随机挑一件；同一批里
	 * <b>不出现重复</b>（商店 6 格、宝箱 5 格都不允许两个一样）。
	 *
	 * <p>神器与石板混着出时（商店货架、升级宝箱）走 {@link #rollMixed}：先按调用方给的
	 * 神器/石板比例掷「这一件抽哪一边」，再在那一侧按品质掷。
	 */
public final class ArtifactLoot {
	/** 一批里最多能出几件（受现有物品数量限制）。 */
	public static final int MAX_ROLL = 5;

	/**
	 * 品质概率：宝箱（神器 / 石板 / 升级共用一组）、商店货架、抽卡、药水各一套。
	 *
	 * <p>口径（2026-10-18 用户定）：权重是**这一档实际开出的概率份数**——抽多了各品质的数量比
	 * 就是权重比本身，**与这一档里有几件东西无关**（先掷档、再在档里随机挑一件）。
	 * 所以加新物品不会稀释别的档；调概率只动这组数字。
	 *
	 * @param bond     羁绊（神器最高品质）
	 * @param eternal  永恒（石板最高品质）——与羁绊分开，想给它们不同的概率时互不影响
	 */
	public record Weights(double common, double advanced, double rare, double legendary, double bond, double eternal) {
		/**
		 * 神器 / 石板宝箱：30/30/10/3/5/1（用户 2026-10-18 定的统一比值，商店 / 抽卡 / 升级宝箱
		 * 现在都是同一组）。
		 *
		 * <p>口径（2026-10-18 定）：权重就是**这一档实际开出的概率份数**——抽多了各品质的数量比
		 * 就是这组数，**与这一档里有几件东西无关**（掷到哪档之后才在档里随机挑一件）。
		 * 所以加新物品不会稀释别的档，调概率只动这组数字。
		 */
		/**
		 * 神器 / 石板 / 升级三种宝箱：白47/蓝47/黄3.32/红0.917/彩1.608（用户 2026-10-18 定）。
		 * 这是「单张」的比值——玩家在 5 选 1 里会挑品质更高的（红 &gt; 彩 &gt; 黄 &gt; 蓝 &gt; 白），
		 * 最终拿到的分布会被「5 张里至少出一张」放大高档、几乎抹掉低档（算给用户对过账）。
		 */
		public static final Weights DEFAULT = new Weights(47, 47, 3.32, 0.917, 1.608, 1);
		/** 商店货架：白20/蓝20/黄10/红3/彩5（用户 2026-10-18 定的，与抽卡分开了）。 */
		public static final Weights SHOP = new Weights(20, 20, 10, 3, 5, 1);
		/** 抽卡（许愿池）：白30/蓝30/黄10/红3/彩5（用户 2026-10-18 定的）。 */
		public static final Weights GACHA = new Weights(30, 30, 10, 3, 5, 1);
		/** 药水（商店的两个药水格）：档位份数 200/10/5/1，白药水常见、红的极稀有。 */
		public static final Weights POTION = new Weights(200, 10, 5, 1, 1, 1);

		double weightOf(ArtifactRarity rarity) {
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

	/**
	 * 产出来源：宝箱（神器 / 石板 / 升级共用一组）、商店货架、抽卡（许愿池）、药水格。
	 * 「概率函数」按它取比值——整合包想改哪个来源就改哪个来源的键。
	 */
	public enum WeightsKind {
		CHEST, SHOP, GACHA, POTION
	}

	/** 各来源的内置默认值；覆盖文件里缺的档回退到它。 */
	private static final Map<WeightsKind, Weights> BUILTIN = Map.of(
			WeightsKind.CHEST, Weights.DEFAULT,
			WeightsKind.SHOP, Weights.SHOP,
			WeightsKind.GACHA, Weights.GACHA,
			WeightsKind.POTION, Weights.POTION);

	/** config 覆盖（服务器启动时读一遍；空 = 没有覆盖，全用内置默认）。 */
	private static Map<WeightsKind, Weights> overrides = Map.of();

	/**
	 * **概率的唯一入口**：任何要抽神器 / 石板 / 药水的地方都从这里拿比值，别直接引用
	 * {@code Weights.DEFAULT} 那几个常量（它们只是内置默认）。
	 *
	 * <p>整合包 / 服主的接口：放一份 {@code config/sephiria-fantasy-loot-weights.json}
	 * （第一次启动会自动生成默认文件），键是 {@code chest / shop / gacha / potion}，
	 * 每档的数字就是「抽多了各品质开出的数量比」；缺的档回退到内置默认，改完重启服务端生效。
	 */
	public static Weights weightsFor(WeightsKind kind) {
		return overrides.getOrDefault(kind, BUILTIN.get(kind));
	}

	/** 概率覆盖的入口注册：服务器每次启动（重新）读一遍覆盖文件，改完 JSON 重启即生效。 */
	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> loadOverrides());
		loadOverrides();
	}

	/** 读（没有就生成）覆盖文件。 */
	private static void loadOverrides() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("sephiria-fantasy-loot-weights.json");
		Map<WeightsKind, Weights> loaded = new EnumMap<>(WeightsKind.class);

		if (!Files.exists(file)) {
			writeDefaults(file);
		} else {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

				for (WeightsKind kind : WeightsKind.values()) {
					String key = kind.name().toLowerCase(Locale.ROOT);

					if (root.has(key) && root.get(key).isJsonObject()) {
						JsonObject entry = root.getAsJsonObject(key);
						Weights base = BUILTIN.get(kind);
						// 每一档都可以只写一部分：缺的档用内置默认补上
						loaded.put(kind, new Weights(
								opt(entry, "common", base.common()),
								opt(entry, "advanced", base.advanced()),
								opt(entry, "rare", base.rare()),
								opt(entry, "legendary", base.legendary()),
								opt(entry, "bond", base.bond()),
								opt(entry, "eternal", base.eternal())));
					}
				}

				overrides = loaded;
				Sephiria.LOGGER.info("[SEPHIRIA] 概率覆盖已载入：{} 个来源（{}）", loaded.size(), file.getFileName());
			} catch (Exception exception) {
				// 读不出来就沿用现在的值（首次是空 = 内置默认），别让一个坏文件弄挂服务器
				Sephiria.LOGGER.warn("[SEPHIRIA] 概率覆盖文件读不出来，沿用当前值：{}（{}）", file, exception);
			}
		}
	}

	/** 没有覆盖文件时生成一份默认的（全部键 + 内置默认值），整合包作者照着改。 */
	private static void writeDefaults(Path file) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			JsonObject root = new JsonObject();

			for (WeightsKind kind : WeightsKind.values()) {
				Weights weights = BUILTIN.get(kind);
				JsonObject entry = new JsonObject();
				entry.addProperty("common", weights.common());
				entry.addProperty("advanced", weights.advanced());
				entry.addProperty("rare", weights.rare());
				entry.addProperty("legendary", weights.legendary());
				entry.addProperty("bond", weights.bond());
				entry.addProperty("eternal", weights.eternal());
				root.add(kind.name().toLowerCase(Locale.ROOT), entry);
			}

			writer.write(gson.toJson(root));
			Sephiria.LOGGER.info("[SEPHIRIA] 已生成概率覆盖文件的默认版：{}（改完重启服务端生效）", file);
		} catch (IOException exception) {
			Sephiria.LOGGER.warn("[SEPHIRIA] 概率覆盖默认文件写不出去：{}（{}）", file, exception);
		}
	}

	/** 覆盖文件里读一个可缺省的数字档。 */
	private static double opt(JsonObject object, String field, double fallback) {
		return object.has(field) && !object.get(field).isJsonNull() ? object.get(field).getAsDouble() : fallback;
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
	 * 按品质掷一档（许愿池要先知道掷到哪一档，才好套「锚定」）。
	 *
	 * <p>权重口径（2026-10-18 用户定）：**权重就是这一档的概率份数**——抽多了，各品质开出的
	 * 数量比就是权重比本身（30 : 30 : 10 : 3 : 5），与这一档里有几件东西**无关**。
	 * 所以「抽卡与商店概率一致」这条，现在靠的是两边都用同一组档位份数。
	 */
	public static ArtifactRarity rollRarity(RandomSource random, List<Item> pool, Weights weights) {
		// 与 pickByRarity 同一口径：权重 = 这一档的概率份数，**不乘件数**；没货的档不参与
		double total = 0;
		List<ArtifactRarity> available = new ArrayList<>();

		for (ArtifactRarity rarity : ArtifactRarity.values()) {
			if (!ofRarity(pool, rarity).isEmpty()) {
				available.add(rarity);
				total += weights.weightOf(rarity);
			}
		}

		if (total <= 0) {
			return null;
		}

		double roll = random.nextDouble() * total;

		for (ArtifactRarity rarity : available) {
			roll -= weights.weightOf(rarity);

			if (roll < 0) {
				return rarity;
			}
		}

		return available.get(available.size() - 1);
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
				ModItems.ICE_WINGS, ModItems.FROZEN_HEART, ModItems.VOLSPA, ModItems.ICE_BOW,
				ModItems.BOOK);
	}

	private static List<Item> slates() {
		return List.of(ModItems.SLATE_OF_FUTURE, ModItems.SLATE_OF_OATH, ModItems.SLATE_OF_BELIEF,
				ModItems.SLATE_OF_ENTRANCE, ModItems.SLATE_OF_COMPETITION, ModItems.SLATE_OF_RALLY, ModItems.SLATE_OF_WAVE, ModItems.SLATE_OF_DOUBLE_STAR, ModItems.SLATE_OF_HANDSHAKE,
				ModItems.SLATE_OF_SEVERANCE,
				ModItems.SLATE_OF_WEDGE,
				ModItems.SLATE_OF_DEFENSE,
				ModItems.SLATE_OF_DROUGHT,
				ModItems.SLATE_OF_EXPLOITATION,
				ModItems.SLATE_OF_UNITY,
				ModItems.SLATE_OF_PEACE,
				ModItems.SLATE_OF_DAYDREAM,
				ModItems.SLATE_OF_REBELLION,
				ModItems.SLATE_OF_JUNCTION,
				ModItems.SLATE_OF_GRATITUDE,
				ModItems.SLATE_OF_ARRIVAL);
	}

	/**
	 * 神器与石板混着抽：<b>每一件先掷「这一件抽哪一边」</b>（石板占 {@code slatePercent}%，
	 * 其余是神器），再在那一侧按品质掷档、在档里随机挑一件。
	 *
	 * <p>口径（用户 2026-10-18）：商店是神器 75% / 石板 25%、升级宝箱是神器 60% / 石板 40%
	 * ——比例由调用方给（{@code ShopStock} 与 {@code ChestItem.Kind}），这里只管掷。
	 *
	 * <p>同一批里不重复：两侧各自去重就够（两边没有同名的东西）。掷中的一侧抽干了（比如只要
	 * 石板、石板却比格数少）就用另一侧补，别空着格子。
	 */
	public static List<ItemStack> rollMixed(RandomSource random, int count, double slatePercent, Weights weights) {
		List<Item> artifacts = new ArrayList<>(artifacts());
		List<Item> slates = new ArrayList<>(slates());
		List<ItemStack> result = new ArrayList<>();

		while (result.size() < count) {
			boolean wantSlate = random.nextDouble() * 100.0D < slatePercent;
			List<Item> pool = wantSlate ? slates : artifacts;

			if (pool.isEmpty()) {
				pool = wantSlate ? artifacts : slates;
			}

			if (pool.isEmpty()) {
				break;
			}

			Item picked = pickByRarity(random, pool, weights);

			if (picked == null) {
				break;
			}

			pool.remove(picked);
			result.add(new ItemStack(picked));
		}

		return result;
	}

	/** 抽 {@code count} 瓶互不重复的药水（商店药水格用）。 */
	public static List<ItemStack> rollPotions(RandomSource random, int count) {
		return rollFrom(random, count, ModItems.POTIONS, weightsFor(WeightsKind.POTION));
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
		// 先掷「档位」（权重 = 这一档的概率份数，**不乘件数**），再在这一档里随机挑一件；
		// 掷到的档没货就不会被掷到（归堆时就排掉了），等价于外层重试时整体降级
		List<ArtifactRarity> available = new ArrayList<>();
		List<List<Item>> tierItems = new ArrayList<>();
		double total = 0;

		for (ArtifactRarity rarity : ArtifactRarity.values()) {
			List<Item> tier = new ArrayList<>();

			for (Item item : candidates) {
				if (item instanceof Quality quality && quality.rarity() == rarity) {
					tier.add(item);
				}
			}

			if (!tier.isEmpty()) {
				available.add(rarity);
				tierItems.add(tier);
				total += weights.weightOf(rarity);
			}
		}

		if (total <= 0) {
			return candidates.isEmpty() ? null : candidates.get(0);
		}

		double roll = random.nextDouble() * total;

		for (int index = 0; index < available.size(); index++) {
			roll -= weights.weightOf(available.get(index));

			if (roll < 0) {
				List<Item> tier = tierItems.get(index);
				return tier.get(random.nextInt(tier.size()));
			}
		}

		return null;
	}

	/** 物品的品质（没有品质的东西按普通算）。 */
	public static ArtifactRarity rarityOf(Item item) {
		return item instanceof Quality quality ? quality.rarity() : ArtifactRarity.COMMON;
	}
}
