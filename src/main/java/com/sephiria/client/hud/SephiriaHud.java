package com.sephiria.client.hud;

import com.sephiria.network.HudSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.21.1 的基础 HUD：左下角两排图标（上排技能、下排神器），冷却用黑色遮罩从上往下退，
 * 充能用一条绿色进度条。26.3 的布局设置、缩放、模块化拖拽还没接。
 */
public final class SephiriaHud implements HudRenderCallback {
	private static final int ICON = 16;
	private static final int STEP = 18;
	private static final int MARGIN = 8;
	private static final int ROW_GAP = 4;

	@Override
	public void onHudRender(GuiGraphics graphics, DeltaTracker tickDelta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.options.hideGui || !HudConfig.visible()) {
			return;
		}

		graphics.pose().pushPose();
		graphics.pose().scale(HudConfig.scale(), HudConfig.scale(), 1.0F);

		int y = graphics.guiHeight() - 64;
		drawRow(graphics, HudClientData.skills(), MARGIN, y);
		drawRow(graphics, HudClientData.artifacts(), MARGIN, y - STEP - ROW_GAP);
		graphics.pose().popPose();
	}

	private void drawRow(GuiGraphics graphics, List<HudSyncPayload.Entry> entries, int x, int y) {
		for (HudSyncPayload.Entry entry : entries) {
			ItemStack stack = HudClientData.stackOf(entry);

			if (stack.isEmpty()) {
				x += STEP;
				continue;
			}

			graphics.fill(x - 1, y - 1, x + ICON + 1, y + ICON + 1, 0xC0000000);
			graphics.renderItem(stack, x, y);

			float progress = HudClientData.progress(entry);

			if (entry.charging()) {
				graphics.fill(x, y + ICON + 2, x + Math.round(ICON * progress), y + ICON + 4, 0xFF55FF55);
			}
			else if (progress < 1.0F) {
				int shade = Math.round(ICON * (1.0F - progress));
				graphics.fill(x, y, x + ICON, y + shade, 0x90000000);
			}

			x += STEP;
		}
	}
}
