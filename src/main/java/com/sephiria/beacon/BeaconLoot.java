package com.sephiria.beacon;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.ItemQualities;
import com.sephiria.artifact.Quality;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.potion.PotionEffect;
import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛菲莉娅信标里的那一件奖励：按结构生成时所在的群系抽。
 *
 * <p>类型比例是<b>神器 8 / 石板 2 / 神器附魔币 2 / 药水 1 / 赛菲莉娅额外背包栏 1</b>
 * （用户 2026-10-27 定，背包栏为后加）。
 * 神器与石板只出<b>稀有 / 传说</b>各一半；附魔币固定传说。
 *
 * <p><b>药水不套这组稀有 / 传说比例</b>：火 / 冰 / 电 / 物理元素药水现在有高级（+2）与稀有（+4）
 * 两档；如果也按稀有 / 传说各半，高级档会被挤掉。所以药水按群系元素规则单独从全部药水里抽——
 * 指定元素共占 60%（同一元素内高级与稀有等概率），其他元素药水为 0%，剩下 40% 给非元素药水。
 */
public final class BeaconLoot {
	/** 类型权重总和：8 + 2 + 2 + 1 + 1。 */
	private static final double TYPE_TOTAL = 14.0D;
	private static final double ARTIFACT_CUT = 8.0D;
	private static final double SLATE_CUT = 10.0D;
	private static final double COIN_CUT = 12.0D;
	private static final double POTION_CUT = 13.0D;

	private record ComboWeight(ArtifactCombo combo, int weight) {
	}

	private record ElementWeight(PotionEffect.Kind kind, int weight) {
	}

	private record Profile(List<ComboWeight> combos, List<ElementWeight> elements) {
	}

