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

import java.util.ArrayList;
import java.util.List;

/**
 * 神器附魔面板：上面一个神器槽、下面一个附魔币槽，右边三个升级选项（1 / 2 / 5 级）。
 *
 * <p>选项在附魔币不够时置灰（数量够不够是本地先算一遍，服务端还会再判一次）；
 * 面板同样用纯色块画。
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

	private final List<Button> upgradeButtons = new ArrayList<>();

	public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight());
		this.inventoryLabelY = EnchantMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		SephiriaTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, SephiriaTab.ATTRIBUTES);
		this.upgradeButtons.clear();

		for (int index = 0; index < EnchantMenu.UPGRADE_STEPS.length; index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			Button button = Button.builder(
							Component.translatable("screen.sephiria.enchant.upgrade_n", levels),
							pressed -> ClientPlayNetworking.send(new UpgradeArtifactPayload(levels)))
					.bounds(this.leftPos + EnchantMenu.BUTTON_X,
							this.topPos + EnchantMenu.BUTTON_Y + index * EnchantMenu.BUTTON_SPACING,
							EnchantMenu.BUTTON_WIDTH, EnchantMenu.BUTTON_HEIGHT)
					.build();
			this.upgradeButtons.add(button);
			addRenderableWidget(button);
		}
	}

	/** 每个选项能不能用：按「实际能升的级数」判断，够不着就置灰。 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		for (int index = 0; index < this.upgradeButtons.size(); index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			// 只按“附魔币够不够这次所需”置灰：差 2 级满 + 有 2 枚时，「5 级」也是可用的
			this.upgradeButtons.get(index).active = this.menu.affordableLevels(levels) > 0;
		}

		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 两个功能槽的底
		for (Slot slot : this.menu.slots) {
			int index = this.menu.slots.indexOf(slot);

			if (index > 1) {
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
		int y = this.topPos + EnchantMenu.COIN_SLOT_Y + 22;
		extractor.text(font, Component.translatable("screen.sephiria.enchant.artifact"), left, y, HINT_COLOR);
		extractor.text(font, Component.translatable("screen.sephiria.enchant.coins", coinText()),
				left, y + 11, HINT_COLOR);
	}

	/** 附魔币栏的枚数（黄色数字）。 */
	private Component coinText() {
		return Component.literal(String.valueOf(this.menu.coinCount())).withColor(COIN_COLOR);
	}
}
