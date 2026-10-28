package com.sephiria.client;

import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.backpack.ChestMenu;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.registry.ModMenus;
import com.sephiria.shop.ShopMenu;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.trader.SephiriaTraderMenu;
import com.sephiria.wishpool.WishPoolMenu;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * 1.20.1 客户端入口（核心版）。
 *
 * <p>目前只注册容器界面：统一用 {@link SephiriaMenuScreen} 画一个可用面板。
 * HUD、自绘界面、网络同步与 GeckoLib 动画还没有移植。
 *
 * <p>1.20.1 的 {@code MenuScreens.register} 是包私有，靠访问拓宽打开
 * （见 {@code sephiria_fantasy.accesswidener}）。
 */
public final class SephiriaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.<ArtifactBackpackMenu, SephiriaMenuScreen<ArtifactBackpackMenu>>register(
				ModMenus.ARTIFACT_BACKPACK,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						ArtifactBackpackMenu.IMAGE_WIDTH, ArtifactBackpackMenu.imageHeight(BackpackWindow.ROWS),
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
		MenuScreens.<com.sephiria.beacon.SephiriaBeaconMenu, SephiriaMenuScreen<com.sephiria.beacon.SephiriaBeaconMenu>>register(
				ModMenus.SEPHIRIA_BEACON,
				(menu, inventory, title) -> new SephiriaMenuScreen<>(menu, inventory, title,
						com.sephiria.beacon.SephiriaBeaconMenu.IMAGE_WIDTH,
						com.sephiria.beacon.SephiriaBeaconMenu.IMAGE_HEIGHT, true));
	}
}
