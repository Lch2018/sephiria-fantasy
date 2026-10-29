package com.sephiria.client.slate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.Sephiria;
import com.sephiria.client.backpack.BackpackCellRenderer;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.FuseSlatesPayload;
import com.sephiria.network.RotateFusionSlatePayload;
import com.sephiria.slate.PatternSlateItem;
import com.sephiria.slate.SlateFusionMenu;
import com.sephiria.slate.SlateItem;
import com.sephiria.util.Numbers;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 石板合成界面（移植 26.3 的样子）：最上面标题，中间 7×7 的合成石板效果预览，
 * 下面一行「两个输入格 / 合成 / 输出格」，再下面是玩家背包。
 *
 * <p>预览的画法与赛菲莉娅背包同源（格子底、绿加红减的等级数字），合成石板画在网格的
 * {@link SlateFusionMenu#PREVIEW_ORIGIN} 那一格；数字就是它放进背包后对周围格子的加成。
 *
 * <p>预览只是把两块输入石板的图案按 {@code SlateFusion.combine} 合起来画，不碰容器：
 * 输入格的内容原版会同步过来，所以客户端自己就能算，不需要发包。
 *
 * <p>按 R 旋转鼠标下的输入石板（与服务端发来的朝向同步）；「合成」只在两块输入都在、
 * 输出格还空着且这一次还没合成过时可点。
 */
public class SlateFusionScreen extends SephiriaMenuScreen<SlateFusionMenu> {
	private static final int LEVEL_POSITIVE = 0xFF55FF55;
	private static final int LEVEL_NEGATIVE = 0xFFFF5555;
	/** 预览格子的淡色底：正增量绿、负增量红（很低的不透明度，别盖住数字）。 */
	private static final int TINT_POSITIVE = 0x2055FF55;
	private static final int TINT_NEGATIVE = 0x20FF5555;
	private static final float LEVEL_TEXT_SCALE = 0.6F;
	/**
	 * 预览用的合成石板图标：直接 blit 贴图，不走物品模型——预览只是背景装饰，
	 * 别为它把物品渲染管线拖进 {@code renderBg}。贴图与物品图标是同一张（16×16）。
	 */
	private static final ResourceLocation FUSION_ICON =
			new ResourceLocation(Sephiria.MOD_ID, "textures/item/slate_of_fusion.png");

	private Button craftButton;

	public SlateFusionScreen(SlateFusionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SlateFusionMenu.IMAGE_WIDTH, SlateFusionMenu.imageHeight(), true);
		this.inventoryLabelX = SlateFusionMenu.PLAYER_X;
		this.inventoryLabelY = SlateFusionMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		this.craftButton = Button.builder(
				Component.translatable("screen.sephiria_fantasy.slate_fusion.craft"),
				pressed -> ClientPlayNetworking.send(FuseSlatesPayload.CHANNEL,
						new FriendlyByteBuf(Unpooled.buffer())))
				.bounds(this.leftPos + SlateFusionMenu.BUTTON_X, this.topPos + SlateFusionMenu.BUTTON_Y,
						SlateFusionMenu.BUTTON_WIDTH, SlateFusionMenu.BUTTON_HEIGHT).build();
		this.addRenderableWidget(this.craftButton);
	}

	/** 「合成」只在两块输入都在、输出格还空着、并且这一次界面里还没合成过时可点。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.craftButton != null) {
			this.craftButton.active = this.menu.canCraft() && !this.menu.fusionUsed();
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);
		renderPreviewGrid(graphics);

		// 输入 / 输出格的底（玩家背包沿用通用界面的画法）
		for (Slot slot : this.menu.slots) {
			drawSlotBackground(graphics, slot);
		}
	}

	/** 7×7 预览：格子底 + 合并图案的数字 + 正中间的合成石板图标。 */
	private void renderPreviewGrid(GuiGraphics graphics) {
		int gridX = this.leftPos + SlateFusionMenu.GRID_X;
		int gridY = this.topPos + SlateFusionMenu.GRID_Y;

		for (int row = 0; row < SlateFusionMenu.GRID_CELLS; row++) {
			for (int col = 0; col < SlateFusionMenu.GRID_CELLS; col++) {
				int x = gridX + col * SlateFusionMenu.GRID_CELL;
				int y = gridY + row * SlateFusionMenu.GRID_CELL;
				graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
				graphics.fill(x, y, x + 16, y + 16, SLOT);
			}
		}

		if (!this.menu.hasPreview()) {
			Component hint = Component.translatable("screen.sephiria_fantasy.slate_fusion.empty");
			int width = this.font.width(hint);
			graphics.drawString(this.font, hint,
					gridX + (SlateFusionMenu.GRID_CELLS * SlateFusionMenu.GRID_CELL - width) / 2,
					// 预览区正中（字高 9，网格高度 - 9 再取半）
					gridY + (SlateFusionMenu.GRID_CELLS * SlateFusionMenu.GRID_CELL - 9) / 2, HINT, false);
			return;
		}

		List<PatternSlateItem.Cell> pattern = this.menu.previewPattern();

		for (PatternSlateItem.Cell cell : pattern) {
			int col = SlateFusionMenu.PREVIEW_ORIGIN + cell.dx();
			int row = SlateFusionMenu.PREVIEW_ORIGIN + cell.dy();

			// 图案可能伸到 7×7 外面（±8 的贯穿型石板）：界面上就只画看得见的那一段
			if (col < 0 || col >= SlateFusionMenu.GRID_CELLS || row < 0 || row >= SlateFusionMenu.GRID_CELLS) {
				continue;
			}

			int x = gridX + col * SlateFusionMenu.GRID_CELL;
			int y = gridY + row * SlateFusionMenu.GRID_CELL;
			int delta = cell.delta();
			graphics.fill(x, y, x + 16, y + 16, delta > 0 ? TINT_POSITIVE : TINT_NEGATIVE);
			drawLevelText(graphics, x + 1, y + 1, delta);
		}

		int originX = gridX + SlateFusionMenu.PREVIEW_ORIGIN * SlateFusionMenu.GRID_CELL;
		int originY = gridY + SlateFusionMenu.PREVIEW_ORIGIN * SlateFusionMenu.GRID_CELL;
		graphics.fill(originX, originY, originX + 16, originY + 16, 0xFF3A3A46);
		graphics.blit(FUSION_ICON, originX, originY, 0, 0, 16, 16, 16, 16);
	}

	/** 预览格左上角的增量文字：与背包里的格子等级同一套画法（0.6 倍、绿加红减）。 */
	private void drawLevelText(GuiGraphics graphics, int x, int y, int delta) {
		Component text = Component.literal((delta > 0 ? "+" : "") + Numbers.format(delta));
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x, y, 0.0F);
		pose.scale(LEVEL_TEXT_SCALE, LEVEL_TEXT_SCALE, 1.0F);
		graphics.drawString(this.font, text, 0, 0, delta > 0 ? LEVEL_POSITIVE : LEVEL_NEGATIVE, true);
		pose.popPose();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// super 画标题与「物品栏」标签
		super.renderLabels(graphics, mouseX, mouseY);

		Component hint = Component.translatable("screen.sephiria_fantasy.slate_fusion.hint");
		int hintWidth = this.font.width(hint);
		graphics.drawString(this.font, hint,
				(this.imageWidth - hintWidth) / 2, SlateFusionMenu.HINT_Y, HINT, false);
	}

	/** 输入格里的石板要按朝向转过来（与背包一致的显示）。 */
	@Override
	protected void renderSlotOverlays(GuiGraphics graphics) {
		for (int index = 0; index < SlateFusionMenu.INPUT_COUNT; index++) {
			Slot slot = this.menu.slots.get(index);
			ItemStack stack = slot.getItem();

			if (stack.getItem() instanceof SlateItem && SlateItem.rotationOf(stack) != 0) {
				redrawSlot(graphics, slot);
				BackpackCellRenderer.drawRotatedItem(graphics, this.font,
						this.leftPos + slot.x, this.topPos + slot.y, stack);
				restoreSlotHighlight(graphics, slot);
			}
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null
				&& (hovered.index == SlateFusionMenu.INPUT_A || hovered.index == SlateFusionMenu.INPUT_B)) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			RotateFusionSlatePayload.write(buffer,
					new RotateFusionSlatePayload(hovered.index == SlateFusionMenu.INPUT_A ? 0 : 1));
			ClientPlayNetworking.send(RotateFusionSlatePayload.CHANNEL, buffer);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
