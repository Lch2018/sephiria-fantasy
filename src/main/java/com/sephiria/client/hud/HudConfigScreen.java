package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的一级界面：每个模块一行「显示开关」+「布局」。
 *
 * <p>模块清单在 {@link HudConfig#MODULES} 统一登记，所以这个界面不需要因为新增模块而改动；
 * 点「布局」进 {@link HudModuleScreen}，在那里拖动预览调整位置、用 - / + 调缩放。
 */
public class HudConfigScreen extends Screen {
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int BUTTON_GAP = 8;
	private static final int TOGGLE_WIDTH = 160;

	private final Screen parent;

	public HudConfigScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.hud"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		// GUI 宽度最小 320，但留一点余量，别在极端分辨率下把按钮挤出屏幕
		int rowWidth = Math.min(ROW_WIDTH, this.width - 16);
		int toggleWidth = Math.min(TOGGLE_WIDTH, rowWidth - 64 - BUTTON_GAP);
		int left = (this.width - rowWidth) / 2;
		int top = 52;

		for (int index = 0; index < HudConfig.MODULES.size(); index++) {
			String module = HudConfig.MODULES.get(index);
			HudConfig.Element element = HudConfig.element(module);
			int y = top + index * (ROW_HEIGHT + ROW_GAP);

			addRenderableWidget(Button.builder(toggleLabel(module, element), button -> {
						element.visible = !element.visible;
						button.setMessage(toggleLabel(module, element));
						HudConfig.save();
					})
					.bounds(left, y, toggleWidth, ROW_HEIGHT)
					.build());

			addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud.layout"),
							button -> Minecraft.getInstance().setScreen(new HudModuleScreen(this, module)))
					.bounds(left + toggleWidth + BUTTON_GAP, y,
							rowWidth - toggleWidth - BUTTON_GAP, ROW_HEIGHT)
					.build());
		}

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 32, 200, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 的 renderBackground 只收 GuiGraphics 一个参数
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("screen.sephiria_fantasy.hud.list_hint"),
				this.width / 2, 34, 0xFFB0B0B0);
	}

	@Override
	public void onClose() {
		HudConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreen(this.parent);
		}
		else {
			super.onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		// 布局时要能看见 HUD 实时更新，而且多人游戏里不该把世界冻住
		return false;
	}

	private static Component toggleLabel(String module, HudConfig.Element element) {
		return Component.translatable("screen.sephiria_fantasy.hud.toggle",
				Component.translatable("screen.sephiria_fantasy.hud." + module),
				Component.translatable(element.visible ? "gui.yes" : "gui.no"));
	}
}
