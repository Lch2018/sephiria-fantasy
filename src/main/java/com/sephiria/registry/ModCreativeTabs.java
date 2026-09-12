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
					.displayItems((parameters, output) -> ModItems.BASE_WEAPONS
							.forEach(weapon -> output.accept(weapon.item())))
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
