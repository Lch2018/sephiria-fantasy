package com.sephiria.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD 布局设置的二级界面：拖动该模块的预览调整位置，- / + 调整缩放，按钮切换显示。
 *
 * <p>预览画的就是 {@link SephiriaHud} 正式渲染用的同一套图标与布局，所以拖到哪里、缩到多少
 * 在游戏里就是什么样。坐标用鼠标拖拽的屏幕坐标，并在每次移动 / 缩放后夹进屏幕
 * （{@link HudConfig#clamp}）；返回一级界面或按 Esc 时写盘。
 */
public class HudModuleScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final int BOTTOM_ROW_GAP = 26;

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
		int rowY = this.height - 32 - BOTTOM_ROW_GAP;

		addRenderableWidget(Button.builder(Component.literal("-"),
						button -> changeScale(-HudConfig.SCALE_STEP))
				.bounds(left, rowY, 20, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("+"),
						button -> changeScale(HudConfig.SCALE_STEP))
				.bounds(left + 24, rowY, 20, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(visibleLabel(), button -> {
					this.element.visible = !this.element.visible;
					button.setMessage(visibleLabel());
					HudConfig.save();
				})
				.bounds(left + 118, rowY, 100, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(left, this.height - 32, 200, BUTTON_HEIGHT)
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
		graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.hud.scale_label",
						Math.round(this.element.scale * 100.0D) + "%"),
				this.width / 2 - 50, this.height - 32 - BOTTOM_ROW_GAP + 6, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// 先让按钮吃掉点击，避免按钮压在预览上时一点就开始拖
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

	private void changeScale(double delta) {
		this.element.scale = HudConfig.clampScale(this.element.scale + delta);
		clampElement();
	}

	private void clampElement() {
		SephiriaHud.Layout layout = SephiriaHud.layout(this.module);
		HudConfig.clamp(this.element, this.width, this.height, layout.width(), layout.height());
	}

	private Component visibleLabel() {
		return Component.translatable("screen.sephiria_fantasy.hud.visible",
				Component.translatable(this.element.visible ? "gui.yes" : "gui.no"));
	}
}
