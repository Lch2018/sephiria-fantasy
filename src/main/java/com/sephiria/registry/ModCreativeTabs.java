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
					.title(Component.translatable("itemGroup.sephiria.weapons"))
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
					.title(Component.translatable("itemGroup.sephiria.artifacts"))
					.icon(() -> new ItemStack(ModItems.ARTIFACT_TAB_ICON))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.CHARM_OF_STRENGTH);
						output.accept(ModItems.WARRIORS_PROOF);
						output.accept(ModItems.SLATE_OF_FUTURE);
					})
					.build());

	/** 璧涜彶鍒╀簹閬撳叿鏍囩椤碉細鐩墠鍙湁绁炲櫒闄勯瓟甯侊紝鍥炬爣灏辩敤瀹冭嚜宸便€?*/
	public static final ResourceKey<CreativeModeTab> ITEMS_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "items"));

	public static final CreativeModeTab ITEMS = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			ITEMS_KEY,
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.sephiria.items"))
					.icon(() -> new ItemStack(ModItems.ENCHANT_COIN))
					.displayItems((parameters, output) -> output.accept(ModItems.ENCHANT_COIN))
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
