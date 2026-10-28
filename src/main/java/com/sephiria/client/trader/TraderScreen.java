package com.sephiria.client.trader;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.OpenSlateFusionPayload;
import com.sephiria.trader.SephiriaTraderMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 商人界面：通用面板 + 左下角「石板合成」按钮。 */
public class TraderScreen extends SephiriaMenuScreen<SephiriaTraderMenu> {
	public TraderScreen(SephiriaTraderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SephiriaTraderMenu.IMAGE_WIDTH, SephiriaTraderMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.trader.slate_fusion"),
				button -> ClientPlayNetworking.send(OpenSlateFusionPayload.CHANNEL,
						new FriendlyByteBuf(Unpooled.buffer())))
				.bounds(this.leftPos + SephiriaTraderMenu.FUSION_BUTTON_X,
						this.topPos + SephiriaTraderMenu.fusionButtonY(),
						SephiriaTraderMenu.FUSION_BUTTON_WIDTH, SephiriaTraderMenu.FUSION_BUTTON_HEIGHT).build());
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);

		for (int index = 0; index < SephiriaTraderMenu.SLOTS; index++) {
			ItemStack stack = this.menu.getSlot(index).getItem();

			if (!stack.isEmpty()) {
				graphics.drawString(this.font, stack.getHoverName(),
						SephiriaTraderMenu.SLOT_X + index * 18, SephiriaTraderMenu.SLOT_Y + 18,
						0xFFD0D0D0, false);
			}
		}
	}
}
