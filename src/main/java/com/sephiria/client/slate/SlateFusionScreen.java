package com.sephiria.client.slate;

import com.sephiria.Sephiria;
import com.sephiria.client.backpack.BackpackCellRenderer;
import com.sephiria.network.FuseSlatesPayload;
import com.sephiria.network.RotateFusionSlatePayload;
import com.sephiria.slate.PatternSlateItem;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.slate.SlateItem;
import com.sephiria.util.Numbers;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * 石板合成界面：最上面标题，中间 7×7 的合成石板效果预览，下面一行「两个输入格 / 合成 / 输出格」，
 * 再下面是玩家背包。
 *
 * <p>预览的画法与赛菲莉娅背包同源（格子底、绿加红减的等级数字），合成石板画在网格的
 * {@link SlateFusionMenu#PREVIEW_ORIGIN} 那一格；数字就是它放进背包后对周围格子的加成。
 *
 * <p>按 R 旋转鼠标下的输入石板（与服务端发来的朝向同步）；「合成」只在两块输入都在、
 * 输出格还空着时可点。
 */
public class SlateFusionScreen extends AbstractContainerScreen<SlateFusionMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HINT_COLOR = 0xFF9A9AA2;
	private static final int LEVEL_POSITIVE = 0xFF55FF55;
	private static final int LEVEL_NEGATIVE = 0xFFFF5555;
	/** 预览格子的淡色底：正增量绿、负增量红（很低的不透明度，别盖住数字）。 */
	private static final int TINT_POSITIVE = 0x2055FF55;
	private static final int TINT_NEGATIVE = 0x20FF5555;
	private static final float LEVEL_TEXT_SCALE = 0.6F;
	/**
	 * 预览用的合成石板图标。这里必须直接 blit 贴图，不能用 {@code extractor.item(...)}：
	 * 背景这一层是「收集渲染状态」的阶段，26.3 在这里走物品模型会崩（技能页踩过一次，
	 * 见 ArtifactSkillScreen#drawIcon 的注释）。
	 */
	private static final Identifier FUSION_ICON =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/item/slate_of_fusion.png");

	private final Component fusionTitle;
	private Button craftButton;

	public SlateFusionScreen(SlateFusionMenu menu, Inventory inventory, Component title) {
		// 标题自己居中画，super 那份传空，免得左上角再画一遍
		super(menu, inventory, Component.empty(), SlateFusionMenu.IMAGE_WIDTH, SlateFusionMenu.imageHeight());
		this.fusionTitle = title;
		this.inventoryLabelX = SlateFusionMenu.PLAYER_X;
		this.inventoryLabelY = SlateFusionMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		this.craftButton = Button.builder(
						Component.translatable("screen.sephiria_fantasy.slate_fusion.craft"),
						pressed -> ClientPlayNetworking.send(FuseSlatesPayload.INSTANCE))
				.bounds(this.leftPos + SlateFusionMenu.BUTTON_X, this.topPos + SlateFusionMenu.BUTTON_Y,
						SlateFusionMenu.BUTTON_WIDTH, SlateFusionMenu.BUTTON_HEIGHT)
				.build();
		addRenderableWidget(this.craftButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		if (this.craftButton != null) {
			// 每个商人每个玩家一次：成功合成后这一次界面里也不许再合
			this.craftButton.active = this.menu.canCraft() && !this.menu.fusionUsed();
		}

		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	/** 按 R 旋转鼠标下的输入石板；朝向由服务端改，改完原版会把新组件同步回来。 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		// F 是原版的交换副手键：合成界面没有这个功能，照背包页的规矩吞掉，
		// 免得它绕过输入格的 mayPlace 把副手物品换进来
		if (event.key() == InputConstants.KEY_F) {
			return true;
		}

		if (event.key() == InputConstants.KEY_R && this.hoveredSlot != null) {
			int index = this.menu.slots.indexOf(this.hoveredSlot);

			if (index >= 0 && index < SlateFusionMenu.INPUT_COUNT) {
				ItemStack stack = this.hoveredSlot.getItem();

				if (stack.getItem() instanceof SlateItem slate && slate.rotatable(stack)) {
					ClientPlayNetworking.send(new RotateFusionSlatePayload(index));
					return true;
				}
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 与背包 / 宝箱 / 商人界面同一套：虚化 + 25% 黑底纹
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		renderPreviewGrid(extractor);

		// 输入 / 输出格的底（玩家背包沿用其它界面：只画物品、不画格子底）
		for (int index = 0; index < SlateFusionMenu.SLOTS; index++) {
			Slot slot = this.menu.slots.get(index);
			int x = left + slot.x;
			int y = top + slot.y;
			extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}
	}

	/** 7×7 预览：格子底 + 合并图案的数字 + 正中间的合成石板图标。 */
	private void renderPreviewGrid(GuiGraphicsExtractor extractor) {
		int gridX = this.leftPos + SlateFusionMenu.GRID_X;
		int gridY = this.topPos + SlateFusionMenu.GRID_Y;

		for (int row = 0; row < SlateFusionMenu.GRID_CELLS; row++) {
			for (int col = 0; col < SlateFusionMenu.GRID_CELLS; col++) {
				int x = gridX + col * SlateFusionMenu.GRID_CELL;
				int y = gridY + row * SlateFusionMenu.GRID_CELL;
				extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
				extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
			}
		}

		if (!this.menu.hasPreview()) {
			Component hint = Component.translatable("screen.sephiria_fantasy.slate_fusion.empty");
			int width = this.font.width(hint);
			extractor.text(this.font, hint,
					gridX + (SlateFusionMenu.GRID_CELLS * SlateFusionMenu.GRID_CELL - width) / 2,
					// 预览区正中（字高 9，网格高度 - 9 再取半）
					gridY + (SlateFusionMenu.GRID_CELLS * SlateFusionMenu.GRID_CELL - 9) / 2, HINT_COLOR);
			return;
		}

		for (PatternSlateItem.Cell cell : this.menu.previewPattern()) {
			int col = SlateFusionMenu.PREVIEW_ORIGIN + cell.dx();
			int row = SlateFusionMenu.PREVIEW_ORIGIN + cell.dy();

			// 图案可能伸到 7×7 外面（±8 的贯穿型石板）：界面上就只画看得见的那一段
			if (col < 0 || col >= SlateFusionMenu.GRID_CELLS || row < 0 || row >= SlateFusionMenu.GRID_CELLS) {
				continue;
			}

			int x = gridX + col * SlateFusionMenu.GRID_CELL;
			int y = gridY + row * SlateFusionMenu.GRID_CELL;
			int delta = cell.delta();
			extractor.fill(x, y, x + 16, y + 16, delta > 0 ? TINT_POSITIVE : TINT_NEGATIVE);
			drawLevelText(extractor, x + 1, y + 1, delta);
		}

		int originX = gridX + SlateFusionMenu.PREVIEW_ORIGIN * SlateFusionMenu.GRID_CELL;
		int originY = gridY + SlateFusionMenu.PREVIEW_ORIGIN * SlateFusionMenu.GRID_CELL;
		extractor.fill(originX, originY, originX + 16, originY + 16, 0xFF3A3A46);
		extractor.blit(RenderPipelines.GUI_TEXTURED, FUSION_ICON, originX, originY, 0.0F, 0.0F,
				16, 16, 16, 16);
	}

	/** 预览格左上角的增量文字：与背包里的格子等级同一套画法（0.6 倍、绿加红减）。 */
	private void drawLevelText(GuiGraphicsExtractor extractor, int x, int y, int delta) {
		Component text = Component.literal((delta > 0 ? "+" : "") + Numbers.format(delta));
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE);
		extractor.text(this.font, text, 0, 0, delta > 0 ? LEVEL_POSITIVE : LEVEL_NEGATIVE, true);
		pose.popMatrix();
	}

	/** 输入格里的石板要按朝向转过来（与背包一致的显示）。 */
	@Override
	protected void extractSlot(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY) {
		int index = this.menu.slots.indexOf(slot);
		ItemStack stack = slot.getItem();

		if (index >= 0 && index < SlateFusionMenu.INPUT_COUNT
				&& stack.getItem() instanceof SlateItem && SlateItem.rotationOf(stack) != 0) {
			BackpackCellRenderer.drawRotatedItem(extractor, slot, stack);
			return;
		}

		super.extractSlot(extractor, slot, mouseX, mouseY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		// super 画「物品栏」标签（标题是空字符串）
		super.extractLabels(extractor, mouseX, mouseY);

		int titleWidth = this.font.width(this.fusionTitle);
		extractor.text(this.font, this.fusionTitle,
				this.leftPos + (this.imageWidth - titleWidth) / 2, this.topPos + 6, TEXT_COLOR, true);

		Component hint = Component.translatable("screen.sephiria_fantasy.slate_fusion.hint");
		int hintWidth = this.font.width(hint);
		extractor.text(this.font, hint,
				this.leftPos + (this.imageWidth - hintWidth) / 2, this.topPos + SlateFusionMenu.HINT_Y,
				HINT_COLOR);
	}
}
