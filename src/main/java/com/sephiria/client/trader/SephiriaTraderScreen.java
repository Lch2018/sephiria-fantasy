package com.sephiria.client.trader;

import com.sephiria.network.OpenSlateFusionPayload;
import com.sephiria.trader.SephiriaTraderMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 赛菲莉娅流浪商人的交易界面（自定义，和宝箱页同一套画法）：一排 9 格商品 + 玩家背包。
 *
 * <p>商品格只是展示，点哪一格服务端就把那件货免费塞进背包；卖掉的格子会变空，不会补货。
 */
public class SephiriaTraderScreen extends AbstractContainerScreen<SephiriaTraderMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;

	/** 「石板合成」按钮：这个玩家在这只商人身上用过之后就置灰。 */
	private Button fusionButton;

	public SephiriaTraderScreen(SephiriaTraderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SephiriaTraderMenu.IMAGE_WIDTH, SephiriaTraderMenu.imageHeight());
		this.inventoryLabelY = SephiriaTraderMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		// 面板左下角的「石板合成」：容器菜单必须由服务端打开，所以这里只发一个请求包
		this.fusionButton = Button.builder(
						Component.translatable("screen.sephiria_fantasy.trader.slate_fusion"),
						pressed -> ClientPlayNetworking.send(OpenSlateFusionPayload.INSTANCE))
				.bounds(this.leftPos + SephiriaTraderMenu.FUSION_BUTTON_X,
						this.topPos + SephiriaTraderMenu.fusionButtonY(),
						SephiriaTraderMenu.FUSION_BUTTON_WIDTH, SephiriaTraderMenu.FUSION_BUTTON_HEIGHT)
				.build();
		addRenderableWidget(this.fusionButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		if (this.fusionButton != null) {
			// 每个商人每个玩家一次：服务端把「用没用过」同步在数据槽里，用过就灰掉
			this.fusionButton.active = !this.menu.fusionUsed();
		}

		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 与背包页 / 宝箱页同一套：虚化 + 25% 黑底纹
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		for (Slot slot : this.menu.slots) {
			if (this.menu.slots.indexOf(slot) >= SephiriaTraderMenu.SLOTS) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;
			extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}
	}
}
