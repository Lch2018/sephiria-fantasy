package com.sephiria.client.config;

import com.sephiria.client.damage.DamageDisplayConfig;
import com.sephiria.client.hud.HudConfig;
import com.sephiria.client.hud.HudConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「赛菲莉娅 Mod 设置」一级界面：HUD 设置与伤害显示。
 *
 * <p>H 键现在打开这里；HUD 布局设置从原来的直接入口移到本页的「HUD 设置」。
 */
public class SephiriaSettingsScreen extends Screen {
	private final Screen parent;

	public SephiriaSettingsScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = 50;

		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.settings.hud"),
						button -> Minecraft.getInstance().setScreenAndShow(new HudConfigScreen(this)))
				.bounds(left, y, 200, 20)
				.build());
		y += 24;
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.settings.damage_display"),
						button -> Minecraft.getInstance().setScreenAndShow(new DamageDisplayScreen(this)))
				.bounds(left, y, 200, 20)
				.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(left, this.height - 40, 200, 20)
				.build());
	}

	@Override
	public void onClose() {
		HudConfig.save();
		DamageDisplayConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreenAndShow(this.parent);
		} else {
			super.onClose();
		}
	}
}
