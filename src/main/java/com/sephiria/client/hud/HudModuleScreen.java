package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的二级界面：X / Y / 缩放三个滑块 + 显示开关，同时保留拖动预览。
 *
 * <p>0.5.3.8 的口径是「拖动移动 + X/Y 精准调整条」：滑块改的是屏幕绝对像素，
 * 拖动预览仍然实时生效；两者改的是同一份 {@link HudConfig.Element}，返回一级界面时写盘。
 */
public class HudModuleScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;

	private final Screen parent;
	private final String module;
	private final HudConfig.Element element;

	private boolean dragging;
	private double grabX;
	private double grabY;

	public HudModuleScreen(Screen parent, String module) {
		super(Component.translatable("screen.sephiria_fantasy.hud." + module));
		this.parent = parent;
		this.module = module;
		this.element = HudConfig.element(module);
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int top = this.height / 4;

		addRenderableWidget(new PositionSlider(true, left, top, 200, BUTTON_HEIGHT, 0.0D, this.width - 20.0D));
		addRenderableWidget(new PositionSlider(false, left, top + 24, 200, BUTTON_HEIGHT,
				0.0D, this.height - 20.0D));
		addRenderableWidget(new ScaleSlider(left, top + 48, 200, BUTTON_HEIGHT));
		addRenderableWidget(Button.builder(visibleLabel(), button -> {
					this.element.visible = !this.element.visible;
					button.setMessage(visibleLabel());
				})
				.bounds(left, top + 72, 200, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left, this.height - 40, 200, BUTTON_HEIGHT)
				.build());

		// 位置可能因为上次退出时的分辨率比现在大而落在屏幕外，进界面先夹一次
		clampElement();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 的 renderBackground 只收 GuiGraphics 一个参数
		this.renderBackground(graphics);
		SephiriaHud.renderPreview(graphics, this.module);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, Component.translatable("screen.sephiria_fantasy.hud.drag_hint"),
				this.width / 2, 26, 0xFFB0B0B0);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// 先让按钮 / 滑块吃掉点击，避免它们压在预览上时一点就开始拖
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		SephiriaHud.Layout layout = SephiriaHud.layout(this.module);

		if (button == 0 && layout.contains(mouseX, mouseY)) {
			// 从夹取后的实际位置开始拖：配置里的坐标可能被屏幕边缘夹过，直接接管会让预览跳一下
			this.element.x = layout.x();
			this.element.y = layout.y();
			this.dragging = true;
			this.grabX = mouseX - layout.x();
			this.grabY = mouseY - layout.y();
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.dragging) {
			this.element.x = mouseX - this.grabX;
			this.element.y = mouseY - this.grabY;
			clampElement();
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.dragging) {
			this.dragging = false;
			HudConfig.save();
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
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
		// 拖动时 HUD 还在后面实时渲染，冻结世界没有必要
		return false;
	}

	private void clampElement() {
		SephiriaHud.Layout layout = SephiriaHud.layout(this.module);
		HudConfig.clamp(this.element, this.width, this.height, layout.width(), layout.height());
	}

	private Component visibleLabel() {
		return Component.translatable("screen.sephiria_fantasy.hud.visible",
				Component.translatable(this.element.visible ? "gui.yes" : "gui.no"));
	}

	/** X / Y 位置滑块：值域是屏幕像素。 */
	private final class PositionSlider extends AbstractSliderButton {
		private final boolean horizontal;
		private final double min;
		private final double max;

		PositionSlider(boolean horizontal, int x, int y, int width, int height, double min, double max) {
			super(x, y, width, height, Component.empty(),
					((horizontal ? HudModuleScreen.this.element.x : HudModuleScreen.this.element.y) - min)
							/ (max - min));
			this.horizontal = horizontal;
			this.min = min;
			this.max = max;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable(
					this.horizontal ? "screen.sephiria_fantasy.hud.x" : "screen.sephiria_fantasy.hud.y",
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

			HudModuleScreen.this.clampElement();
		}

		private double currentValue() {
			return this.min + (this.max - this.min) * this.value;
		}
	}

	/** 缩放滑块：0.5 ~ 2.0。 */
	private final class ScaleSlider extends AbstractSliderButton {
		ScaleSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(),
					(HudModuleScreen.this.element.scale - HudConfig.SCALE_MIN)
							/ (HudConfig.SCALE_MAX - HudConfig.SCALE_MIN));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("screen.sephiria_fantasy.hud.scale",
					Component.translatable("screen.sephiria_fantasy.hud." + HudModuleScreen.this.module),
					Component.literal(String.format(java.util.Locale.ROOT, "%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			HudModuleScreen.this.element.scale = currentValue();
			HudModuleScreen.this.clampElement();
		}

		private double currentValue() {
			return HudConfig.SCALE_MIN + (HudConfig.SCALE_MAX - HudConfig.SCALE_MIN) * this.value;
		}
	}
}
