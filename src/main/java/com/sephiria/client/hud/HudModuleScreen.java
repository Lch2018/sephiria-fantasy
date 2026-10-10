package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的二级界面：调整某一个模块的位置与缩放。
 *
 * <p>三个滑块——X、Y 是屏幕绝对像素，缩放 0.5~2.0。改动实时生效（HUD 直接从
 * {@link HudConfig} 读），返回一级界面时写盘。
 */
public class HudModuleScreen extends Screen {
	private static final double SCALE_MIN = 0.5D;
	private static final double SCALE_MAX = 2.0D;

	private final Screen parent;
	private final String module;
	private final HudConfig.Element element;

	public HudModuleScreen(Screen parent, String module) {
		super(Component.translatable("screen.sephiria_fantasy.hud." + module));
		this.parent = parent;
		this.module = module;
		this.element = HudConfig.element(module);
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = this.height / 4;

		addRenderableWidget(new PositionSlider(true, left, y, 200, 20, 0.0D, this.width - 20.0D));
		y += 24;
		addRenderableWidget(new PositionSlider(false, left, y, 200, 20, 0.0D, this.height - 20.0D));
		y += 24;
		addRenderableWidget(new ScaleSlider(left, y, 200, 20));

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
		// Esc 视作返回上一级，而不是直接回到游戏
		back();
	}

	/** 位置滑块：值域是屏幕像素。 */
	private final class PositionSlider extends AbstractSliderButton {
		private final boolean horizontal;
		private final double min;
		private final double max;

		PositionSlider(boolean horizontal, int x, int y, int width, int height, double min, double max) {
			super(x, y, width, height, Component.empty(),
					((horizontal ? HudModuleScreen.this.element.x : HudModuleScreen.this.element.y) - min) / (max - min));
			this.horizontal = horizontal;
			this.min = min;
			this.max = max;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable(this.horizontal ? "screen.sephiria_fantasy.hud.x" : "screen.sephiria_fantasy.hud.y",
					Component.translatable("screen.sephiria_fantasy.hud." + HudModuleScreen.this.module),
					Component.literal(String.valueOf((int) Math.round(currentValue())))));
		}

		@Override
		protected void applyValue() {
			if (this.horizontal) {
				HudModuleScreen.this.element.x = currentValue();
			}
			else {
				HudModuleScreen.this.element.y = currentValue();
			}
		}

		private double currentValue() {
			return this.min + (this.max - this.min) * this.value;
		}
	}

	/** 缩放滑块：0.5 ~ 2.0。 */
	private final class ScaleSlider extends AbstractSliderButton {
		ScaleSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(),
					(HudModuleScreen.this.element.scale - SCALE_MIN) / (SCALE_MAX - SCALE_MIN));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("screen.sephiria_fantasy.hud.scale",
					Component.translatable("screen.sephiria_fantasy.hud." + HudModuleScreen.this.module),
					Component.literal(String.format("%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			HudModuleScreen.this.element.scale = currentValue();
		}

		private double currentValue() {
			return SCALE_MIN + (SCALE_MAX - SCALE_MIN) * this.value;
		}
	}
}
