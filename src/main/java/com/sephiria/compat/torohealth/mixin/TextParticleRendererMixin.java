package com.sephiria.compat.torohealth.mixin;

import com.sephiria.client.damage.DamageDisplayConfig;
import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthTextRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Quaternionfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 ToroHealth 的浮动数字，把本模组的标签条目交给自定义渲染器。
 *
 * <p>ToroHealth 原渲染固定 0.025 缩放、整段单色；这里改为读取「赛菲莉娅 Mod 设置 → 伤害显示」
 * 的亮度 / 大小 / 税前伤害，并用本模组元素色（双属性取面板值较高一侧）重画。
 * 非本模组条目不带标签，直接放行给 ToroHealth 原方法。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.TextParticleRenderer")
public abstract class TextParticleRendererMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("SephiriaToroHealth");
	private static int logs;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void sephiria$render(String text, Quaternionfc cameraRotation,
			float x, float y, float z, float u, float v, int color, SubmitNodeCollector queue,
			int light, CallbackInfo callback) {
		ToroHealthCompatClient.ParsedTag tag = ToroHealthCompatClient.parseTag(text);

		if (tag != null && logs < 10) {
			logs++;
			LOGGER.info("[SEPHIRIA] renderer text='{}', tag={}", text, tag);
		}

		if (tag == null) {
			return;
		}

		int primary = ToroHealthTextRenderer.brighten(color, DamageDisplayConfig.brightness());

		// 双属性也按单色显示：primary 是面板值较高那一侧的元素色。
		ToroHealthTextRenderer.render(queue, cameraRotation, x, y, z, tag.text(),
				primary, light, tag.crit());
		callback.cancel();
	}
}
