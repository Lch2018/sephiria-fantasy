package com.sephiria.client.shop;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.shop.ShopMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 商店：刷新/出售按钮；货架格点击购买沿用原版 clicked。 */
public class ShopScreen extends SephiriaMenuScreen<ShopMenu> {
	public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ShopMenu.IMAGE_WIDTH, ShopMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.shop.refresh_button"),
				button -> ClientPlayNetworking.send(RefreshShopPayload.INSTANCE))
				.bounds(this.leftPos + ShopMenu.REFRESH_BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.shop.sell_button"),
				button -> ClientPlayNetworking.send(SellItemPayload.INSTANCE))
				.bounds(this.leftPos + ShopMenu.BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT).build());
	}
}
