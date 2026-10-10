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
						output.accept(ModItems.CROSSBOW_BOLT);
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
						output.accept(ModItems.ARTIFACT_CHEST);
						output.accept(ModItems.SLATE_CHEST);
						output.accept(ModItems.UPGRADE_CHEST);
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
