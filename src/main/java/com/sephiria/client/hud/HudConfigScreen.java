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
 *
 * <p>模块到 9 个以后单列放不下 240 高的 GUI，所以超过 5 行时自动排两列。
 */
public class HudConfigScreen extends Screen {
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int BUTTON_GAP = 8;
	private static final int LAYOUT_WIDTH = 52;
	/** 超过这个行数就排两列，免得在 240 高的 GUI 上把「完成」按钮挤出屏幕。 */
	private static final int SINGLE_COLUMN_ROWS = 5;

	private final Screen parent;

	public HudConfigScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.hud"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int count = HudConfig.MODULES.size();
		int columns = count > SINGLE_COLUMN_ROWS ? 2 : 1;
		// GUI 宽度最小 320，但留一点余量，别在极端分辨率下把按钮挤出屏幕
		int columnWidth = Math.min(ROW_WIDTH, (this.width - 16 - BUTTON_GAP * (columns - 1)) / columns);
		int toggleWidth = Math.max(40, columnWidth - LAYOUT_WIDTH - BUTTON_GAP);
		int left = (this.width - (columnWidth * columns + BUTTON_GAP * (columns - 1))) / 2;
		int top = 52;

		for (int index = 0; index < count; index++) {
			String module = HudConfig.MODULES.get(index);
			HudConfig.Element element = HudConfig.element(module);
			int column = index % columns;
			int row = index / columns;
			int rowX = left + column * (columnWidth + BUTTON_GAP);
			int y = top + row * (ROW_HEIGHT + ROW_GAP);

			addRenderableWidget(Button.builder(toggleLabel(module, element), button -> {
						element.visible = !element.visible;
						button.setMessage(toggleLabel(module, element));
						HudConfig.save();
					})
					.bounds(rowX, y, toggleWidth, ROW_HEIGHT)
					.build());

			addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud.layout"),
							button -> Minecraft.getInstance().setScreen(new HudModuleScreen(this, module)))
					.bounds(rowX + toggleWidth + BUTTON_GAP, y, LAYOUT_WIDTH, ROW_HEIGHT)
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
