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
						button -> Minecraft.getInstance().setScreen(new HudConfigScreen(this)))
				.bounds(left, y, 200, 20)
				.build());
		y += 24;
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.settings.damage_display"),
						button -> Minecraft.getInstance().setScreen(new DamageDisplayScreen(this)))
				.bounds(left, y, 200, 20)
				.build());

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(left, this.height - 40, 200, 20)
				.build());
	}

	@Override
	public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 的 Screen.render 不铺背景、也不画标题
		this.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		HudConfig.save();
		DamageDisplayConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreen(this.parent);
		} else {
			super.onClose();
		}
	}
}
