package com.sephiria.mixin.client;

import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthTextRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 ToroHealth 的浮动数字，把带本模组标签的条目交给自定义渲染器。
 *
 * <p>1.5.1 的 {@code TextParticleRenderer#render} 固定 0.025 缩放、整段单色；这里改为用本模组
 * 元素色重画，暴击放大 2 倍。颜色由服务端算好（双属性取面板值较高的一边），客户端只负责
 * 剥标签、套亮度和放大。非本模组条目不带标签，直接放行给 ToroHealth 原方法。
 * 绘制走原版 {@code Font#drawInBatch}，与 ToroHealth 原渲染器同一条管线。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.TextParticleRenderer")
public abstract class TextParticleRendererMixin {
	@Inject(method = "render(Ljava/lang/String;Lnet/minecraft/client/Camera;FFFFFILnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void sephiria$render(String text, Camera camera, float x, float y, float z,
			float u, float v, int color, MultiBufferSource buffers, int light, CallbackInfo callback) {
		ToroHealthCompatClient.ParsedTag tag = ToroHealthCompatClient.parseTag(text);

		if (tag == null) {
			return;
		}

		int primary = ToroHealthTextRenderer.brighten(color, ToroHealthTextRenderer.DEFAULT_BRIGHTNESS);

		// 双属性也按单色显示：primary 是面板值较高那一侧的元素色。
		ToroHealthTextRenderer.render(buffers, camera.rotation(), x, y, z, tag.text(),
				primary, light, tag.crit());
		callback.cancel();
	}
}
