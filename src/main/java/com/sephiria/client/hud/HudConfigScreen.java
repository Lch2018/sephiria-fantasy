package com.sephiria.client.hud;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置界面：每个元素三个滑块——X、Y、缩放。
 *
 * <p>坐标是屏幕绝对像素，缩放 0.5~2.0。改动实时生效（HUD 直接从 {@link HudConfig} 读），
 * 关闭界面时写盘。界面打开期间 HUD 仍然绘制，方便对着调。
 */
public class HudConfigScreen extends Screen {
	private final Screen parent;

	public HudConfigScreen(Screen parent) {
		super(Component.translatable("screen.sephiria.hud"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height / 4;
		int step = 24;

		for (String key : new String[]{HudConfig.WEAPON, HudConfig.DASH}) {
			HudConfig.Element element = HudConfig.element(key);

			addRenderableWidget(slider(element, true, 0.0D, this.width - 20.0D, y, key));
			y += step;
			addRenderableWidget(slider(element, false, 0.0D, this.height - 20.0D, y, key));
			y += step;
			addRenderableWidget(new ScaleSlider(element, key, this.width / 2 - 100, y, 200, 20));
			y += step + 8;
		}

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(this.width / 2 - 100, this.height - 40, 200, 20)
				.build());
	}

	private AbstractSliderButton slider(HudConfig.Element element, boolean horizontal, double min, double max, int y, String key) {
		double value = horizontal ? element.x : element.y;

		return new PositionSlider(element, horizontal, key, this.width / 2 - 100, y, 200, 20, min, max, value);
	}

	@Override
	public void onClose() {
		HudConfig.save();
		// 关闭本身交给基类（26.2 里 Minecraft 的 setScreen 已经改名，别自己调）
		super.onClose();
	}

	/** 位置滑块：值域是屏幕像素。 */
	private static final class PositionSlider extends AbstractSliderButton {
		private final HudConfig.Element element;
		private final boolean horizontal;
		private final double min;
		private final double max;
		private final String key;

		PositionSlider(HudConfig.Element element, boolean horizontal, String key, int x, int y, int width, int height, double min, double max, double value) {
			super(x, y, width, height, Component.empty(), (value - min) / (max - min));
			this.element = element;
			this.horizontal = horizontal;
			this.key = key;
			this.min = min;
			this.max = max;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable(this.horizontal ? "screen.sephiria.hud.x" : "screen.sephiria.hud.y",
					Component.translatable("screen.sephiria.hud." + this.key),
					Component.literal(String.valueOf((int) Math.round(currentValue())))));
		}

		@Override
		protected void applyValue() {
			if (this.horizontal) {
				this.element.x = currentValue();
			}
			else {
				this.element.y = currentValue();
			}
		}

		private double currentValue() {
			return this.min + (this.max - this.min) * this.value;
		}
	}

	/** 缩放滑块：0.5 ~ 2.0。 */
	private static final class ScaleSlider extends AbstractSliderButton {
		private static final double MIN = 0.5D;
		private static final double MAX = 2.0D;

		private final HudConfig.Element element;
		private final String key;

		ScaleSlider(HudConfig.Element element, String key, int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), (element.scale - MIN) / (MAX - MIN));
			this.element = element;
			this.key = key;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("screen.sephiria.hud.scale",
					Component.translatable("screen.sephiria.hud." + this.key),
					Component.literal(String.format("%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			this.element.scale = currentValue();
		}

		private double currentValue() {
			return MIN + (MAX - MIN) * this.value;
		}
	}
}
