package com.sephiria.client.beacon;

import com.sephiria.beacon.BeaconLoot;
import com.sephiria.beacon.SephiriaBeaconMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅信标界面：一个格子 + 玩家背包，格子边框用里面物品的品质色。
 */
public class SephiriaBeaconScreen extends AbstractContainerScreen<SephiriaBeaconMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;

	public SephiriaBeaconScreen(SephiriaBeaconMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = SephiriaBeaconMenu.IMAGE_WIDTH;
		this.imageHeight = SephiriaBeaconMenu.IMAGE_HEIGHT;
		this.inventoryLabelY = SephiriaBeaconMenu.PLAYER_ROWS_Y - 11;
	}

	@Override
	protected void renderBg(GuiGraphics extractor, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		int x = left + SephiriaBeaconMenu.SLOT_X;
		int y = top + SephiriaBeaconMenu.SLOT_Y;
		extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
		extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);

		ItemStack held = this.menu.heldItem();

		if (!held.isEmpty()) {
			// 格子外框 = 光柱颜色，让玩家放进去之前就能看到这次会是什么颜色（四条边，不盖住格子）
			int color = 0xFF000000 | BeaconLoot.beamColor(held);
			extractor.fill(x - 2, y - 2, x + 18, y - 1, color);
			extractor.fill(x - 2, y + 17, x + 18, y + 18, color);
			extractor.fill(x - 2, y - 1, x - 1, y + 17, color);
			extractor.fill(x + 17, y - 1, x + 18, y + 17, color);
		}
	}
}
