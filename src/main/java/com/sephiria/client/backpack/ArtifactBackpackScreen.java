package com.sephiria.client.backpack;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.slate.SlateItem;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

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
	/** 格子等级文字：正数绿、负数红。 */
	private static final int SLOT_LEVEL_POSITIVE = 0xFF55FF55;
	private static final int SLOT_LEVEL_NEGATIVE = 0xFFFF5555;
	/** 格子里的等级文字缩放：缩到 0.6 倍，免得盖住图标。 */
	private static final float LEVEL_TEXT_SCALE = 0.6F;
	/** 神器等级文字：失效红、未满白、满级绿、超上限黄。 */
	private static final int LEVEL_INACTIVE = 0xFFFF5555;
	private static final int LEVEL_NORMAL = 0xFFFFFFFF;
	private static final int LEVEL_MAX = 0xFF55FF55;
	private static final int LEVEL_OVER = 0xFFFFD24A;

	private static final int COMBO_ROW_HEIGHT = 26;
	private static final int COMBO_TOP = 30;
	private static final int COMBO_ICON = 16;

	private final int rows;

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

	/** 按 R 旋转鼠标下的石板（默认逆时针 90°）。 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_R) {
			Slot hovered = hoveredBackpackSlot();

			if (hovered != null && hovered.getItem().getItem() instanceof SlateItem) {
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

		renderComboPanel(extractor, mouseX, mouseY);
	}

	/** 左侧连击栏：标题 + 每个已激活连击的一行（图标 / 名字 / 等级）。 */
	private void renderComboPanel(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		Font font = this.font;
		int left = this.leftPos + 8;
		int headerY = this.topPos + 8;
		extractor.text(font, Component.translatable("screen.sephiria.combo.header"), left, headerY, HEADER_COLOR, true);

		Map<ArtifactCombo, Integer> levels = ArtifactEffects.comboLevels(this.menu.container(), this.menu::slotLevel);
		int row = 0;

		for (Map.Entry<ArtifactCombo, Integer> entry : levels.entrySet()) {
			ArtifactCombo combo = entry.getKey();
			int level = entry.getValue();

			if (level <= 0) {
				continue;
			}

			int rowY = this.topPos + COMBO_TOP + row * COMBO_ROW_HEIGHT;
			extractor.fill(left - 1, rowY - 1, left + COMBO_ICON + 1, rowY + COMBO_ICON + 1, SLOT_EDGE);
			extractor.blit(RenderPipelines.GUI_TEXTURED, comboIcon(combo), left, rowY, 0.0F, 0.0F,
					COMBO_ICON, COMBO_ICON, COMBO_ICON, COMBO_ICON);

			MutableComponent name = Component.translatable(combo.translationKey()).withColor(combo.nameColour(level));
			extractor.text(font, name, left + COMBO_ICON + 6, rowY, TEXT_COLOR, true);
			extractor.text(font, Component.literal(level + " / " + combo.nextTier(level)),
					left + COMBO_ICON + 6, rowY + 10, TEXT_COLOR, true);

			row++;
		}
	}

	/** 连击效果浮窗（参考图那种：标题 + 名字 + 各档位，已达成的亮、未达成的灰）。 */
	private void renderComboTooltip(GuiGraphicsExtractor extractor, ArtifactCombo combo, int level, int mouseX, int mouseY) {
		Font font = this.font;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.sephiria.combo.header").withColor(0xFFAAAAAA));
		lines.add(Component.empty());
		lines.add(Component.translatable(combo.translationKey()).withColor(combo.nameColour(level)));
		lines.add(Component.empty());
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

	/** 鼠标下的那行连击；不在任何一行上时返回 null。 */
	private ArtifactCombo hoveredCombo(int mouseX, int mouseY) {
		if (mouseX < this.leftPos - 1 || mouseX >= this.leftPos + ArtifactBackpackMenu.COMBO_PANEL_WIDTH) {
			return null;
		}

		Map<ArtifactCombo, Integer> levels = ArtifactEffects.comboLevels(this.menu.container(), this.menu::slotLevel);
		int row = 0;

		for (Map.Entry<ArtifactCombo, Integer> entry : levels.entrySet()) {
			if (entry.getValue() <= 0) {
				continue;
			}

			int rowY = this.topPos + COMBO_TOP + row * COMBO_ROW_HEIGHT;

			if (mouseY >= rowY - 1 && mouseY < rowY + COMBO_ICON + 1) {
				return entry.getKey();
			}

			row++;
		}

		return null;
	}

	/** 不画标题与「物品栏」标签——这两行在背包页上是多余的。 */
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
	}

	/** 格子里的等级显示：有神器就显示「等级/上限」，否则显示格子等级。 */
	@Override
	protected void extractSlot(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY) {
		ItemStack shown = slot.getItem();

		if (shown.getItem() instanceof SlateItem slate && SlateItem.rotationOf(shown) != 0) {
			// 石板：按朝向把图标一起转过来（绕着格子中心转），这样一眼能看出它朝哪边
			// 这里自己画，所以不调 super（否则会再画一遍没转的图标）
			Matrix3x2fStack pose = extractor.pose();
			pose.pushMatrix();
			pose.translate(slot.x + 8.0F, slot.y + 8.0F);
			// 屏幕坐标 y 向下：负角度才是视觉上的逆时针，与 SlateItem#rotateOffset 的方向一致
			pose.rotate(-SlateItem.rotationOf(shown) * ((float) Math.PI / 2.0F));
			pose.translate(-8.0F, -8.0F);
			extractor.item(shown, 0, 0);
			pose.popMatrix();
		} else {
			super.extractSlot(extractor, slot, mouseX, mouseY);
		}

		if (!this.menu.isBackpackSlot(slot)) {
			return;
		}

		drawLevelText(extractor, slot, shown);
	}

	/** 格子左上角的等级文字（0.6 倍大小）。 */
	private void drawLevelText(GuiGraphicsExtractor extractor, Slot slot, ItemStack stack) {
		Component text;
		int colour;

		if (stack.getItem() instanceof SephiriaArtifact artifact) {
			int level = ArtifactEffects.effectiveLevel(stack, this.menu.slotLevelOf(slot));
			int max = artifact.maxLevel();

			if (level < 0) {
				colour = LEVEL_INACTIVE;
			} else if (level > max) {
				colour = LEVEL_OVER;
			} else if (level == max) {
				colour = LEVEL_MAX;
			} else {
				colour = LEVEL_NORMAL;
			}

			text = Component.literal(level + "/" + max);
		} else {
			int slotLevel = this.menu.slotLevelOf(slot);

			if (slotLevel == 0) {
				return;
			}

			text = Component.literal((slotLevel > 0 ? "+" : "") + Numbers.format(slotLevel));
			colour = slotLevel > 0 ? SLOT_LEVEL_POSITIVE : SLOT_LEVEL_NEGATIVE;
		}

		// 注意坐标系：容器内容是在「以面板左上角为原点」的平移里画的，所以这里用面板内坐标；
		// 字号缩到 0.6 倍，神器等级要小到不盖住图标。
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(slot.x + 1.0F, slot.y + 1.0F);
		pose.scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE);
		extractor.text(this.font, text, 0, 0, colour, true);
		pose.popMatrix();
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
