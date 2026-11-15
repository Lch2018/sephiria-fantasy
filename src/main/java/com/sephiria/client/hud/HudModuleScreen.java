package com.sephiria.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的二级界面：X / Y / 缩放三个滑块 + 显示开关，同时可以直接拖动预览。
 *
 * <p>滑块改的是屏幕绝对像素，拖动预览实时更新同一份 {@link HudConfig.Element}；
 * 两者都会把模块夹在屏幕内。返回一级界面时写盘。
 */
public class HudModuleScreen extends Screen {
	private static final double SCALE_MIN = 0.5D;
	private static final double SCALE_MAX = 2.0D;

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
		int y = this.height / 4;

		addRenderableWidget(new PositionSlider(true, left, y, 200, 20, 0.0D, this.width - 20.0D));
		y += 24;
		addRenderableWidget(new PositionSlider(false, left, y, 200, 20, 0.0D, this.height - 20.0D));
		y += 24;
		addRenderableWidget(new ScaleSlider(left, y, 200, 20));
		y += 24;
		addRenderableWidget(Button.builder(visibleLabel(), button -> {
			this.element.visible = !this.element.visible;
			button.setMessage(visibleLabel());
		}).bounds(left, y, 200, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> back())
				.bounds(left, this.height - 40, 200, 20)
				.build());

		// 上次退出时分辨率可能比现在大，进界面先夹一次
		clampElement();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);

		int width = previewWidth();
		int height = previewHeight();
		int x = (int) Math.round(this.element.x);
		int y = (int) Math.round(this.element.y);
		int scaledWidth = Math.max(1, (int) Math.round(width * this.element.scale));
		int scaledHeight = Math.max(1, (int) Math.round(height * this.element.scale));

		// 预览框：拖动它移动 HUD；隐藏时边框压暗
		extractor.fill(x - 1, y - 1, x + scaledWidth + 1, y + scaledHeight + 1,
				this.element.visible ? 0xFFE8E8E8 : 0xFF707070);
		extractor.fill(x, y, x + scaledWidth, y + scaledHeight, 0x80101010);
		extractor.text(this.font, this.title, x + 3, y + 3,
				this.element.visible ? 0xFFFFFFFF : 0xFFA0A0A0, false);

		Component hint = Component.translatable("screen.sephiria_fantasy.hud.drag_hint");
		extractor.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, 26, 0xFFB0B0B0, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// 先让按钮 / 滑块吃掉点击，避免它们压在预览上时一点就开始拖
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}

		if (insidePreview(event.x(), event.y())) {
			// 从夹取后的实际位置开始拖，避免配置坐标被屏幕边缘夹过时预览跳一下
			this.element.x = Math.max(0.0D, Math.min(this.element.x,
					this.width - previewWidth() * this.element.scale));
			this.element.y = Math.max(0.0D, Math.min(this.element.y,
					this.height - previewHeight() * this.element.scale));
			this.dragging = true;
			this.grabX = event.x() - this.element.x;
			this.grabY = event.y() - this.element.y;
			this.element.centered = false;
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging) {
			this.element.x = event.x() - this.grabX;
			this.element.y = event.y() - this.grabY;
			clampElement();
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging) {
			this.dragging = false;
			HudConfig.save();
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean isPauseScreen() {
		// 拖动时 HUD 还在后面实时渲染，冻结世界没有必要
		return false;
	}

	private boolean insidePreview(double mouseX, double mouseY) {
		int width = Math.max(1, (int) Math.round(previewWidth() * this.element.scale));
		int height = Math.max(1, (int) Math.round(previewHeight() * this.element.scale));
		return mouseX >= this.element.x && mouseX <= this.element.x + width
				&& mouseY >= this.element.y && mouseY <= this.element.y + height;
	}

	/** 预览框的估算尺寸：图标行按 9 格，其余按一行文字面板。 */
	private int previewWidth() {
		return switch (this.module) {
			case HudConfig.SKILLS, HudConfig.ARTIFACTS -> 164;
			case HudConfig.INVULN -> HudConfig.INVULN_WIDTH;
			case HudConfig.WEAPON -> 150;
			default -> 120;
		};
	}

	private int previewHeight() {
		return switch (this.module) {
			case HudConfig.SKILLS, HudConfig.ARTIFACTS -> 20;
			case HudConfig.INVULN -> 9;
			case HudConfig.WEAPON -> 22;
			default -> 18;
		};
	}

	private void clampElement() {
		HudConfig.clamp(this.element, this.width, this.height, previewWidth(), previewHeight());
	}

	private Component visibleLabel() {
		return Component.translatable("screen.sephiria_fantasy.hud.visible",
				Component.translatable(this.element.visible ? "gui.yes" : "gui.no"));
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
				// 手动拖过 X 就取消自动居中，否则拖不走无敌条
				HudModuleScreen.this.element.centered = false;
			}
			else {
				HudModuleScreen.this.element.y = currentValue();
			}

			clampElement();
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
					Component.literal(String.format(java.util.Locale.ROOT, "%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			HudConfig.Element element = HudModuleScreen.this.element;
			double previousScale = element.scale;
			double center = element.x + HudConfig.INVULN_WIDTH * previousScale / 2.0D;
			element.scale = currentValue();

			// 无敌条：缩放围绕「中心」而不是左上角，中心位置保持不变
			if (HudConfig.INVULN.equals(HudModuleScreen.this.module)) {
				element.x = center - HudConfig.INVULN_WIDTH * element.scale / 2.0D;
			}

			clampElement();
		}

		private double currentValue() {
			return SCALE_MIN + (SCALE_MAX - SCALE_MIN) * this.value;
		}
	}
}
