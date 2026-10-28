package com.sephiria.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** HUD 基础设置：显示开关与缩放。 */
public class HudSettingsScreen extends Screen {
	public HudSettingsScreen() {
		super(Component.translatable("screen.sephiria_fantasy.hud"));
	}

	@Override
	protected void init() {
		super.init();
		int x = this.width / 2 - 80;
		int y = this.height / 2 - 30;
		this.addRenderableWidget(Button.builder(visibleLabel(), button -> {
			HudConfig.toggleVisible();
			button.setMessage(visibleLabel());
		}).bounds(x, y, 160, 20).build());
		this.addRenderableWidget(Button.builder(scaleLabel(), button -> {
			HudConfig.cycleScale();
			button.setMessage(scaleLabel());
		}).bounds(x, y + 24, 160, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
				button -> this.onClose()).bounds(x, y + 52, 160, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
	}

	private static Component visibleLabel() {
		return Component.translatable("screen.sephiria_fantasy.hud.visible",
				Component.translatable(HudConfig.visible() ? "gui.yes" : "gui.no"));
	}

	private static Component scaleLabel() {
		return Component.translatable("screen.sephiria_fantasy.hud.scale",
				Math.round(HudConfig.scale() * 100.0F) + "%");
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
