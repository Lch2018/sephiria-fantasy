package com.sephiria.client.backpack;

import com.sephiria.backpack.ChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 宝箱页面：上面 5 格随机奖励（点一格直接拿走），下面是玩家背包。
 *
 * <p>没有标签栏——它是一次性的选择界面，选完就关（服务端那边也是这么处理的）。
 */
public class ChestScreen extends AbstractContainerScreen<ChestMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;
	private static final int HINT_COLOR = 0xFF9A9AA2;

	public ChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ChestMenu.IMAGE_WIDTH, ChestMenu.imageHeight());
		this.inventoryLabelY = ChestMenu.playerRowsY() - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 与背包页同一套：容器界面原版只压暗不虚化，这里补上「属性 / 技能页」那套（虚化 + 25% 黑底纹），
		// 虚化会把 HUD 与物品栏一起糊掉（它们在界面之前抽取）
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		for (Slot slot : this.menu.slots) {
			int index = this.menu.slots.indexOf(slot);

			if (index >= ChestMenu.REWARD_SLOTS) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;
			extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		super.extractLabels(extractor, mouseX, mouseY);
		extractor.text(this.font, Component.translatable("screen.sephiria.chest.hint"),
				this.leftPos + 8, this.topPos + ChestMenu.SLOT_Y + 24, HINT_COLOR);
	}
}
