package com.sephiria.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.difficulty.RootRetreatBlock;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.network.DashPayload;
import com.sephiria.network.ReloadPayload;
import com.sephiria.registry.ModMenus;
import com.sephiria.shop.ShopMenu;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.trader.SephiriaTraderMenu;
import com.sephiria.wishpool.WishPoolBlock;
import com.sephiria.wishpool.WishPoolMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;

/**
 * 1.21.1 客户端入口（核心版）。
 *
 * <p>目前只做两件不依赖 26.3 渲染管线的事：容器界面注册（统一用 {@link SephiriaMenuScreen}
 * 画一个可用面板）与冲刺 / 装填两个按键。26.3 的自绘 HUD、ToroHealth、ModMenu、
 * GeckoLib 刀动画还没有移植，别把这一版当完整客户端。
 */
public final class SephiriaClient implements ClientModInitializer {
	/** 1.21.1 的按键分类仍是一个字符串。 */
	private static final String CATEGORY = "key.categories.gameplay";

	private static final KeyMapping DASH_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.dash", InputConstants.Type.MOUSE, 4, CATEGORY));

	private static final KeyMapping RELOAD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.sephiria_fantasy.reload", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY));

	@Override
	public void onInitializeClient() {
		MenuScreens.<ArtifactBackpackMenu, SephiriaMenuScreen<ArtifactBackpackMenu>>register(ModMenus.ARTIFACT_BACKPACK,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title, ArtifactBackpackMenu.IMAGE_WIDTH, ArtifactBackpackMenu.imageHeight(BackpackWindow.ROWS),
						true));
		MenuScreens.<EnchantMenu, SephiriaMenuScreen<EnchantMenu>>register(ModMenus.ARTIFACT_ENCHANT,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight(), true));
		MenuScreens.<ChestMenu, SephiriaMenuScreen<ChestMenu>>register(ModMenus.CHEST,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						ChestMenu.IMAGE_WIDTH, ChestMenu.imageHeight(), true));
		MenuScreens.<ShopMenu, SephiriaMenuScreen<ShopMenu>>register(ModMenus.SHOP,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						ShopMenu.IMAGE_WIDTH, ShopMenu.imageHeight(), true));
		MenuScreens.<SlateFusionMenu, SephiriaMenuScreen<SlateFusionMenu>>register(ModMenus.SLATE_FUSION,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						SlateFusionMenu.IMAGE_WIDTH, SlateFusionMenu.imageHeight(), true));
		MenuScreens.<SephiriaTraderMenu, SephiriaMenuScreen<SephiriaTraderMenu>>register(ModMenus.TRADER,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						SephiriaTraderMenu.IMAGE_WIDTH, SephiriaTraderMenu.imageHeight(), true));
		MenuScreens.<RootRetreatMenu, SephiriaMenuScreen<RootRetreatMenu>>register(ModMenus.ROOT_RETREAT,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						RootRetreatMenu.IMAGE_WIDTH, 220, false));
		MenuScreens.<WishPoolMenu, SephiriaMenuScreen<WishPoolMenu>>register(ModMenus.WISH_POOL,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						WishPoolMenu.IMAGE_WIDTH, WishPoolMenu.IMAGE_HEIGHT, false));
		MenuScreens.<com.sephiria.beacon.SephiriaBeaconMenu, SephiriaMenuScreen<com.sephiria.beacon.SephiriaBeaconMenu>>register(ModMenus.SEPHIRIA_BEACON,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						com.sephiria.beacon.SephiriaBeaconMenu.IMAGE_WIDTH,
						com.sephiria.beacon.SephiriaBeaconMenu.IMAGE_HEIGHT, true));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (DASH_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(DashPayload.INSTANCE);
				}
			}

			while (RELOAD_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(ReloadPayload.INSTANCE);
				}
			}
		});

		// 引用一次，保证标题常量被初始化（也避免 IDE 把 import 当成未使用）
		Component.empty();
		RootRetreatBlock.title();
		WishPoolBlock.title();
	}
}
