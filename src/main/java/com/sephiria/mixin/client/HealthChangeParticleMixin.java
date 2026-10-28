package com.sephiria.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sephiria.compat.torohealth.ToroHealthCritAccess;
import com.sephiria.compat.torohealth.ToroHealthTextRenderer;
import net.kairost.torohealth.ToroHealth;
import net.kairost.torohealth.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 ToroHealth 的伤害数字粒子挂上本模组的显示信息，并接管带标记数字的绘制。
 *
 * <p>1.20.1 的 ToroHealth 没有 1.21.1 的 {@code TextParticleRenderer}：它在
 * {@code HealthChangeParticle#render} 里直接建 PoseStack、调 {@code Font#drawInBatch}
 * 画 {@code Integer.toString(value)}。所以这里在 HEAD 处截住：本模组伤害的数字改用
 * {@link ToroHealthTextRenderer}（元素色 + 暴击 2 倍 + 居中），其余条目直接放行给原方法。
 *
 * <p>为了保持与 ToroHealth 原渲染一致的坐标 / 光照，这里复刻它的变换：按 tick 插值粒子位置、
 * 相机朝向、0.025 缩放（负 Y 翻转）、光照模式。颜色从粒子自己的 {@code rCol/gCol/bCol}
 * 现算（工厂混入已用 {@code Particle#setColor} 写入服务端算好的元素色），alpha 保留粒子的淡出。
 *
 * <p>目标类来自可选 mod，所以整体走 {@code required:false} 的 mixin 配置。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.HealthChangeParticle")
public abstract class HealthChangeParticleMixin implements ToroHealthCritAccess {
	/** ToroHealth 存的绝对值（服务端生命变化量的绝对值）。 */
	@Shadow
	@Final
	private int value;
	/** 粒子的上一帧 / 当前帧位置与透明度（继承自 {@code Particle}）。 */
	@Shadow
	protected double xo;
	@Shadow
	protected double yo;
	@Shadow
	protected double zo;
	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;
	@Shadow
	protected float rCol;
	@Shadow
	protected float gCol;
	@Shadow
	protected float bCol;
	@Shadow
	protected float alpha;
	@Shadow
	protected abstract int getLightColor(float partialTick);

	@Unique
	private boolean sephiria$custom;
	@Unique
	private boolean sephiria$crit;
	@Unique
	private int sephiria$secondaryColor = -1;
	@Unique
	private int sephiria$displayValue = -1;

	@Override
	public void sephiria$setCustom(boolean custom) {
		this.sephiria$custom = custom;
	}

	@Override
	public boolean sephiria$isCustom() {
		return this.sephiria$custom;
	}

	@Override
	public void sephiria$setCrit(boolean crit) {
		this.sephiria$crit = crit;
	}

	@Override
	public boolean sephiria$isCrit() {
		return this.sephiria$crit;
	}

	@Override
	public void sephiria$setSecondaryColor(int color) {
		this.sephiria$secondaryColor = color;
	}

	@Override
	public int sephiria$secondaryColor() {
		return this.sephiria$secondaryColor;
	}

	@Override
	public void sephiria$setDisplayValue(int value) {
		this.sephiria$displayValue = value;
	}

	@Override
	public int sephiria$displayValue() {
		return this.sephiria$displayValue;
	}

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V",
			at = @At("HEAD"), cancellable = true)
	private void sephiria$render(VertexConsumer vertexConsumer, Camera camera, float partialTick,
			CallbackInfo callback) {
		if (!this.sephiria$custom) {
			return;
		}

		String text = this.sephiria$displayValue >= 0
				? Integer.toString(this.sephiria$displayValue)
				: Integer.toString(this.value);
		// 颜色：RGB 由工厂混入写入的元素色，alpha 跟随粒子淡出。
		int alphaBits = ((int) (this.alpha * 255.0F) & 0xFF) << 24;
		int color = alphaBits
				| (((int) (this.rCol * 255.0F) & 0xFF) << 16)
				| (((int) (this.gCol * 255.0F) & 0xFF) << 8)
				| ((int) (this.bCol * 255.0F) & 0xFF);
		color = ToroHealthTextRenderer.brighten(color, ToroHealthTextRenderer.DEFAULT_BRIGHTNESS);
		int light = ToroHealth.getConfig().particleOptions.particleLightMode
				== ModConfig.ParticleLightMode.FULL_BRIGHT
						? 0xF000F0
						: this.getLightColor(partialTick);

		// 与 ToroHealth 原渲染相同的插值位置（粒子在实体半身高处）
		float px = (float) (Mth.lerp(partialTick, this.xo, this.x) - camera.getPosition().x);
		float py = (float) (Mth.lerp(partialTick, this.yo, this.y) - camera.getPosition().y);
		float pz = (float) (Mth.lerp(partialTick, this.zo, this.z) - camera.getPosition().z);

		// 原渲染器同样自建一个 BufferBuilder（不复用 Tesselator 的共享 builder，避免与粒子引擎打架）
		MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new BufferBuilder(256));
		ToroHealthTextRenderer.render(buffers, camera.rotation(), px, py, pz, text, color, light,
				this.sephiria$crit);
		buffers.endBatch();
		callback.cancel();
	}
}
