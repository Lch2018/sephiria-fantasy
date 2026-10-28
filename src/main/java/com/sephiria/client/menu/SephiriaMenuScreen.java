package com.sephiria.client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * 移植版的通用容器界面。
 *
 * <p>26.3 的界面都带一套自绘面板与动画；1.21.1 这边先给每个菜单一个能用的统一外观：
 * 深色面板、四边描边、每个槽位画一格底，标题与「物品栏」标签交给原版。
 * 这样槽位点击、拖拽、Shift 移动等原版行为全部可用，后续再逐个换回专属绘制。
 */
public class SephiriaMenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	private static final int PANEL = 0xF0101014;
	private static final int BORDER = 0xFF4A4A52;
	private static final int SLOT = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;

	private final boolean playerInventory;

	public SephiriaMenuScreen(T menu, Inventory inventory, Component title, int width, int height,
			boolean playerInventory) {
		super(menu, inventory, title);
		this.imageWidth = width;
		this.imageHeight = height;
		this.playerInventory = playerInventory;

		if (playerInventory) {
			this.inventoryLabelY = height - 94;
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;

		graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL);
		graphics.fill(left, top, left + this.imageWidth, top + 1, BORDER);
		graphics.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, BORDER);
		graphics.fill(left, top, left + 1, top + this.imageHeight, BORDER);
		graphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, BORDER);

		for (Slot slot : this.menu.slots) {
			int x = left + slot.x;
			int y = top + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			graphics.fill(x, y, x + 16, y + 16, SLOT);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);

		if (this.playerInventory) {
			graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
					0xFFA0A0A0, false);
		}
	}
}