	private static final Profile DESERT = profile(
			line(c(ArtifactCombo.EMBER, 5), c(ArtifactCombo.SUN_SWORD, 5), c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.FIRE, 60)));
	private static final Profile ICE = profile(
			line(c(ArtifactCombo.FROST_ARMAMENT, 5), c(ArtifactCombo.GLACIER, 5), c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.ICE, 60)));
	private static final Profile BAMBOO = profile(
			line(c(ArtifactCombo.SHADOW, 7), c(ArtifactCombo.WIND_SONG, 3), c(ArtifactCombo.PRECISION, 3)),
			List.of());
	private static final Profile JAGGED_PEAKS = profile(
			line(c(ArtifactCombo.DARK_CLOUD, 5), c(ArtifactCombo.MAGIC_TECH, 5),
					c(ArtifactCombo.FROST_ARMAMENT, 1), c(ArtifactCombo.GLACIER, 1), c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.LIGHTNING, 30), e(PotionEffect.Kind.PHYSICAL, 30)));
	private static final Profile FROZEN_PEAKS = profile(
			line(c(ArtifactCombo.DARK_CLOUD, 1), c(ArtifactCombo.MAGIC_TECH, 1),
					c(ArtifactCombo.FROST_ARMAMENT, 1), c(ArtifactCombo.GLACIER, 1), c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.LIGHTNING, 30), e(PotionEffect.Kind.ICE, 30)));
	private static final Profile STONY_PEAKS = profile(
			line(c(ArtifactCombo.DARK_CLOUD, 5), c(ArtifactCombo.MAGIC_TECH, 5), c(ArtifactCombo.STURDY, 5),
					c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.LIGHTNING, 30), e(PotionEffect.Kind.PHYSICAL, 30)));
	private static final Profile WIND = profile(line(c(ArtifactCombo.WIND_SONG, 1)), List.of());
	private static final Profile PRECISION = profile(line(c(ArtifactCombo.PRECISION, 1)), List.of());
	private static final Profile GROVE = profile(
			line(c(ArtifactCombo.GLACIER, 2), c(ArtifactCombo.WIND_SONG, 2), c(ArtifactCombo.SHADOW, 2),
					c(ArtifactCombo.ELEMENT, 1)),
			elements(e(PotionEffect.Kind.ICE, 60)));
	private static final Profile WINDSWEPT_FOREST = profile(
			line(c(ArtifactCombo.WIND_SONG, 1), c(ArtifactCombo.SHADOW, 1)), List.of());
	private static final Profile SHADOW = profile(line(c(ArtifactCombo.SHADOW, 1)), List.of());
	private static final Profile TAIGA = profile(
			line(c(ArtifactCombo.SHADOW, 2), c(ArtifactCombo.WIND_SONG, 1), c(ArtifactCombo.STURDY, 1)), List.of());
	private static final Profile SNOWY_TAIGA = profile(
			line(c(ArtifactCombo.GLACIER, 2), c(ArtifactCombo.SHADOW, 2), c(ArtifactCombo.WIND_SONG, 1),
					c(ArtifactCombo.STURDY, 1)),
			elements(e(PotionEffect.Kind.ICE, 30), e(PotionEffect.Kind.PHYSICAL, 30)));
	private static final Profile FLOWER = profile(
			line(c(ArtifactCombo.PRECISION, 1), c(ArtifactCombo.SHADOW, 1), c(ArtifactCombo.WIND_SONG, 1)),
			List.of());
	private static final Profile SPARSE_JUNGLE = profile(
			line(c(ArtifactCombo.SHADOW, 5), c(ArtifactCombo.WIND_SONG, 5), c(ArtifactCombo.ELEMENT, 1)), List.of());
	private static final Profile DAPPLED = profile(
			line(c(ArtifactCombo.SHADOW, 1), c(ArtifactCombo.PRECISION, 1), c(ArtifactCombo.WIND_SONG, 1)),
			List.of());
	private static final Profile SWAMP = profile(
			line(c(ArtifactCombo.SHADOW, 1), c(ArtifactCombo.PRECISION, 1)), List.of());
	private static final Profile PLAINS = profile(line(c(ArtifactCombo.WIND_SONG, 1)), List.of());
	private static final Profile SNOWY = profile(
			line(c(ArtifactCombo.WIND_SONG, 1), c(ArtifactCombo.GLACIER, 1)),
			elements(e(PotionEffect.Kind.ICE, 60)));
	private static final Profile STONY_SHORE = profile(
			line(c(ArtifactCombo.STURDY, 1)), elements(e(PotionEffect.Kind.PHYSICAL, 60)));
	private static final Profile SAVANNA = profile(
			line(c(ArtifactCombo.WIND_SONG, 1), c(ArtifactCombo.EMBER, 1)),
			elements(e(PotionEffect.Kind.FIRE, 60)));
	private static final Profile BADLANDS = profile(
			line(c(ArtifactCombo.STURDY, 1), c(ArtifactCombo.WIND_SONG, 1), c(ArtifactCombo.EMBER, 1)),
			elements(e(PotionEffect.Kind.FIRE, 30), e(PotionEffect.Kind.PHYSICAL, 30)));
	private static final Profile ERODED_BADLANDS = profile(
			line(c(ArtifactCombo.STURDY, 1), c(ArtifactCombo.WIND_SONG, 1)),
			elements(e(PotionEffect.Kind.PHYSICAL, 60)));
	private static final Profile DEFAULT = profile(line(c(ArtifactCombo.SHADOW, 1)), List.of());

	private BeaconLoot() {
	}

	/** 生成信标里的那一件。结构只在标签里的群系生成，默认分支只是防止标签被改坏后空手。 */
	public static ItemStack roll(LevelReader level, BlockPos pos, RandomSource random) {
		Profile profile = profileFor(level.getBiome(pos));
		double roll = random.nextDouble() * TYPE_TOTAL;

		if (roll < ARTIFACT_CUT) {
			return wrapped(pickArtifact(profile, random));
		}

		if (roll < SLATE_CUT) {
			return wrapped(pickSlate(random));
		}

		if (roll < COIN_CUT) {
			// 附魔币这一档固定给 3 个（同一格内堆叠；容器允许叠到 3，其他物品仍只能放 1 个）
			return new ItemStack(ModItems.ENCHANT_COIN, 3);
		}

		if (roll < POTION_CUT) {
			return wrapped(pickPotion(profile, random));
		}

		return new ItemStack(ModItems.EXTRA_BACKPACK_ROW);
	}

	/**
	 * 光柱颜色：跟物品自己的品质色走；没有品质的物品给白色。
	 *
	 * <p><b>必须保留 alpha=$FF</b>：原版信标的实心光柱直接把 Section 颜色当顶点色用，
	 * Bliss / Photon 等光影会按顶点 alpha 剔除（alpha &lt; 1 或 &lt; 0.5），
	 * 早先这里用 {@code & 0xFFFFFF} 把 alpha 抹成 0，于是无光影还能看到辉光、开光影就整条消失。
	 */
	public static int beamColor(ItemStack stack) {
		Item item = stack.getItem();

		if (item instanceof Quality quality) {
			return quality.rarity().color() | 0xFF000000;
		}

		ArtifactRarity rarity = ItemQualities.of(item);
		return rarity == null ? 0xFFFFFFFF : rarity.color() | 0xFF000000;
	}

	private static ItemStack wrapped(Item item) {
		// 理论上不会走到空手；真没货时给附魔币，至少不生成一个空信标
		return new ItemStack(item == null ? ModItems.ENCHANT_COIN : item);
	}

	private static Profile profileFor(Holder<Biome> biome) {
		String path = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");

		return switch (path) {
			case "desert" -> DESERT;
			case "ice_spikes", "snowy_slopes", "frozen_river" -> ICE;
			case "bamboo_jungle" -> BAMBOO;
			case "jagged_peaks" -> JAGGED_PEAKS;
			case "frozen_peaks" -> FROZEN_PEAKS;
			case "stony_peaks" -> STONY_PEAKS;
			case "meadow", "windswept_hills", "windswept_gravelly_hills" -> WIND;
			case "cherry_grove" -> PRECISION;
			case "grove" -> GROVE;
			case "windswept_forest" -> WINDSWEPT_FOREST;
			case "forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "jungle" -> SHADOW;
			case "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga" -> TAIGA;
			case "snowy_taiga" -> SNOWY_TAIGA;
			case "flower_forest" -> FLOWER;
			case "sparse_jungle" -> SPARSE_JUNGLE;
			case "dappled_forest" -> DAPPLED;
			case "swamp", "mangrove_swamp", "pale_garden" -> SWAMP;
			case "river", "beach", "plains", "sunflower_plains" -> PLAINS;
			case "snowy_beach", "snowy_plains" -> SNOWY;
			case "stony_shore" -> STONY_SHORE;
			case "savanna", "savanna_plateau", "windswept_savanna" -> SAVANNA;
			case "badlands" -> BADLANDS;
			case "eroded_badlands", "wooded_badlands" -> ERODED_BADLANDS;
			default -> DEFAULT;
		};
	}

	private static Item pickArtifact(Profile profile, RandomSource random) {
		ArtifactCombo combo = pickCombo(profile.combos(), random);

		if (combo == null) {
			return null;
		}

		List<Item> rare = new ArrayList<>();
		List<Item> legendary = new ArrayList<>();

		for (Item item : ArtifactLoot.artifactPool()) {
			if (!(item instanceof SephiriaArtifact artifact) || !artifact.combos().contains(combo)) {
				continue;
			}

			ArtifactRarity rarity = ArtifactLoot.rarityOf(item);

			if (rarity == ArtifactRarity.RARE) {
				rare.add(item);
			} else if (rarity == ArtifactRarity.LEGENDARY) {
				legendary.add(item);
			}
		}

		return pickFromTier(rare, legendary, random);
	}

	private static Item pickSlate(RandomSource random) {
		return pickFromTier(ArtifactLoot.ofRarity(ArtifactLoot.slatePool(), ArtifactRarity.RARE),
				ArtifactLoot.ofRarity(ArtifactLoot.slatePool(), ArtifactRarity.LEGENDARY), random);
	}

	/**
	 * 稀有 / 传说各一半；某一档在连击里没有货（例如风之歌没有传说、冰川没有稀有）时，
	 * 只能从有货的那一档里挑——不能为了凑 1:1 给这个连击硬造一件不存在的品质。
	 */
	private static Item pickFromTier(List<Item> rare, List<Item> legendary, RandomSource random) {
		List<Item> pool;

		if (rare.isEmpty()) {
			pool = legendary;
		} else if (legendary.isEmpty()) {
			pool = rare;
		} else {
			pool = random.nextBoolean() ? rare : legendary;
		}

		return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
	}

	private static Item pickPotion(Profile profile, RandomSource random) {
		List<ElementWeight> preferred = profile.elements();

		if (preferred.isEmpty()) {
			return randomOf(ModItems.POTIONS, random);
		}

		int roll = random.nextInt(100);
		int accumulated = 0;

		for (ElementWeight element : preferred) {
			accumulated += element.weight();

			if (roll < accumulated) {
				Item picked = randomOf(potionsOf(element.kind()), random);

				if (picked != null) {
					return picked;
				}
			}
		}

		// 剩下 40%：非元素药水；其他元素的药水按要求完全不出现
		Item other = randomOf(nonElementPotions(), random);
		return other != null ? other : randomOf(ModItems.POTIONS, random);
	}

	private static List<Item> potionsOf(PotionEffect.Kind kind) {
		List<Item> result = new ArrayList<>();

		for (Item item : ModItems.POTIONS) {
			if (item instanceof SephiriaPotionItem potion && potion.effect().kind() == kind) {
				result.add(item);
			}
		}

		return result;
	}

	private static List<Item> nonElementPotions() {
		List<Item> result = new ArrayList<>();

		for (Item item : ModItems.POTIONS) {
			if (!(item instanceof SephiriaPotionItem potion) || !isElement(potion.effect().kind())) {
				result.add(item);
			}
		}

		return result;
	}

	private static boolean isElement(PotionEffect.Kind kind) {
		return kind == PotionEffect.Kind.FIRE || kind == PotionEffect.Kind.ICE
				|| kind == PotionEffect.Kind.LIGHTNING || kind == PotionEffect.Kind.PHYSICAL;
	}

	private static Item randomOf(List<Item> pool, RandomSource random) {
		return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
	}

	private static ArtifactCombo pickCombo(List<ComboWeight> combos, RandomSource random) {
		int total = 0;

		for (ComboWeight combo : combos) {
			total += combo.weight();
		}

		if (total <= 0) {
			return null;
		}

		int roll = random.nextInt(total);
		int accumulated = 0;

		for (ComboWeight combo : combos) {
			accumulated += combo.weight();

			if (roll < accumulated) {
				return combo.combo();
			}
		}

		return combos.get(combos.size() - 1).combo();
	}

	private static Profile profile(List<ComboWeight> combos, List<ElementWeight> elements) {
		return new Profile(combos, elements);
	}

	private static ComboWeight c(ArtifactCombo combo, int weight) {
		return new ComboWeight(combo, weight);
	}

	private static ElementWeight e(PotionEffect.Kind kind, int weight) {
		return new ElementWeight(kind, weight);
	}

	private static List<ComboWeight> line(ComboWeight... combos) {
		return List.of(combos);
	}

	private static List<ElementWeight> elements(ElementWeight... elements) {
		return List.of(elements);
	}
}
