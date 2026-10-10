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
		int left = this.width / 2 - 100;
		int y = 40;

		for (String key : HudConfig.MODULES) {
			addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.hud." + key),
							button -> Minecraft.getInstance().setScreenAndShow(new HudModuleScreen(this, key)))
					.bounds(left, y, 200, 20)
					.build());
			y += 24;
		}

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(left, this.height - 40, 200, 20)
				.build());
	}

	@Override
	public void onClose() {
		HudConfig.save();
		super.onClose();
	}
}
