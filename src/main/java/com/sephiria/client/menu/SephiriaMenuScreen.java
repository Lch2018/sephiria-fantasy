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
 *
 * <p>面板与格底的画法做成了 protected 的小工具（{@link #drawPanel}、{@link #drawSlotBackground}），
 * 专属界面覆写 {@code renderBg} 时直接复用，配色与描边才不会各写一套。
 */
public class SephiriaMenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected static final int PANEL = 0xF0101014;
	protected static final int BORDER = 0xFF4A4A52;
	protected static final int SLOT = 0xFF2A2A30;
	protected static final int SLOT_EDGE = 0xFF15151A;
	/** 说明文字（提示、列头）的颜色。 */
	protected static final int HINT = 0xFF9A9AA2;

	/** 所有赛菲莉娅界面共用的滚动条：2 px 宽，轨道压暗、滑块提亮。 */
	protected static final int SCROLLBAR_WIDTH = 2;
	protected static final int SCROLLBAR_TRACK = 0xFF15151A;
	protected static final int SCROLLBAR_THUMB = 0xFF7A7A88;

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
		drawPanel(graphics);

		for (Slot slot : this.menu.slots) {
			drawSlotBackground(graphics, slot);
		}
	}

	/** 面板底色与四边描边（界面自绘时先调它，再往上画内容）。 */
	protected void drawPanel(GuiGraphics graphics) {
		int left = this.leftPos;
		int top = this.topPos;

		graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL);
		graphics.fill(left, top, left + this.imageWidth, top + 1, BORDER);
		graphics.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, BORDER);
		graphics.fill(left, top, left + 1, top + this.imageHeight, BORDER);
		graphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, BORDER);
	}

	/** 一格槽位的底（外描边 + 内格）：槽位坐标是面板内坐标，这里补 leftPos / topPos。 */
	protected void drawSlotBackground(GuiGraphics graphics, Slot slot) {
		int x = this.leftPos + slot.x;
		int y = this.topPos + slot.y;
		graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
		graphics.fill(x, y, x + 16, y + 16, SLOT);
	}

	/** 竖直滚动条：{@code thumbY} 与 {@code thumb} 由调用方按内容算好。 */
	protected static void drawScrollbar(GuiGraphics graphics, int x, int top, int bottom, int thumbY, int thumb) {
		graphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK);
		graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, SCROLLBAR_THUMB);
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
