package com.sephiria.client.backpack;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.slate.SlateItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 赛菲利亚背包的界面。
 *
 * <p>左边是连击栏（连击图标 / 名字 / 等级，鼠标放上去看该连击的效果），右边是背包格子与玩家背包。
 * 面板不贴图，直接用纯色块画，这样扩容到任意高度都不用再切图。
 *
 * <p>格子里的显示：神器显示「实际等级/上限」（按与上限的关系上色），没有神器时显示格子等级
 * （石板加出来的，可以是负数）——神器放上去会盖住格子等级，和设计一致。
 */
public class ArtifactBackpackScreen extends AbstractContainerScreen<ArtifactBackpackMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;
	private static final int HEADER_COLOR = 0xFFFFD24A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int TAB_HEIGHT = 20;

	private static final int COMBO_ROW_HEIGHT = 26;
	private static final int COMBO_TOP = 30;
	private static final int COMBO_ICON = 16;
	/** 连击列表下方的留白：裁剪框与滚动条都按它收边。 */
	private static final int COMBO_BOTTOM_PAD = 4;
	/** 连击栏的滚动条：2 px 宽，贴左栏右缘；轨道压暗、滑块提亮。 */
	private static final int COMBO_SCROLLBAR_WIDTH = 2;
	private static final int COMBO_SCROLLBAR_COLOR = 0xFF15151A;
	private static final int COMBO_SCROLLBAR_THUMB = 0xFF7A7A88;

	private final int rows;

	/** 连击列表滚到第几行（0 = 从头开始）：装不下时才用得上，滚轮一次一行。 */
	private int comboScrollRow;

	public ArtifactBackpackScreen(ArtifactBackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ArtifactBackpackMenu.IMAGE_WIDTH,
				ArtifactBackpackMenu.imageHeight(ArtifactBackpack.DEFAULT_HEIGHT));
		this.rows = ArtifactBackpack.DEFAULT_HEIGHT;
		this.inventoryLabelY = ArtifactBackpackMenu.playerRowsY(this.rows) - 11;
	}

	@Override
	protected void init() {
		super.init();
		SephiriaTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, SephiriaTab.BACKPACK);
	}

	/** 按 R 旋转鼠标下的石板（默认逆时针 90°）；写着「不可旋转」的石板不响应。 */
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		ArtifactTooltip.setSlotLevelHint(hoveredSlotLevel(mouseX, mouseY));
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		ArtifactTooltip.clearSlotLevelHint();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 容器界面在原版里 isInGameUi() 为 true，原版这条路只压暗、不虚化；补上「属性 / 技能页」
		// 那套（普通 Screen 走的）：先虚化，再盖一层 25% 黑的菜单底纹。虚化会把这一层之前抽取的
		// 内容一起糊掉——26.3 里 HUD 与物品栏正是在界面之前抽取的，所以它们也会跟着虚化。
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 连击栏与容器区之间的分隔线
		int divider = left + ArtifactBackpackMenu.COMBO_PANEL_WIDTH;
		extractor.fill(divider - 2, top + 4, divider - 1, top + this.imageHeight - 4, BORDER_LIGHT);

		for (Slot slot : this.menu.slots) {
			if (!this.menu.isBackpackSlot(slot)) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;
			extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}

		renderComboPanel(extractor);
	}

	/** 连击栏可视区的上边（绝对 y）。 */
	private int comboViewTop() {
		return this.topPos + COMBO_TOP;
	}

	/** 可视区的下边：面板底部按 {@link #COMBO_BOTTOM_PAD} 收边。 */
	private int comboViewBottom() {
		return this.topPos + this.imageHeight - COMBO_BOTTOM_PAD;
	}

	/** 一屏能完整显示几行连击：按面板高度算出来，别写死——背包扩容时面板会跟着变高。 */
	private int visibleComboRows() {
		return Math.max(1, (comboViewBottom() - comboViewTop()) / COMBO_ROW_HEIGHT);
	}

	/**
	 * 当前生效的连击（等级 &gt; 0），按枚举顺序（{@code comboLevels} 给的是 EnumMap）。
	 *
	 * <p>绘制、悬停判定与滚动范围都吃这一份：以前三处各自遍历一遍地图，布局一改就会出现
	 * 「画在这儿、悬停判定在那儿」的错位。
	 */
	private List<Map.Entry<ArtifactCombo, Integer>> activeCombos() {
		List<Map.Entry<ArtifactCombo, Integer>> active = new ArrayList<>();

		for (Map.Entry<ArtifactCombo, Integer> entry
				: ArtifactEffects.comboLevels(this.menu.container(), this.menu::slotLevel).entrySet()) {
			if (entry.getValue() > 0) {
				active.add(entry);
			}
		}

		return active;
	}

	/** 最多能往下滚几行（内容装得下时是 0）。 */
	private int maxComboScrollRow(int activeRows) {
		return Math.max(0, activeRows - visibleComboRows());
	}

	/** 鼠标是不是在左侧连击栏这一列里（滚轮滚动按它判断）。 */
	private boolean isOverComboColumn(double mouseX, double mouseY) {
		return mouseX >= this.leftPos - 1 && mouseX < this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH
				&& mouseY >= this.topPos && mouseY < comboViewBottom();
	}

	/** 左侧连击栏：标题固定，列表在可视区里滚动（连击多了也不会画到面板外）。 */
	private void renderComboPanel(GuiGraphicsExtractor extractor) {
		Font font = this.font;
		int left = this.leftPos + 8;
		int headerY = this.topPos + 8;
		extractor.text(font, Component.translatable("screen.sephiria_fantasy.combo.header"), left, headerY, HEADER_COLOR, true);

		List<Map.Entry<ArtifactCombo, Integer>> combos = activeCombos();
		int maxScroll = maxComboScrollRow(combos.size());
		this.comboScrollRow = Mth.clamp(this.comboScrollRow, 0, maxScroll);
		int offset = this.comboScrollRow * COMBO_ROW_HEIGHT;

		// 连击比面板装得下的多时，滚出来的那几行必须裁掉——不然会盖到下面的快捷栏上
		extractor.enableScissor(this.leftPos, comboViewTop() - 2,
				this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH - 1, comboViewBottom());

		for (int row = 0; row < combos.size(); row++) {
			ArtifactCombo combo = combos.get(row).getKey();
			int level = combos.get(row).getValue();
			int rowY = comboViewTop() + row * COMBO_ROW_HEIGHT - offset;

			extractor.fill(left - 1, rowY - 1, left + COMBO_ICON + 1, rowY + COMBO_ICON + 1, SLOT_EDGE);
			extractor.blit(RenderPipelines.GUI_TEXTURED, comboIcon(combo), left, rowY, 0.0F, 0.0F,
					COMBO_ICON, COMBO_ICON, COMBO_ICON, COMBO_ICON);

			MutableComponent name = Component.translatable(combo.translationKey()).withColor(combo.nameColour(level));
			extractor.text(font, name, left + COMBO_ICON + 6, rowY, TEXT_COLOR, true);
			extractor.text(font, Component.literal(level + " / " + combo.nextTier(level)),
					left + COMBO_ICON + 6, rowY + 10, TEXT_COLOR, true);
		}

		extractor.disableScissor();
		renderComboScrollbar(extractor, combos.size(), maxScroll);
	}

	/** 连击栏的滚动条：只有装不下时才画，滑块长度按「一屏占内容的比例」算。 */
	private void renderComboScrollbar(GuiGraphicsExtractor extractor, int activeRows, int maxScroll) {
		if (maxScroll <= 0) {
			return;
		}

		int x = this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH - COMBO_SCROLLBAR_WIDTH - 2;
		int top = comboViewTop();
		int bottom = comboViewBottom();
		int track = bottom - top;
		int thumb = Math.max(COMBO_SCROLLBAR_WIDTH * 4, track * visibleComboRows() / activeRows);
		int thumbY = top + (track - thumb) * this.comboScrollRow / maxScroll;

		extractor.fill(x, top, x + COMBO_SCROLLBAR_WIDTH, bottom, COMBO_SCROLLBAR_COLOR);
		extractor.fill(x, thumbY, x + COMBO_SCROLLBAR_WIDTH, thumbY + thumb, COMBO_SCROLLBAR_THUMB);
	}

	/** 连击效果浮窗（参考图那种：标题 + 名字 + 各档位，已达成的亮、未达成的灰）。 */
	private void renderComboTooltip(GuiGraphicsExtractor extractor, ArtifactCombo combo, int level, int mouseX, int mouseY) {
		Font font = this.font;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.sephiria_fantasy.combo.header").withColor(0xFFAAAAAA));
		lines.add(Component.empty());
		lines.add(Component.translatable(combo.translationKey()).withColor(combo.nameColour(level)));

		// 带「效果说明」的连击（魔法科技）在档位表之前先给一行说明，再空一行隔开
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
		extractor.fill(x - 3, y - 3, x + width + 3, y + height - 3, 0xF0100010);
		extractor.fill(x - 3, y - 3, x + width + 3, y - 2, 0xFF5050A0);

		int lineY = y;

		for (Component line : lines) {
			extractor.text(font, line, x, lineY, TEXT_COLOR);
			lineY += 10;
		}
	}

	/** 连击浮窗画在提示框层——这一层在最上面，不会被格子与物品盖住。 */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		super.extractTooltip(extractor, mouseX, mouseY);

		ArtifactCombo hovered = hoveredCombo(mouseX, mouseY);

		if (hovered != null) {
			int level = ArtifactEffects.comboLevels(this.menu.container(), this.menu::slotLevel).getOrDefault(hovered, 0);
			renderComboTooltip(extractor, hovered, level, mouseX, mouseY);
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

	/** 鼠标停在连击栏上时，滚轮一次翻一行（内容装得下就不拦，交回原版处理）。 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0D && isOverComboColumn(mouseX, mouseY)) {
			int maxScroll = maxComboScrollRow(activeCombos().size());

			if (maxScroll > 0) {
				this.comboScrollRow = Mth.clamp(this.comboScrollRow - (int) Math.signum(scrollY), 0, maxScroll);
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** 不画标题与「物品栏」标签——这两行在背包页上是多余的。 */
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
	}

	/** 格子里的等级显示：有神器就显示「等级/上限」，否则显示格子等级。 */
	@Override
	protected void extractSlot(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY) {
		ItemStack shown = slot.getItem();

		if (shown.getItem() instanceof SlateItem && SlateItem.rotationOf(shown) != 0) {
			// 石板：按朝向把图标一起转过来（绕着格子中心转），这样一眼能看出它朝哪边
			// 这里自己画，所以不调 super（否则会再画一遍没转的图标）
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

	private static Identifier comboIcon(ArtifactCombo combo) {
		return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/combo_" + combo.id() + ".png");
	}
}
