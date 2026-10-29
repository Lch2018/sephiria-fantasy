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
 * <p>1.20.1 的界面都带一套自绘面板：深色面板、四边描边、每个槽位画一格底，
 * 标题与「物品栏」标签交给原版。这样槽位点击、拖拽、Shift 移动等原版行为全部可用。
 *
 * <p>面板与格底的画法做成了 protected 的小工具（{@link #drawPanel}、{@link #drawSlotBackground}），
 * 专属界面覆写 {@code renderBg} 时直接复用，配色与描边才不会各写一套。
 *
 * <p>与 1.21.1 的差别（1.20.1 的原版实现不同，所以这里补了三件事）：
 * <ul>
 *   <li>{@code Screen.render} 不铺背景——容器界面要自己调 {@link #renderBackground}；</li>
 *   <li>{@code AbstractContainerScreen.render} 不画悬停提示——要自己调 {@link #renderTooltip}；</li>
 *   <li>{@code renderSlot} 是 private，没法重写——专属界面在 {@link #renderSlotOverlays} 里
 *       物品画完之后补画（旋转石板、格子等级），提示框仍压在最上面。</li>
 * </ul>
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

	/** 背景铺底 → 原版画面板/按钮/物品 → 补画格子内容 → 悬停提示（顺序与 1.20.1 原版容器界面一致）。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.21.1 由 Screen.render 代劳；1.20.1 的 Screen.render 只画按钮，背景得自己铺
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		renderSlotOverlays(graphics);

		// 1.20.1 的容器界面要自己画悬停提示，原版不会代劳；
		// 放在补画之后，提示框才能压在格子等级/旋转图标的上面
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);

		for (Slot slot : this.menu.slots) {
			drawSlotBackground(graphics, slot);
		}
	}

	/** 物品画完之后、悬停提示之前的补画口（子类用来补旋转石板与格子等级文字）。 */
	protected void renderSlotOverlays(GuiGraphics graphics) {
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

	/**
	 * 重画一格的底（把原版画好的没旋转的图标擦掉）。
	 *
	 * <p>1.20.1 的 {@code renderSlot} 是 private：想把石板图标转过来，只能等原版画完后
	 * 用格底盖掉再重画。悬停高亮要等重画过物品之后再补，见 {@link #restoreSlotHighlight}。
	 */
	protected void redrawSlot(GuiGraphics graphics, Slot slot) {
		drawSlotBackground(graphics, slot);
	}

	/** 原版是在物品画完之后才叠悬停高亮；重画过图标的格子按同样顺序补一次。 */
	protected void restoreSlotHighlight(GuiGraphics graphics, Slot slot) {
		if (slot == this.hoveredSlot) {
			AbstractContainerScreen.renderSlotHighlight(graphics, this.leftPos + slot.x, this.topPos + slot.y, 0);
		}
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
