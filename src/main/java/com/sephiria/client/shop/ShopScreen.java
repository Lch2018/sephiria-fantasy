package com.sephiria.client.shop;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.shop.ShopMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.literal("叶子: " + (int) com.sephiria.client.ClientStats.leaves()),
				this.imageWidth - 70, 6, 0xFFFFD060, false);
		super.renderLabels(graphics, mouseX, mouseY);

		for (int index = 0; index < com.sephiria.shop.ShopStock.SLOT_COUNT; index++) {
			ItemStack stack = this.menu.shopStack(index);

			if (stack.isEmpty()) {
				continue;
			}

			int price = com.sephiria.shop.ShopPrices.buy(stack);
			int remaining = this.menu.remaining(index);
			int x = this.menu.getSlot(index).x;
			int y = this.menu.getSlot(index).y + 17;
			graphics.pose().pushPose();
			graphics.pose().translate(x, y, 0.0F);
			graphics.pose().scale(0.5F, 0.5F, 1.0F);
			graphics.drawString(this.font, Component.literal(String.valueOf(price)), 0, 0,
					price > 0 ? 0xFF55FF55 : 0xFFFF5555, false);
			graphics.pose().popPose();
		}
	}
}
