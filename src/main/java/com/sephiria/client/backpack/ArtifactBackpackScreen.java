package com.sephiria.client.backpack;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.ScrollBackpackPayload;
import com.sephiria.slate.SlateItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 赛菲莉娅背包的界面（移植 26.3 的样子）。
 *
 * <p>左边是连击栏（连击图标 / 名字 / 已到档位），右边是背包格区与玩家背包。面板不贴图，
 * 直接用纯色块画，这样背包扩容时界面尺寸不变、也不用再切图。
 *
 * <p>格子里的显示：神器显示「实际等级/上限」（按与上限的关系上色），没有神器时显示格子等级
 * （石板加出来的，可以是负数）——神器放上去会盖住格子等级，和 26.3 一致。
 *
 * <p>交互保持移植版原来的样子：滚轮翻背包的页、悬停石板按 R 旋转；连击栏自己也能滚，
 * 鼠标停在连击栏上时滚轮先滚它（装得下就不拦，避免误滚背包）。
 */
public class ArtifactBackpackScreen extends SephiriaMenuScreen<ArtifactBackpackMenu> {
	private static final int HEADER_COLOR = 0xFFFFD24A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	/** 连击栏一行的高度、列表上沿与图标尺寸。 */
	private static final int COMBO_ROW_HEIGHT = 26;
	private static final int COMBO_TOP = 30;
	private static final int COMBO_ICON = 16;
	/** 连击列表下方的留白：裁剪框与滚动条都按它收边。 */
	private static final int COMBO_BOTTOM_PAD = 4;

	/** 连击列表滚到第几行（0 = 从头开始）：装不下时才用得上，滚轮一次一行。 */
	private int comboScrollRow;

