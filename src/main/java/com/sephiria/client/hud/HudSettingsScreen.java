package com.sephiria.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** HUD 基础设置：显示开关与缩放。 */
public class HudSettingsScreen extends Screen {
	private boolean dragging;
	private double lastMouseX;
	private double lastMouseY;
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
		this.addRenderableWidget(Button.builder(Component.literal("技能 HUD: " + (HudConfig.showSkills() ? "开" : "关")),
				button -> {
					HudConfig.toggleSkills();
					button.setMessage(Component.literal("技能 HUD: " + (HudConfig.showSkills() ? "开" : "关")));
				}).bounds(x, y + 72, 160, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("神器 HUD: " + (HudConfig.showArtifacts() ? "开" : "关")),
				button -> {
					HudConfig.toggleArtifacts();
					button.setMessage(Component.literal("神器 HUD: " + (HudConfig.showArtifacts() ? "开" : "关")));
				}).bounds(x, y + 96, 160, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
				button -> this.onClose()).bounds(x, y + 52, 160, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFFFF);

		int boxX = this.width / 2 - 60 + HudConfig.offsetX();
		int boxY = this.height / 2 - 110 + HudConfig.offsetY();
		graphics.fill(boxX, boxY, boxX + 120, boxY + 34, 0x80000000);
		graphics.fill(boxX, boxY, boxX + 120, boxY + 1, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.literal("拖动这里移动 HUD"), boxX + 60, boxY + 13, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int boxX = this.width / 2 - 60 + HudConfig.offsetX();
		int boxY = this.height / 2 - 110 + HudConfig.offsetY();

		if (mouseX >= boxX && mouseX <= boxX + 120 && mouseY >= boxY && mouseY <= boxY + 34) {
			this.dragging = true;
			this.lastMouseX = mouseX;
			this.lastMouseY = mouseY;
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.dragging) {
			HudConfig.setOffset(HudConfig.offsetX() + (int) (mouseX - this.lastMouseX),
					HudConfig.offsetY() + (int) (mouseY - this.lastMouseY));
			this.lastMouseX = mouseX;
			this.lastMouseY = mouseY;
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.dragging) {
			this.dragging = false;
			HudConfig.save();
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
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
