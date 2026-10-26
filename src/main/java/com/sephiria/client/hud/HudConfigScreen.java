package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
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
	private final Screen parent;

	public HudConfigScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.hud"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		// 模块多了以后单列在 240 高的 GUI 上放不下：超过 7 个就自动排两列
		int columns = HudConfig.MODULES.size() > 7 ? 2 : 1;
		int width = columns == 1 ? 200 : 150;
		int gap = 8;
		int left = Math.max(8, this.width / 2 - (columns * width + (columns - 1) * gap) / 2);
		int y = 30;

		for (int index = 0; index < HudConfig.MODULES.size(); index++) {
			String key = HudConfig.MODULES.get(index);
			int column = index % columns;
			int row = index / columns;
			int x = left + column * (width + gap);
			int top = y + row * 24;
			addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud." + key),
							button -> Minecraft.getInstance().setScreenAndShow(new HudModuleScreen(this, key)))
					.bounds(x, top, width, 20)
					.build());
		}

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 40, 200, 20)
				.build());
	}

	@Override
	public void onClose() {
		HudConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreenAndShow(this.parent);
		} else {
			super.onClose();
		}
	}
}
