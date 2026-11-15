package com.sephiria.mixin.client;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 放开 1.20.1 的元素旋转角度白名单。
 *
 * <p>原版 {@code BlockElement.Deserializer#getAngle} 只允许 -45/-22.5/0/22.5/45，
 * 但 {@code BlockElementRotation} 的数学本身支持任意角度；26.x 的匕首刃尖（-58°）、
 * 盾的角（-135°）、弩的护木（20°）都超过这个白名单，归一到 45° 会改变轮廓。
 * 这里直接读 JSON 里的 angle 返回，让这些模型保持 0.5.3.8 的原始角度。
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public class BlockElementDeserializerMixin {
	@Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
	private void sephiria$allowAnyAngle(JsonObject json, CallbackInfoReturnable<Float> callback) {
		if (json.has("angle")) {
			callback.setReturnValue(json.get("angle").getAsFloat());
		}
	}
}
