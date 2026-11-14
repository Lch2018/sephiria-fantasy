package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「统一调整」：用一个滑块同时改所有 HUD 模块的缩放。
 *
 * <p>移动滑块即时生效（HUD 每次渲染都从 {@link HudConfig} 读），返回时统一写盘。
 * 只改缩放，不碰各模块的位置。
 */
public class HudUniformScaleScreen extends Screen {
	private static final double SCALE_MIN = 0.5D;
	private static final double SCALE_MAX = 2.0D;

	private final Screen parent;

	public HudUniformScaleScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.hud.uniform"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = this.height / 4;
		addRenderableWidget(new UniformScaleSlider(left, y, 200, 20));
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> back())
				.bounds(left, this.height - 40, 200, 20)
				.build());
	}

	private void back() {
		HudConfig.save();
		Minecraft.getInstance().setScreenAndShow(this.parent);
	}

	@Override
	public void onClose() {
		back();
	}

	private static final class UniformScaleSlider extends AbstractSliderButton {
		UniformScaleSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(),
					(HudConfig.averageScale() - SCALE_MIN) / (SCALE_MAX - SCALE_MIN));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("screen.sephiria_fantasy.hud.uniform_scale",
					Component.literal(String.format("%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			HudConfig.applyScaleToAll(currentValue());
		}

		private double currentValue() {
			return SCALE_MIN + (SCALE_MAX - SCALE_MIN) * this.value;
		}
	}
}
