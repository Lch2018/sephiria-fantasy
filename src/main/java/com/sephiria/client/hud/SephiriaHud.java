package com.sephiria.client.hud;

import com.sephiria.network.HudSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.21.1 的 HUD：技能与神器两排图标，各自按 {@link HudConfig} 里的位置和缩放独立绘制。
 *
 * <p>布局沿用旧版：每格 16px 图标 + 1px 底衬、格距 18px；冷却是黑色遮罩从上往下退，
 * 充能是图标下方一条绿色进度条。两排不再共用一个全局缩放——拖动布局时每排可以单独
 * 摆位、单独缩放（{@link HudConfigScreen} → {@link HudModuleScreen}）。
 */
public final class SephiriaHud implements HudRenderCallback {
	private static final int ICON = 16;
	private static final int STEP = 18;
	/** 行高含图标下方 4px 的充能条；布局夹取按它算，保证放大后也不出屏幕下边。 */
	private static final int ROW_HEIGHT = 22;
	/** 图标相对行内左上角的留白（给 1px 底衬）。 */
	private static final int ICON_OFFSET = 1;
	/** 没有实际条目时预览用的占位格数，和技能栏格数一致。 */
	private static final int DEFAULT_SLOTS = 6;

	private static final int PANEL_COLOR = 0xC0000000;
	private static final int CHARGE_COLOR = 0xFF55FF55;
	private static final int COOLDOWN_COLOR = 0x90000000;
	private static final int FRAME_VISIBLE_COLOR = 0xFFFFFFFF;
	private static final int FRAME_HIDDEN_COLOR = 0xFF808080;
	private static final int PLACEHOLDER_FILL = 0x40000000;
	private static final int PLACEHOLDER_BORDER = 0x60FFFFFF;

	/**
	 * 一个模块在屏幕上的实际位置与尺寸（已经夹进屏幕）。缩放后的宽高是
	 * {@code width * scale} / {@code height * scale}。
	 */
	public record Layout(double x, double y, int width, int height, double scale) {
		public boolean contains(double mouseX, double mouseY) {
			return mouseX >= this.x && mouseX <= this.x + this.width * this.scale
					&& mouseY >= this.y && mouseY <= this.y + this.height * this.scale;
		}
	}

	@Override
	public void onHudRender(GuiGraphics graphics, DeltaTracker tickDelta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.options.hideGui) {
			return;
		}