	public ArtifactBackpackScreen(ArtifactBackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ArtifactBackpackMenu.IMAGE_WIDTH,
				ArtifactBackpackMenu.imageHeight(BackpackWindow.ROWS), true);
	}

	/**
	 * 面板自绘：背包格区只画真正存在的格子（背包按「一次 +1 格」长，末尾那几个位置不是格子），
	 * 右侧画翻页滚动条，左侧画连击栏。
	 */
	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);

		// 连击栏与容器区之间的分隔线（26.3 的背包页同款）
		int divider = this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH;
		graphics.fill(divider - 2, this.topPos + 4, divider - 1, this.topPos + this.imageHeight - 4, BORDER);

		for (Slot slot : this.menu.slots) {
			if (this.menu.isBackpackSlot(slot) && !this.menu.backpackSlotExists(slot)) {
				continue;
			}

			drawSlotBackground(graphics, slot);
		}

		renderBackpackScrollbar(graphics);
		renderComboPanel(graphics);
	}

	/** 背包页不画标题与「物品栏」标签——左上角留给连击栏的列头。 */
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
	}

	/** 格子里的等级文字与旋转石板的图标（与附魔界面共用 {@link BackpackCellRenderer}）。 */
	@Override
	protected void renderSlot(GuiGraphics graphics, Slot slot) {
		// 末尾那些不是格子的位置什么都不画（连悬停高亮都不该有）
		if (this.menu.isBackpackSlot(slot) && !this.menu.backpackSlotExists(slot)) {
			return;
		}

		ItemStack shown = slot.getItem();

		if (shown.getItem() instanceof SlateItem && SlateItem.rotationOf(shown) != 0) {
			// 石板：按朝向把图标一起转过来（绕着格子中心转），这样一眼能看出它朝哪边
			BackpackCellRenderer.drawRotatedItem(graphics, slot, shown);
		} else {
			super.renderSlot(graphics, slot);
		}

		if (!this.menu.isBackpackSlot(slot)) {
			return;
		}

		BackpackCellRenderer.drawLevelText(graphics, this.font, slot, shown, this.menu.slotLevelOf(slot));
	}

	/**
	 * 渲染前把鼠标下格子的等级塞给提示框（神器词条按「自身等级 + 格子等级」显示），
	 * 渲染完清掉；连击浮窗最后画，免得被格子与物品盖住。
	 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ArtifactTooltip.setSlotLevelHint(hoveredSlotLevel(mouseX, mouseY));
		super.render(graphics, mouseX, mouseY, partialTick);
		ArtifactTooltip.clearSlotLevelHint();

		ArtifactCombo hovered = hoveredCombo(mouseX, mouseY);

		if (hovered != null) {
			renderComboTooltip(graphics, hovered, this.menu.comboLevel(hovered), mouseX, mouseY);
		}
	}

	/** 连击栏可视区的上边（绝对 y）。 */
	private int comboViewTop() {
		return this.topPos + COMBO_TOP;
	}

	/** 可视区的下边：面板底部按 {@link #COMBO_BOTTOM_PAD} 收边。 */
	private int comboViewBottom() {
		return this.topPos + this.imageHeight - COMBO_BOTTOM_PAD;
	}

	/** 一屏能完整显示几行连击：按面板高度算，别写死——背包扩容时面板会跟着变高。 */
	private int visibleComboRows() {
		return Math.max(1, (comboViewBottom() - comboViewTop()) / COMBO_ROW_HEIGHT);
	}

	/**
	 * 当前生效的连击（等级 &gt; 0），按枚举顺序。
	 *
	 * <p>档位由服务端按**整只背包**算好同步过来（菜单的数据槽）：可视区之外的神器客户端收不到，
	 * 自己拿可视窗口算的话，一滚轮面板上的档位就会少算。
	 */
	private List<Map.Entry<ArtifactCombo, Integer>> activeCombos() {
		List<Map.Entry<ArtifactCombo, Integer>> active = new ArrayList<>();

		for (ArtifactCombo combo : ArtifactCombo.values()) {
			int level = this.menu.comboLevel(combo);

			if (level > 0) {
				active.add(Map.entry(combo, level));
			}
		}

		return active;
	}

	/** 最多能往下滚几行（内容装得下时是 0）。 */
	private int maxComboScrollRow(int activeRows) {
		return Math.max(0, activeRows - visibleComboRows());
	}

	/** 鼠标是不是在左侧连击栏这一列里（滚轮优先级按它判断）。 */
	private boolean isOverComboColumn(double mouseX, double mouseY) {
		return mouseX >= this.leftPos - 1 && mouseX < this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH
				&& mouseY >= this.topPos && mouseY < comboViewBottom();
	}

	/** 左侧连击栏：列头固定，列表在可视区里滚动（连击多了也不会画到面板外）。 */
	private void renderComboPanel(GuiGraphics graphics) {
		Font font = this.font;
		int left = this.leftPos + 8;
		int headerY = this.topPos + 8;
		graphics.drawString(font, Component.translatable("screen.sephiria_fantasy.combo.header"), left, headerY,
				HEADER_COLOR, true);

		List<Map.Entry<ArtifactCombo, Integer>> combos = activeCombos();
		int maxScroll = maxComboScrollRow(combos.size());
		this.comboScrollRow = Mth.clamp(this.comboScrollRow, 0, maxScroll);
		int offset = this.comboScrollRow * COMBO_ROW_HEIGHT;

		// 连击比面板装得下的多时，滚出来的那几行必须裁掉——不然会盖到下面的快捷栏上
		graphics.enableScissor(this.leftPos, comboViewTop() - 2,
				this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH - 1, comboViewBottom());

		for (int row = 0; row < combos.size(); row++) {
			ArtifactCombo combo = combos.get(row).getKey();
			int level = combos.get(row).getValue();
			int rowY = comboViewTop() + row * COMBO_ROW_HEIGHT - offset;

			graphics.fill(left - 1, rowY - 1, left + COMBO_ICON + 1, rowY + COMBO_ICON + 1, SLOT_EDGE);
			graphics.blit(comboIcon(combo), left, rowY, 0, 0, COMBO_ICON, COMBO_ICON, COMBO_ICON, COMBO_ICON);

			MutableComponent name = Component.translatable(combo.translationKey())
					.withColor(combo.nameColour(level));
			graphics.drawString(font, name, left + COMBO_ICON + 6, rowY, TEXT_COLOR, true);
			graphics.drawString(font, Component.literal(level + " / " + combo.nextTier(level)),
					left + COMBO_ICON + 6, rowY + 10, TEXT_COLOR, true);
		}

		graphics.disableScissor();
		renderComboScrollbar(graphics, combos.size(), maxScroll);
	}

	/** 连击栏的滚动条：只有装不下时才画，滑块长度按「一屏占内容的比例」算。 */
	private void renderComboScrollbar(GuiGraphics graphics, int activeRows, int maxScroll) {
		if (maxScroll <= 0) {
			return;
		}

		int x = this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH - SCROLLBAR_WIDTH - 2;
		int top = comboViewTop();
		int bottom = comboViewBottom();
		int track = bottom - top;
		int thumb = Math.max(SCROLLBAR_WIDTH * 4, track * visibleComboRows() / activeRows);
		int thumbY = top + (track - thumb) * this.comboScrollRow / maxScroll;

		drawScrollbar(graphics, x, top, bottom, thumbY, thumb);
	}

	/** 连击效果浮窗：标题 + 名字 + 各档位，已达成的亮、未达成的灰。 */
	private void renderComboTooltip(GuiGraphics graphics, ArtifactCombo combo, int level, int mouseX, int mouseY) {
		Font font = this.font;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.sephiria_fantasy.combo.header").withColor(0xFFAAAAAA));
		lines.add(Component.empty());
		lines.add(Component.translatable(combo.translationKey()).withColor(combo.nameColour(level)));

		// 带「主动效果」说明的连击（魔法科技、余烬这类）在档位表之前先给一行说明，再空一行隔开
		Component intro = combo.introLine();

		if (intro != null) {
			lines.add(intro);
			lines.add(Component.empty());
		}

		lines.addAll(combo.effectLines(level));

		int width = 0;

		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}

		int x = mouseX + 8;
		int y = mouseY + 8;
		int height = lines.size() * 10 + 6;
		graphics.fill(x - 3, y - 3, x + width + 3, y + height - 3, 0xF0100010);
		graphics.fill(x - 3, y - 3, x + width + 3, y - 2, 0xFF5050A0);

		int lineY = y;

		for (Component line : lines) {
			graphics.drawString(font, line, x, lineY, TEXT_COLOR, false);
			lineY += 10;
		}
	}

	/**
	 * 鼠标下的那行连击；不在任何一行上、或那一行已经被滚出可视区时返回 null。
	 *
	 * <p>行坐标要减掉滚动量，并且只认可视区内的行——否则滚上去的行虽然画不出来，
	 * 鼠标划过原位置时还会弹出浮窗。
	 */
	private ArtifactCombo hoveredCombo(int mouseX, int mouseY) {
		if (mouseX < this.leftPos - 1 || mouseX >= this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH) {
			return null;
		}

		if (mouseY < comboViewTop() - 2 || mouseY >= comboViewBottom()) {
			return null;
		}

		List<Map.Entry<ArtifactCombo, Integer>> combos = activeCombos();
		int offset = this.comboScrollRow * COMBO_ROW_HEIGHT;

		for (int row = 0; row < combos.size(); row++) {
			int rowY = comboViewTop() + row * COMBO_ROW_HEIGHT - offset;

			if (mouseY >= rowY - 1 && mouseY < rowY + COMBO_ICON + 1) {
				return combos.get(row).getKey();
			}
		}

		return null;
	}

	/**
	 * 滚轮：连击栏上滚它自己的列表；其它地方照旧把背包翻页的方向发给服务端
	 * （背包的「滚到第几行」在服务端，服务端裁完会把这一屏整批推回来）。
	 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0D && isOverComboColumn(mouseX, mouseY)) {
			int maxScroll = maxComboScrollRow(activeCombos().size());

			if (maxScroll > 0) {
				this.comboScrollRow = Mth.clamp(this.comboScrollRow - (int) Math.signum(scrollY), 0, maxScroll);
				return true;
			}

			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		if (scrollY != 0.0D) {
			ClientPlayNetworking.send(new ScrollBackpackPayload(scrollY > 0.0D ? -1 : 1));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** 悬停石板按 R 旋转（保持移植版原来的行为：只要指着格子就发，服务端自己校验）。 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null) {
			ClientPlayNetworking.send(new RotateSlatePayload(hovered.index));
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** 背包格区右侧的滚动条：只有背包比一屏长时才画。 */
	private void renderBackpackScrollbar(GuiGraphics graphics) {
		int maxRow = this.menu.maxScrollRow();

		if (maxRow <= 0) {
			return;
		}

		int x = this.leftPos + ArtifactBackpackMenu.SLOT_X + ArtifactBackpack.WIDTH * 18 + 3;
		int top = this.topPos + ArtifactBackpackMenu.SLOT_Y - 1;
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

	private static ResourceLocation comboIcon(ArtifactCombo combo) {
		return ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/combo_" + combo.id() + ".png");
	}
}
