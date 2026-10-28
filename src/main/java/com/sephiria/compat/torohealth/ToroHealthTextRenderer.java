package com.sephiria.compat.torohealth;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;

/**
 * 自定义 ToroHealth 伤害数字绘制：元素色 + 暴击 2 倍大小。
 *
 * <p>1.21.1 里 ToroHealth 的浮动数字是「粒子 render → TextRenderQueue → WorldRenderEvents.LAST
 * 一次性画出来」的即时管线，不存在 26.3 那种提交队列被提前 drain 的问题，所以这里直接沿用
 * ToroHealth 原渲染器的变换（相机朝向 + 0.025 缩放 + 负 Y 翻转），只替换颜色、缩放和居中方式。
 *
 * <p>本移植版没有「伤害显示」设置页：亮度固定 1.0、大小固定 1.0、税前伤害固定关闭，
 * 所以这里只留常量；颜色值沿用 26.3 版的元素色表。
 */
public final class ToroHealthTextRenderer {
	/** 与 ToroHealth 原渲染器一致的基准缩放。 */
	private static final float TOROHEALTH_BASE_SCALE = 0.025F;
	/** 设置页缺失时的默认值：亮度 1.0（不调色）、大小 1.0、税前伤害关闭。 */
	public static final double DEFAULT_BRIGHTNESS = 1.0D;
	public static final double DEFAULT_SIZE = 1.0D;
	public static final boolean DEFAULT_PRE_TAX = false;

	private ToroHealthTextRenderer() {
	}

	/** 税前伤害开关（本移植版固定 false，保留接口方便以后接设置页）。 */
	public static boolean preTax() {
		return DEFAULT_PRE_TAX;
	}

	public static void render(MultiBufferSource buffers, Quaternionf cameraRotation,
			float x, float y, float z, String text, int color, int light, boolean crit) {
		Font font = Minecraft.getInstance().font;
		float scale = TOROHEALTH_BASE_SCALE * (float) DEFAULT_SIZE * (crit ? 2.0F : 1.0F);
		PoseStack matrices = new PoseStack();
		matrices.translate(x, y, z);
		matrices.mulPose(cameraRotation);
		matrices.scale(scale, -scale, scale);
		// 原渲染器把文字左端对齐到锚点，这里改成居中（与 26.3 版一致）
		matrices.translate(-font.width(text) / 2.0F, -font.lineHeight / 2.0F, 0.0F);

		font.drawInBatch(text, 0.0F, 0.0F, color, false,
				matrices.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
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
