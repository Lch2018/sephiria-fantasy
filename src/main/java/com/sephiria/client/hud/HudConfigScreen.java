package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的一级界面：列出所有 HUD 模块，点进去才是该模块自己的设置界面。
 *
 * <p>模块清单在 {@link HudConfig#MODULES} 统一登记，所以这个界面和模块界面都不需要
 * 因为新增模块而改动——模块变多也只是这里多一行按钮。
 */
public class HudConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 20;
	private static final int COLUMN_GAP = 8;

	private final Screen parent;

	public HudConfigScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.hud"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		// 最上方是「统一调整」：一次改所有模块的缩放；下面是各模块自己的位置 / 缩放。
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud.uniform"),
						button -> Minecraft.getInstance().setScreen(new HudUniformScaleScreen(this)))
				.bounds(this.width / 2 - 100, 30, 200, ROW_HEIGHT)
				.build());

		// 模块到 11 个以后单列放不下：超过 7 个就自动排两列
		int columns = HudConfig.MODULES.size() > 7 ? 2 : 1;
		int width = columns == 1 ? 200 : 150;
		int left = Math.max(8, this.width / 2 - (columns * width + (columns - 1) * COLUMN_GAP) / 2);
		int y = 56;

		for (int index = 0; index < HudConfig.MODULES.size(); index++) {
			String key = HudConfig.MODULES.get(index);
			int column = index % columns;
			int row = index / columns;
			int x = left + column * (width + COLUMN_GAP);
			int top = y + row * 24;
			addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud." + key),
							button -> Minecraft.getInstance().setScreen(new HudModuleScreen(this, key)))
					.bounds(x, top, width, ROW_HEIGHT)
					.build());
		}

		int bottom = this.height - 40;
		int half = 98;
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud.reset"),
						button -> HudConfig.resetAll())
				.bounds(this.width / 2 - 100, bottom, half, ROW_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(this.width / 2 - 100 + half + 4, bottom, half, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 的 Screen 不会自动画标题，26.x 那套是基类画的
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		HudConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreen(this.parent);
		} else {
			super.onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		// 布局时要能看见 HUD 实时更新，而且多人游戏里不该把世界冻住
		return false;
	}
}
