package com.sephiria.client.config;

import com.sephiria.client.damage.DamageDisplayConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「赛菲莉娅 Mod 设置 → 伤害显示」：亮度、大小、税前伤害。
 *
 * <p>这些值只对装了 ToroHealth 时、由本模组伤害生成的浮动数字生效。页面顶部固定显示
 * 「需要 ToroHealth」；没装时设置仍可保存，但不会有可见效果。
 */
public class DamageDisplayScreen extends Screen {
	private static final int LABEL_Y = 28;
	private static final int FIRST_SLIDER_Y = 58;
	private static final int SLIDER_GAP = 24;

	private final Screen parent;

	public DamageDisplayScreen(Screen parent) {
		super(Component.translatable("screen.sephiria_fantasy.damage_display"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = FIRST_SLIDER_Y;

		addRenderableWidget(new ValueSlider(left, y, 200, 20, true));
		y += SLIDER_GAP;
		addRenderableWidget(new ValueSlider(left, y, 200, 20, false));
		y += SLIDER_GAP;
		addRenderableWidget(Button.builder(preTaxLabel(), button -> {
			DamageDisplayConfig.setPreTax(!DamageDisplayConfig.preTax());
			button.setMessage(preTaxLabel());
		}).bounds(left, y, 200, 20).build());

		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> back())
				.bounds(left, this.height - 40, 200, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		Component required = Component.translatable("screen.sephiria_fantasy.damage_display.requires_torohealth");
		int color = FabricLoader.getInstance().isModLoaded("torohealth") ? 0xFFAAAAAA : 0xFFFF5555;
		extractor.text(this.font, required, this.width / 2 - this.font.width(required) / 2, LABEL_Y, color, true);
	}

	private Component preTaxLabel() {
		return Component.translatable("screen.sephiria_fantasy.damage_display.pre_tax", onOff(DamageDisplayConfig.preTax()));
	}

	private static Component onOff(boolean value) {
		return Component.translatable("screen.sephiria_fantasy.damage_display." + (value ? "on" : "off"));
	}

	private void back() {
		DamageDisplayConfig.save();

		if (this.parent != null) {
			Minecraft.getInstance().setScreenAndShow(this.parent);
		} else {
			super.onClose();
		}
	}

	@Override
	public void onClose() {
		back();
	}

	/** 亮度 / 大小滑块。 */
	private final class ValueSlider extends AbstractSliderButton {
		private final boolean brightness;

		ValueSlider(int x, int y, int width, int height, boolean brightness) {
			super(x, y, width, height, Component.empty(),
					normalize(brightness ? DamageDisplayConfig.brightness() : DamageDisplayConfig.size(), brightness));
			this.brightness = brightness;
			updateMessage();
		}

		private static double normalize(double value, boolean brightness) {
			double min = brightness ? DamageDisplayConfig.BRIGHTNESS_MIN : DamageDisplayConfig.SIZE_MIN;
			double max = brightness ? DamageDisplayConfig.BRIGHTNESS_MAX : DamageDisplayConfig.SIZE_MAX;
			return (value - min) / (max - min);
		}

		@Override
		protected void updateMessage() {
			String key = this.brightness
					? "screen.sephiria_fantasy.damage_display.brightness"
					: "screen.sephiria_fantasy.damage_display.size";
			setMessage(Component.translatable(key, Component.literal(String.format("%.2f", currentValue()))));
		}

		@Override
		protected void applyValue() {
			if (this.brightness) {
				DamageDisplayConfig.setBrightness(currentValue());
			} else {
				DamageDisplayConfig.setSize(currentValue());
			}
		}

		private double currentValue() {
			double min = this.brightness ? DamageDisplayConfig.BRIGHTNESS_MIN : DamageDisplayConfig.SIZE_MIN;
			double max = this.brightness ? DamageDisplayConfig.BRIGHTNESS_MAX : DamageDisplayConfig.SIZE_MAX;
			return min + (max - min) * this.value;
		}
	}
}
