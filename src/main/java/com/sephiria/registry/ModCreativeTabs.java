package com.sephiria.registry;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> WEAPONS_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "weapons"));

	public static final CreativeModeTab WEAPONS = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			WEAPONS_KEY,
			// 26.2 的 Fabric API 里这个模块叫 fabric-creative-tab-api-v1（老的 item-group-api 已改名）。
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria_fantasy.weapons"))
					.icon(() -> new ItemStack(ModItems.DEFAULT_SWORD_AND_SHIELD))
					.displayItems((parameters, output) -> {
						ModItems.BASE_WEAPONS.forEach(weapon -> output.accept(weapon.item()));
					})
					.build());

	/** 神器标签页：标签页图标就是神器系统的那个袋子（用一个隐藏物品承载图标）。 */
	public static final ResourceKey<CreativeModeTab> ARTIFACTS_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifacts"));

	public static final CreativeModeTab ARTIFACTS = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			ARTIFACTS_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria_fantasy.artifacts"))
					.icon(() -> new ItemStack(ModItems.ARTIFACT_TAB_ICON))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.CHARM_OF_STRENGTH);
						output.accept(ModItems.WARRIORS_PROOF);
						output.accept(ModItems.SHIELD_TEXTBOOK);
						output.accept(ModItems.SWORD_TEXTBOOK);
						output.accept(ModItems.WIND_SCORE);
						output.accept(ModItems.PRESSURE_BANDAGE);
						output.accept(ModItems.GOLDEN_CLOAK);
						output.accept(ModItems.WANDERER_NECKLACE);
						output.accept(ModItems.PROJECTION_SWORD);
						output.accept(ModItems.COLORLESS_CUBE);
						output.accept(ModItems.SILVER_PLATE);
						output.accept(ModItems.ENCOURAGEMENT_BANNER);
						output.accept(ModItems.HASTE_GRIMOIRE);
						output.accept(ModItems.RED_DEW);
						output.accept(ModItems.LONGING_AMULET);
						output.accept(ModItems.FAULT_PROBE);
						output.accept(ModItems.DEFT_AMULET);
						output.accept(ModItems.PINWHEEL);
						output.accept(ModItems.SPECIMEN_BEAK);
						output.accept(ModItems.EVERGREEN_CLOAK);
						output.accept(ModItems.RESISTANCE_BAND);
						output.accept(ModItems.WARM_STONE);
						output.accept(ModItems.CRITTONS_SEAL);
						output.accept(ModItems.LUCKY_MEDAL);
						output.accept(ModItems.GOLDEN_MAPLE_LEAF);
						output.accept(ModItems.SWORD_EARRING);
						output.accept(ModItems.KEEN_EYE);
						output.accept(ModItems.MAGIC_CARROT);
						output.accept(ModItems.SHARP_FLINT);
						output.accept(ModItems.RESONANCE_STONE);
						output.accept(ModItems.LIGHTNING_STRUCK_BRANCH);
						output.accept(ModItems.ELECTRIC_BUG);
						output.accept(ModItems.QILIN_HORN);
						output.accept(ModItems.ELECTRIC_AMULET);
						output.accept(ModItems.SANDE_EARRINGS);
						output.accept(ModItems.THUNDER_VERDICT);
						output.accept(ModItems.STORM_COMPASS);
						output.accept(ModItems.FIREFLY);
						output.accept(ModItems.TYPHOON_SCORE);
						output.accept(ModItems.STONE_FLOWER);
						output.accept(ModItems.SAPOTE_FRUIT);
						output.accept(ModItems.LIGHTNING_ROD);
						output.accept(ModItems.POINTY_ACORN);
						output.accept(ModItems.THUNDER_STONE);
						output.accept(ModItems.CLOUDSEED_ARROW);
						output.accept(ModItems.MAST_MODEL);
						output.accept(ModItems.RAVEN_TABLET);
						output.accept(ModItems.SOLIS_FRACTO);
						output.accept(ModItems.SOLIS_PARVO);
						output.accept(ModItems.CRIMSON_SUNSET);
						output.accept(ModItems.ETERNAL_FURNACE);
						output.accept(ModItems.SOLIS_DECUSA);
						output.accept(ModItems.METEORIC_EARRING);
						output.accept(ModItems.SOLIS_DEKURI);
						output.accept(ModItems.NOON_WHETSTONE);
						output.accept(ModItems.METEORIC_MIRROR);
						output.accept(ModItems.RED_SNAKE_EYE);
						output.accept(ModItems.AMBERGRIS);
						output.accept(ModItems.RED_YARN_BALL);
						output.accept(ModItems.OAK_CHARCOAL);
						output.accept(ModItems.FIRE_BUG);
						output.accept(ModItems.LAVA_BEAD);
						output.accept(ModItems.WISDOM_AMULET);
						output.accept(ModItems.BLUE_CLAW);
						output.accept(ModItems.PROSHIUM_RING);
						output.accept(ModItems.SEALED_TEGAS);
						output.accept(ModItems.ETERNAL_ECLIPSE);
						output.accept(ModItems.EXORCISM_SCABBARD);
						output.accept(ModItems.BLIZZARD_HAMMER);
						output.accept(ModItems.MAGIC_CONCH);
						output.accept(ModItems.ICE_WINGS);
						output.accept(ModItems.FROZEN_HEART);
						output.accept(ModItems.VOLSPA);
						output.accept(ModItems.ICE_BOW);
						output.accept(ModItems.BOOK);
						output.accept(ModItems.SIX_LEAF_CLOVER);
					})
					.build());

	/** Slates tab: slates are not artifacts, they get their own page. */
	public static final ResourceKey<CreativeModeTab> SLATES_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "slates"));

	public static final CreativeModeTab SLATES = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			SLATES_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria_fantasy.slates"))
					.icon(() -> new ItemStack(ModItems.SLATE_TAB_ICON))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.SLATE_OF_FUTURE);
						output.accept(ModItems.SLATE_OF_RALLY);
						output.accept(ModItems.SLATE_OF_WAVE);
						output.accept(ModItems.SLATE_OF_DOUBLE_STAR);
						output.accept(ModItems.SLATE_OF_HANDSHAKE);
						output.accept(ModItems.SLATE_OF_OATH);
						output.accept(ModItems.SLATE_OF_BELIEF);
						output.accept(ModItems.SLATE_OF_ENTRANCE);
						output.accept(ModItems.SLATE_OF_COMPETITION);
						output.accept(ModItems.SLATE_OF_SEVERANCE);
						output.accept(ModItems.SLATE_OF_WEDGE);
						output.accept(ModItems.SLATE_OF_DEFENSE);
						output.accept(ModItems.SLATE_OF_DROUGHT);
						output.accept(ModItems.SLATE_OF_EXPLOITATION);
						output.accept(ModItems.SLATE_OF_UNITY);
						output.accept(ModItems.SLATE_OF_PEACE);
						output.accept(ModItems.SLATE_OF_DAYDREAM);
						output.accept(ModItems.SLATE_OF_REBELLION);
						output.accept(ModItems.SLATE_OF_JUNCTION);
						output.accept(ModItems.SLATE_OF_GRATITUDE);
						output.accept(ModItems.SLATE_OF_ARRIVAL);
						output.accept(ModItems.SLATE_OF_APPROXIMATION);
						output.accept(ModItems.SLATE_OF_MISCHIEF);
						output.accept(ModItems.SLATE_OF_NOURISHMENT);
						output.accept(ModItems.SLATE_OF_WISDOM);
						output.accept(ModItems.SLATE_OF_CHIVALRY);
						output.accept(ModItems.SLATE_OF_FATE);
						output.accept(ModItems.SLATE_OF_GAZE);
						output.accept(ModItems.SLATE_OF_GOODWILL);
						output.accept(ModItems.SLATE_OF_HOPE);
						output.accept(ModItems.SLATE_OF_CHEER);
						output.accept(ModItems.SLATE_OF_EXIT);
						output.accept(ModItems.SLATE_OF_PROGRESS);
						output.accept(ModItems.SLATE_OF_THREE_HEADS);
						output.accept(ModItems.SLATE_OF_PREPARATION);
						output.accept(ModItems.SLATE_OF_DISTRIBUTION);
						output.accept(ModItems.SLATE_OF_STACK);
						output.accept(ModItems.SLATE_OF_HONOR);
						output.accept(ModItems.SLATE_OF_PAST);
						output.accept(ModItems.SLATE_OF_ADVANCE);
						output.accept(ModItems.SLATE_OF_OFFERING);
						output.accept(ModItems.SLATE_OF_DESIRE);
						output.accept(ModItems.SLATE_OF_AGITATION);
						output.accept(ModItems.SLATE_OF_HARVEST);
						output.accept(ModItems.SLATE_OF_TRANSFER);
						output.accept(ModItems.SLATE_OF_JUSTICE);
						output.accept(ModItems.SLATE_OF_GATHERING);
						output.accept(ModItems.SLATE_OF_BANNER);
						output.accept(ModItems.SLATE_OF_SIMULTANEITY);
						output.accept(ModItems.SLATE_OF_BURN_THE_BOATS);
						output.accept(ModItems.SLATE_OF_FOUNDATION);
						output.accept(ModItems.SLATE_OF_AUTHORITY);
						output.accept(ModItems.SLATE_OF_COMPRESSION);
						output.accept(ModItems.SLATE_OF_MIRACLE);
						output.accept(ModItems.SLATE_OF_THORNS);
						output.accept(ModItems.SLATE_OF_RADIANCE);
						output.accept(ModItems.SLATE_OF_COURAGE);
						// 石板基础只由「耐心」一阶段发放；创造页留一件方便测试
						output.accept(ModItems.SLATE_BASE);
						// 天国不进掉落池：创造页留一件，只用于测试（正常游戏只能烧远古残骸碰运气）
						output.accept(ModItems.SLATE_OF_HEAVEN);
					})
					.build());

	/** Items tab: the enchant coin, its own icon. */
	public static final ResourceKey<CreativeModeTab> ITEMS_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "items"));

	public static final CreativeModeTab ITEMS = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			ITEMS_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria_fantasy.items"))
					.icon(() -> new ItemStack(ModItems.ENCHANT_COIN))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.ENCHANT_COIN);
						output.accept(ModItems.DICE);
						output.accept(ModItems.EXTRA_BACKPACK_ROW);
						output.accept(ModItems.ARTIFACT_CHEST);
						output.accept(ModItems.SLATE_CHEST);
						output.accept(ModItems.UPGRADE_CHEST);
						// 稀有宝箱正常只有 BOSS 掉；创造页留两件，方便测试奖池
						output.accept(ModItems.RARE_ARTIFACT_CHEST);
						output.accept(ModItems.RARE_SLATE_CHEST);
						output.accept(ModItems.ROOT_RETREAT);
						output.accept(ModItems.WISH_POOL);
					})
					.build());

	/** Potions tab: the potions are their own category, icon is the regeneration potion (fig 1). */
	public static final ResourceKey<CreativeModeTab> POTIONS_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "potions"));

	public static final CreativeModeTab POTIONS = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			POTIONS_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria_fantasy.potions"))
					.icon(() -> new ItemStack(ModItems.REGENERATION_POTION))
					.displayItems((parameters, output) -> ModItems.POTIONS.forEach(output::accept))
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
