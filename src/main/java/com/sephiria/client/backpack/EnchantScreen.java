package com.sephiria.client.backpack;

import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.UpgradeArtifactPayload;
import com.sephiria.slate.SlateItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器附魔面板：左边功能区（神器槽、附魔币槽、三个升级选项），右边是赛菲利亚背包格区与
 * 玩家背包——赛菲利亚背包里的神器不用先搬回原版背包，直接拖进神器槽就能升级。
 *
 * <p>选项在附魔币不够时置灰（数量够不够是本地先算一遍，服务端还会再判一次）；面板同样用
 * 纯色块画。背包格区的画法（石板朝向、等级文字、悬停提示）与赛菲利亚背包界面共用
 * {@link BackpackCellRenderer}，保证两边看起来一致。
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
		// 「物品栏」标签贴在右列玩家背包区的头上；左上角那行留给标题
		this.inventoryLabelX = EnchantMenu.FUNCTION_AREA_WIDTH + 8;
		this.inventoryLabelY = EnchantMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		// 附魔面板不是那三页之一，所以不标当前页——三个标签都可点
		SephiriaTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, null);
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

	/** 按 R 旋转鼠标下的石板（默认逆时针 90°）；写着「不可旋转」的石板不响应。与背包页同一套。 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		// 26.3 移除了 GLFW，键位常量改用 InputConstants（KeyEvent.key() 与
		// InputConstants.KEY_* 同一套键码，原版 InputConstants.getKey 也是这么取的）
		if (event.key() == InputConstants.KEY_R) {
			Slot hovered = hoveredBackpackSlot();

			if (hovered != null && hovered.getItem().getItem() instanceof SlateItem slate && slate.rotatable()) {
				ClientPlayNetworking.send(new RotateSlatePayload(this.menu.slots.indexOf(hovered)));
				return true;
			}
		}

		return super.keyPressed(event);
	}

	/** 每个选项能不能用：按「实际能升的级数」判断，够不着就置灰。 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		for (int index = 0; index < this.upgradeButtons.size(); index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			// 只按“附魔币够不够这次所需”置灰：差 2 级满 + 有 2 枚时，「5 级」也是可用的
			this.upgradeButtons.get(index).active = this.menu.affordableLevels(levels) > 0;
		}

		// 悬停在背包格子上时，把格子等级塞给提示框（神器词条按「自身等级+格子等级」显示）
		ArtifactTooltip.setSlotLevelHint(hoveredSlotLevel(mouseX, mouseY));
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		ArtifactTooltip.clearSlotLevelHint();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 功能区与背包区之间的分隔线（和背包界面左边的连击栏同一画法）
		int divider = left + EnchantMenu.FUNCTION_AREA_WIDTH;
		extractor.fill(divider - 2, top + 4, divider - 1, top + this.imageHeight - 4, BORDER_LIGHT);

		// 神器槽、附魔币槽与赛菲利亚背包格区的底；玩家背包区用原版自己的画法
		for (Slot slot : this.menu.slots) {
			int index = this.menu.slots.indexOf(slot);

			if (index > 1 && !this.menu.isBackpackSlot(slot)) {
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

		// extractLabels 收到的是面板内坐标（super 的「物品栏」就是这么画的），与
		// extractBackground 的绝对坐标不是一套——这里再加 leftPos/topPos 会把字画出面板
		Font font = this.font;
		int left = 8;
		extractor.text(font, Component.translatable("screen.sephiria.enchant.artifact"),
				left, EnchantMenu.ARTIFACT_SLOT_Y + 22, HINT_COLOR);
		extractor.text(font, Component.translatable("screen.sephiria.enchant.coins", coinText()),
				left, EnchantMenu.COIN_SLOT_Y + 22, HINT_COLOR);

		// 背包格区的列头，与左上角的标题同一行
		extractor.text(font, Component.translatable("screen.sephiria.backpack"),
				EnchantMenu.BACKPACK_SLOT_X, EnchantMenu.BACKPACK_SLOT_Y - 12, HINT_COLOR);
	}

	/** 背包格区里的显示与赛菲利亚背包界面一致：石板按朝向转、左上角画等级文字。 */
	@Override
	protected void extractSlot(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY) {
		ItemStack shown = slot.getItem();

		if (shown.getItem() instanceof SlateItem && SlateItem.rotationOf(shown) != 0) {
			BackpackCellRenderer.drawRotatedItem(extractor, slot, shown);
		} else {
			super.extractSlot(extractor, slot, mouseX, mouseY);
		}

		if (!this.menu.isBackpackSlot(slot)) {
			return;
		}

		BackpackCellRenderer.drawLevelText(extractor, this.font, slot, shown, this.menu.slotLevelOf(slot));
	}

	/** 鼠标下那个背包格子；不在任何背包格子上时返回 null。 */
	private Slot hoveredBackpackSlot() {
		for (Slot slot : this.menu.slots) {
			if (this.hoveredSlot == slot && this.menu.isBackpackSlot(slot)) {
				return slot;
			}
		}

		return null;
	}

	/** 鼠标下的背包格子的格子等级；不在背包格子上时返回 -1。 */
	private int hoveredSlotLevel(int mouseX, int mouseY) {
		for (Slot slot : this.menu.slots) {
			if (!this.menu.isBackpackSlot(slot)) {
				continue;
			}

			int x = this.leftPos + slot.x;
			int y = this.topPos + slot.y;

			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				return this.menu.slotLevelOf(slot);
			}
		}

		return -1;
	}

	/** 附魔币栏的枚数（黄色数字）。 */
	private Component coinText() {
		return Component.literal(String.valueOf(this.menu.coinCount())).withColor(COIN_COLOR);
	}
}
