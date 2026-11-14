package com.sephiria.mixin.client;

import com.sephiria.client.ClientDebuffs;
import com.sephiria.debuff.Debuffs;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被冰冻（眩晕）的敌人染成<b>冰蓝色</b>。
 *
 * <p>1.20.1 没有 26.x 的渲染状态与 {@code getModelTint}，所以改用渲染入口的
 * {@code RenderSystem.setShaderColor}：实体着色器的 COLOR_MODULATOR 就是这里设的乘算色，
 * 在 {@code LivingEntityRenderer.render} 前后压上 / 还原冰蓝 #8FDCFF 即可。
 *
 * <p>与 26.x 那版的边界一致：只染本体与它的 RenderLayer（盔甲 / 手持物），
 * 其它实体不受影响；渲染入口没有嵌套调用，所以保存一份进入前的颜色就够。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class FrozenTintMixin {
	/** 冰蓝：白 + 一点蓝，乘上去就是「冻住」的观感（红通道压到 0.56）。 */
	private static final float ICE_R = 0x8F / 255.0F;
	private static final float ICE_G = 0xDC / 255.0F;
	private static final float ICE_B = 0xFF / 255.0F;

	@Unique
	private float[] sephiria$previousColor;

	@Inject(method = "render", at = @At("HEAD"))
	private void sephiria$tintFrozen(LivingEntity entity, float yaw, float partialTick, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		this.sephiria$previousColor = null;

		if (entity != null && ClientDebuffs.has(Debuffs.FROZEN_ID, entity.getId())) {
			float[] current = RenderSystem.getShaderColor();
			this.sephiria$previousColor = current.clone();
			RenderSystem.setShaderColor(ICE_R, ICE_G, ICE_B, 1.0F);
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void sephiria$restoreColor(LivingEntity entity, float yaw, float partialTick, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		if (this.sephiria$previousColor != null) {
			float[] color = this.sephiria$previousColor;
			RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
			this.sephiria$previousColor = null;
		}
	}
}
