package com.sephiria.client.backpack;

import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.ScrollBackpackPayload;
import com.sephiria.network.UpgradeArtifactPayload;
import com.sephiria.slate.SlateItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 神器附魔面板（移植 26.3 的样子）：左边功能区（神器槽、附魔币槽、三个升级选项），
 * 右边是赛菲莉娅背包格区与玩家背包——赛菲莉娅背包里的神器不用先搬回原版背包，
 * 直接拖进神器槽就能升级。
 *
 * <p>选项在附魔币不够时置灰（数量够不够是本地先算一遍，服务端还会再判一次）；
 * 背包格区的画法（石板朝向、等级文字、悬停提示）与背包界面共用 {@link BackpackCellRenderer}，
 * 保证两边看起来一致。
 */
public class EnchantScreen extends SephiriaMenuScreen<EnchantMenu> {
	private static final int COIN_COLOR = 0xFFFFD24A;

	private final List<Button> upgradeButtons = new ArrayList<>();

	public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight(), true);
		// 「物品栏」标签贴在右列玩家背包区的头上；左上角那行留给标题
		this.inventoryLabelX = EnchantMenu.FUNCTION_AREA_WIDTH + 8;
		this.inventoryLabelY = EnchantMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		this.upgradeButtons.clear();

		for (int index = 0; index < EnchantMenu.UPGRADE_STEPS.length; index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			Button button = Button.builder(
					Component.translatable("screen.sephiria_fantasy.enchant.upgrade_n", levels),
					pressed -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
						UpgradeArtifactPayload.write(buffer, new UpgradeArtifactPayload(levels));
						ClientPlayNetworking.send(UpgradeArtifactPayload.CHANNEL, buffer);
					})
					.bounds(this.leftPos + EnchantMenu.BUTTON_X,
							this.topPos + EnchantMenu.BUTTON_Y + index * EnchantMenu.BUTTON_SPACING,
							EnchantMenu.BUTTON_WIDTH, EnchantMenu.BUTTON_HEIGHT).build();
			this.upgradeButtons.add(button);
			this.addRenderableWidget(button);
		}
	}

	/**
	 * 面板自绘：分隔线、神器槽 / 附魔币槽 / 背包格区的底、背包格区滚动条；
	 * 玩家背包区沿用通用界面的画法。
	 */
	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);

		// 功能区与背包区之间的分隔线（和背包界面左边的连击栏同一画法）
		int divider = this.leftPos + EnchantMenu.FUNCTION_AREA_WIDTH;
		graphics.fill(divider - 2, this.topPos + 4, divider - 1, this.topPos + this.imageHeight - 4, BORDER);

		for (Slot slot : this.menu.slots) {
			// 只有真正存在的背包格子才画底（末尾多出来的那几个位置不是格子）
			if (this.menu.isBackpackSlot(slot) && !this.menu.backpackSlotExists(slot)) {
				continue;
			}

			drawSlotBackground(graphics, slot);
		}

		renderBackpackScrollbar(graphics);
	}

	/** 每个选项能不能用：按「实际能升的级数」判断，够不着就置灰。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		for (int index = 0; index < this.upgradeButtons.size(); index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			// 只按「附魔币够不够这次所需」置灰：差 2 级满 + 有 2 枚时，「5 级」也是可用的
			this.upgradeButtons.get(index).active = this.menu.affordableLevels(levels) > 0;
		}

		// 悬停在背包格子上时，把格子等级塞给提示框（神器词条按「自身等级+格子等级」显示）
		ArtifactTooltip.setSlotLevelHint(hoveredSlotLevel(mouseX, mouseY));
		super.render(graphics, mouseX, mouseY, partialTick);
		ArtifactTooltip.clearSlotLevelHint();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);

		// renderLabels 收到的是面板内坐标（super 的「物品栏」就是这么画的），
		// 与 renderBg 的绝对坐标不是一套——这里不要加 leftPos / topPos
		graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.enchant.artifact"),
				8, EnchantMenu.ARTIFACT_SLOT_Y + 22, HINT, false);
		graphics.drawString(this.font,
				Component.translatable("screen.sephiria_fantasy.enchant.coins", coinText()),
				8, EnchantMenu.COIN_SLOT_Y + 22, HINT, false);

		// 背包格区的列头，与左上角的标题同一行
		graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.backpack"),
				EnchantMenu.BACKPACK_SLOT_X, EnchantMenu.BACKPACK_SLOT_Y - 12, HINT, false);
	}

	/** 背包格区里的显示与背包界面一致：石板按朝向转、左上角画等级文字。 */
	@Override
	protected void renderSlotOverlays(GuiGraphics graphics) {
		for (Slot slot : this.menu.slots) {
			if (this.menu.isBackpackSlot(slot) && !this.menu.backpackSlotExists(slot)) {
				continue;
			}

			ItemStack shown = slot.getItem();
			int x = this.leftPos + slot.x;
			int y = this.topPos + slot.y;

			if (shown.getItem() instanceof SlateItem && SlateItem.rotationOf(shown) != 0) {
				redrawSlot(graphics, slot);
				BackpackCellRenderer.drawRotatedItem(graphics, this.font, x, y, shown);
				restoreSlotHighlight(graphics, slot);
			}

			if (this.menu.isBackpackSlot(slot)) {
				BackpackCellRenderer.drawLevelText(graphics, this.font, x, y, shown, this.menu.slotLevelOf(slot));
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (delta != 0.0D) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			ScrollBackpackPayload.write(buffer, new ScrollBackpackPayload(delta > 0.0D ? -1 : 1));
			ClientPlayNetworking.send(ScrollBackpackPayload.CHANNEL, buffer);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			RotateSlatePayload.write(buffer, new RotateSlatePayload(hovered.index));
			ClientPlayNetworking.send(RotateSlatePayload.CHANNEL, buffer);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** 背包格区右侧的滚动条：只有背包比一屏长时才画（与背包页那条同一配色）。 */
	private void renderBackpackScrollbar(GuiGraphics graphics) {
		int maxRow = this.menu.maxScrollRow();

		if (maxRow <= 0) {
			return;
		}

		int x = this.leftPos + EnchantMenu.BACKPACK_SLOT_X + ArtifactBackpack.WIDTH * 18 + 3;
		int top = this.topPos + EnchantMenu.BACKPACK_SLOT_Y - 1;
		int bottom = top + BackpackWindow.ROWS * 18 + 2;
		int track = bottom - top;
		int totalRows = maxRow + BackpackWindow.ROWS;
		int thumb = Math.max(SCROLLBAR_WIDTH * 4, track * BackpackWindow.ROWS / totalRows);
		int thumbY = top + (track - thumb) * this.menu.scrollRow() / maxRow;

		drawScrollbar(graphics, x, top, bottom, thumbY, thumb);
	}

	/** 鼠标下的背包格子的格子等级；不在背包格子上时返回 -1。 */
	private int hoveredSlotLevel(int mouseX, int mouseY) {
		for (Slot slot : this.menu.slots) {
			if (!this.menu.isBackpackSlot(slot) || !this.menu.backpackSlotExists(slot)) {
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
		// 1.20.1 的 MutableComponent 没有 withColor(int)，用 withStyle 改颜色
		return Component.literal(String.valueOf(this.menu.coinCount()))
				.withStyle(style -> style.withColor(COIN_COLOR));
	}
}
