package com.sephiria.client.backpack;

import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.UpgradeArtifactPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 神器附魔面板：左边一个待升级神器的槽，点「升级」消耗一枚神器附魔币让神器 +1 级。
 *
 * <p>面板同样用纯色块画；左下角显示背包里现有的附魔币数量。
 */
public class EnchantScreen extends AbstractContainerScreen<EnchantMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;
	private static final int HINT_COLOR = 0xFF9A9AA2;
	private static final int COIN_COLOR = 0xFFFFD24A;
	private static final int TAB_HEIGHT = 20;

	public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight());
		this.inventoryLabelY = EnchantMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		SephiriaTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, SephiriaTab.ATTRIBUTES);
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria.enchant.upgrade"),
						button -> ClientPlayNetworking.send(UpgradeArtifactPayload.INSTANCE))
				.bounds(this.leftPos + EnchantMenu.BUTTON_X, this.topPos + EnchantMenu.BUTTON_Y,
						EnchantMenu.BUTTON_WIDTH, EnchantMenu.BUTTON_HEIGHT)
				.build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 神器槽的底
		for (Slot slot : this.menu.slots) {
			if (slot.index != 0 || slot.container == null) {
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

		Font font = this.font;
		int left = this.leftPos + 8;
		// 槽下方的提示与附魔币数量
		extractor.text(font, Component.translatable("screen.sephiria.enchant.hint"), left, this.topPos + EnchantMenu.SLOT_Y + 24,
				HINT_COLOR);
		extractor.text(font, Component.translatable("screen.sephiria.enchant.coins",
						Component.literal(String.valueOf(coinCount())).withColor(COIN_COLOR)),
				left, this.topPos + EnchantMenu.SLOT_Y + 36, HINT_COLOR);
	}

	private int coinCount() {
		return this.minecraft == null || this.minecraft.player == null
				? 0
				: EnchantMenu.coinCount(this.minecraft.player);
	}
}
