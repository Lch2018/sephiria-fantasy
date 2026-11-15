package com.sephiria.compat.torohealth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.client.damage.DamageDisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;

/**
 * 自定义 ToroHealth 伤害数字绘制：元素色 + 暴击 2 倍大小。
 *
 * <p>1.20.1 的 ToroHealth 在 {@code HealthChangeParticle#render} 里直接 {@code Font#drawInBatch}
 * 画字（没有 26.3 / 1.21 的 TextParticleRenderer 类），所以由粒子的渲染混入在 HEAD 处接管：
 * 自建 PoseStack（相机朝向 + 0.025 缩放 + 负 Y 翻转 + 居中），再走原版同一条批渲染管线。
 *
 * <p>亮度 / 大小 / 税前伤害读 0.5.x 的「伤害显示」设置页（{@link DamageDisplayConfig}）；
 * 颜色值沿用 26.3 版的元素色表。
 */
public final class ToroHealthTextRenderer {
	/** 与 ToroHealth 原渲染器一致的基准缩放。 */
	private static final float TOROHEALTH_BASE_SCALE = 0.025F;
	private ToroHealthTextRenderer() {
	}

	/** 设置页的税前伤害开关。 */
	public static boolean preTax() {
		return com.sephiria.client.damage.DamageDisplayConfig.preTax();
	}

	/** 设置页的亮度（乘在 RGB 上）。 */
	public static double brightness() {
		return com.sephiria.client.damage.DamageDisplayConfig.brightness();
	}

	public static void render(MultiBufferSource buffers, Quaternionf cameraRotation,
			float x, float y, float z, String text, int color, int light, boolean crit) {
		Font font = Minecraft.getInstance().font;
		float scale = TOROHEALTH_BASE_SCALE * (float) DamageDisplayConfig.size() * (crit ? 2.0F : 1.0F);
		int width = font.width(text);
		PoseStack matrices = new PoseStack();
		matrices.translate(x, y, z);
		matrices.mulPose(cameraRotation);
		// 必须与 ToroHealth 原渲染一样用「X、Y 两个负缩放」：只翻 Y 会把三角形的绕序反掉，
		// 文字被背面剔除，看起来就是「伤害数字跳不出来」。X 正缩放 + 居中是 26.x 的写法，
		// 1.20.1 的 Font 批渲染不是那套状态。
		matrices.scale(-scale, -scale, scale);
		matrices.translate(-width, -3.0F, 0.0F);
		font.drawInBatch(text, -width / 2.0F, -3.0F, color, false,
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
