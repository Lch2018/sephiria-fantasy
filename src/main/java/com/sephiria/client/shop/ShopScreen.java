package com.sephiria.client.shop;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.shop.ShopMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import io.netty.buffer.Unpooled;

/** 商店界面：通用面板 + 「刷新」「出售」两个按钮。 */
public class ShopScreen extends SephiriaMenuScreen<ShopMenu> {
	public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ShopMenu.IMAGE_WIDTH, ShopMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();

		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.shop.refresh_button"),
				button -> sendEmpty(RefreshShopPayload.CHANNEL))
				.bounds(this.leftPos + ShopMenu.REFRESH_BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.shop.sell_button"),
				button -> sendEmpty(SellItemPayload.CHANNEL))
				.bounds(this.leftPos + ShopMenu.BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT).build());
	}

	private static void sendEmpty(net.minecraft.resources.ResourceLocation channel) {
		ClientPlayNetworking.send(channel, new FriendlyByteBuf(Unpooled.buffer()));
	}
}
