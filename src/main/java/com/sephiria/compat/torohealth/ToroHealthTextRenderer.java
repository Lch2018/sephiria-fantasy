package com.sephiria.compat.torohealth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.client.damage.DamageDisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义 ToroHealth 伤害数字绘制：单色、亮度、大小与暴击 2 倍。
 *
 * <p>直接走原版 {@code submitText}（与 ToroHealth 原渲染器同一条管线），只替换颜色、
 * 缩放与位置；不再自造逐字形几何。26.3 的文本管线对自定义顶点几何不稳定，单色需求下
 * 走原版文本提交最可靠。
 *
 * <p>双属性统一用主元素色——面板值较高的一边。
 */
public final class ToroHealthTextRenderer {
	private static final float TOROHEALTH_BASE_SCALE = 0.025F;
	private static final Logger LOGGER = LoggerFactory.getLogger("SephiriaToroHealth");
	private static int logs;

	private static void log(String text, int color) {
		if (logs < 10) {
			logs++;
			LOGGER.info("[SEPHIRIA] floating text='{}', color=#{}, alpha={}",
					text, String.format("%06X", color & 0xFFFFFF), (color >>> 24) & 0xFF);
		}
	}

	private ToroHealthTextRenderer() {
	}

	public static void render(SubmitNodeCollector queue, Quaternionfc cameraRotation,
			float x, float y, float z, String text, int color, int light, boolean crit) {
		Font font = Minecraft.getInstance().font;
		log(text, color);
		float scale = TOROHEALTH_BASE_SCALE * (float) DamageDisplayConfig.size() * (crit ? 2.0F : 1.0F);
		PoseStack matrices = new PoseStack();
		matrices.translate(x, y, z);
		matrices.rotate(cameraRotation);
		matrices.scale(scale, -scale, scale);
		matrices.translate(-font.width(text) / 2.0F, -font.lineHeight / 2.0F, 0.0F);

		queue.submitText(matrices, 0.0F, 0.0F,
				Component.literal(text).getVisualOrderText(), false,
				Font.DisplayMode.NORMAL, light, color, 0, 0);
	}

	/** 亮度：乘在 RGB 上，alpha 保留（粒子淡出仍生效）。 */
	public static int brighten(int color, double factor) {
		int alpha = color & 0xFF000000;
		int red = clampChannel(((color >> 16) & 0xFF) * factor);
		int green = clampChannel(((color >> 8) & 0xFF) * factor);
		int blue = clampChannel((color & 0xFF) * factor);
		return alpha | (red << 16) | (green << 8) | blue;
	}

	private static int clampChannel(double value) {
		return Math.max(0, Math.min(255, (int) Math.round(value)));
	}
}