		renderRow(graphics, HudConfig.SKILLS);
		renderRow(graphics, HudConfig.ARTIFACTS);
	}

	/** 正式 HUD：模块不可见或没有条目时整排不画。 */
	private static void renderRow(GuiGraphics graphics, String module) {
		HudConfig.Element element = HudConfig.element(module);

		if (!element.visible) {
			return;
		}

		List<HudSyncPayload.Entry> entries = entriesOf(module);

		if (entries.isEmpty()) {
			return;
		}

		Layout layout = layout(module, entries.size());
		graphics.pose().pushPose();
		graphics.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
		graphics.pose().scale((float) layout.scale(), (float) layout.scale(), 1.0F);
		drawIcons(graphics, entries, ICON_OFFSET, ICON_OFFSET);
		graphics.pose().popPose();
	}

	/** 布局界面用预览：不看显示开关，但用白 / 灰框标出当前是否可见。 */
	public static void renderPreview(GuiGraphics graphics, String module) {
		Minecraft client = Minecraft.getInstance();
		List<HudSyncPayload.Entry> entries = entriesOf(module);
		int slots = entries.isEmpty() ? DEFAULT_SLOTS : entries.size();
		Layout layout = layout(module, slots);

		graphics.pose().pushPose();
		graphics.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
		graphics.pose().scale((float) layout.scale(), (float) layout.scale(), 1.0F);

		if (entries.isEmpty()) {
			drawPlaceholders(graphics, client, slots);
		}
		else {
			drawIcons(graphics, entries, ICON_OFFSET, ICON_OFFSET);
		}

		drawFrame(graphics, layout.width(), layout.height(), HudConfig.element(module).visible);
		graphics.pose().popPose();
	}

	/** 模块在屏幕上的实际位置：配置坐标先按缩放后的尺寸夹进屏幕。 */
	public static Layout layout(String module) {
		List<HudSyncPayload.Entry> entries = entriesOf(module);
		return layout(module, entries.isEmpty() ? DEFAULT_SLOTS : entries.size());
	}

	private static Layout layout(String module, int slots) {
		HudConfig.Element element = HudConfig.element(module);
		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow() == null ? 320 : client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow() == null ? 240 : client.getWindow().getGuiScaledHeight();
		int width = Math.max(1, slots) * STEP;
		double maxX = Math.max(0.0D, screenWidth - width * element.scale);
		double maxY = Math.max(0.0D, screenHeight - ROW_HEIGHT * element.scale);

		return new Layout(Mth.clamp(element.x, 0.0D, maxX), Mth.clamp(element.y, 0.0D, maxY),
				width, ROW_HEIGHT, element.scale);
	}

	private static List<HudSyncPayload.Entry> entriesOf(String module) {
		return HudConfig.ARTIFACTS.equals(module) ? HudClientData.artifacts() : HudClientData.skills();
	}

	private static void drawIcons(GuiGraphics graphics, List<HudSyncPayload.Entry> entries, int x, int y) {
		for (HudSyncPayload.Entry entry : entries) {
			ItemStack stack = HudClientData.stackOf(entry);

			if (stack.isEmpty()) {
				x += STEP;
				continue;
			}

			graphics.fill(x - 1, y - 1, x + ICON + 1, y + ICON + 1, PANEL_COLOR);
			graphics.renderItem(stack, x, y);

			float progress = HudClientData.progress(entry);

			if (entry.charging()) {
				graphics.fill(x, y + ICON + 2, x + Math.round(ICON * progress), y + ICON + 4, CHARGE_COLOR);
			}
			else if (progress < 1.0F) {
				int shade = Math.round(ICON * (1.0F - progress));
				graphics.fill(x, y, x + ICON, y + shade, COOLDOWN_COLOR);
			}

			x += STEP;
		}
	}

	/** 预览框：可见时白色、隐藏时灰色，1px 粗（随缩放变粗）。 */
	private static void drawFrame(GuiGraphics graphics, int width, int height, boolean visible) {
		int color = visible ? FRAME_VISIBLE_COLOR : FRAME_HIDDEN_COLOR;

		graphics.fill(0, 0, width, 1, color);
		graphics.fill(0, height - 1, width, height, color);
		graphics.fill(0, 0, 1, height, color);
		graphics.fill(width - 1, 0, width, height, color);
	}

	/** 没有技能 / 神器数据时的占位格，保证布局界面里永远有东西可以拖。 */
	private static void drawPlaceholders(GuiGraphics graphics, Minecraft client, int slots) {
		for (int index = 0; index < slots; index++) {
			int x = ICON_OFFSET + index * STEP;
			int top = ICON_OFFSET - 1;
			int bottom = ICON_OFFSET + ICON + 1;

			graphics.fill(x - 1, top, x + ICON + 1, bottom, PLACEHOLDER_FILL);
			graphics.fill(x - 1, top, x + ICON + 1, top + 1, PLACEHOLDER_BORDER);
			graphics.fill(x - 1, bottom - 1, x + ICON + 1, bottom, PLACEHOLDER_BORDER);
			graphics.fill(x - 1, top, x, bottom, PLACEHOLDER_BORDER);
			graphics.fill(x + ICON, top, x + ICON + 1, bottom, PLACEHOLDER_BORDER);
			graphics.drawCenteredString(client.font, "?", x + ICON / 2, top + 4, PLACEHOLDER_BORDER);
		}
	}
}
