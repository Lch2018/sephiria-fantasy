package com.sephiria.mixin.client;

import com.sephiria.client.ToroHealthColor;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 把 ToroHealth 的伤害数字染成本模组武器的颜色（轻量兼容，不改变数值）。 */
@Mixin(targets = "net.kairost.torohealth.client.particle.HealthChangeParticle")
public abstract class HealthChangeParticleMixin {
	@Shadow
	protected float rCol;
	@Shadow
	protected float gCol;
	@Shadow
	protected float bCol;

	@Inject(method = "render", at = @At("HEAD"))
	private void sephiria$tint(com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
			net.minecraft.client.Camera camera, float partialTick, CallbackInfo info) {
		int color = ToroHealthColor.color();

		if (color >= 0) {
			this.rCol = ((color >> 16) & 0xFF) / 255.0F;
			this.gCol = ((color >> 8) & 0xFF) / 255.0F;
			this.bCol = (color & 0xFF) / 255.0F;
		}
	}
}
