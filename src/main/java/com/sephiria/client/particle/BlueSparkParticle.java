package com.sephiria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 电蓝色电火花——把原版 ELECTRIC_SPARK 的渲染行为原样搬过来，只把颜色从白粉改成 #7EFAFF。
 *
 * <p>原版电火花 = {@code GlowParticle}（贴 glow 贴图、半透明层、寿命 4~7 刻、出速×0.25、
 * 全程带平滑自发光），颜色由渲染器写死 {@code setColor(1.0F, 0.9F, 1.0F)}，服务端染不了色，
 * 所以这里复制一份类自己染色。1.20.1 的 {@code GlowParticle.getLightColor} 就是 26.x 的
 * {@code LightCoordsUtil.addSmoothBlockEmission}，这里照抄同一段光照计算。
 */
public class BlueSparkParticle extends TextureSheetParticle {
	/** 电蓝 #7EFAFF：电元素主题的统一色调（原版是白粉 1.0/0.9/1.0）。 */
	private static final float R = 0x7E / 255.0F;
	private static final float G = 0xFA / 255.0F;
	private static final float B = 0xFF / 255.0F;

	private final SpriteSet sprites;

	public BlueSparkParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd);
		this.friction = 0.96F;
		this.speedUpWhenYMotionIsBlocked = true;
		this.sprites = sprites;
		this.quadSize *= 0.75F;
		this.hasPhysics = false;
		this.setSpriteFromAge(sprites);
		this.setColor(R, G, B);
		this.setParticleSpeed(xd * 0.25D, yd * 0.25D, zd * 0.25D);
		this.setLifetime(4 + this.random.nextInt(4));
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getLightColor(float partialTick) {
		// 与 1.20.1 GlowParticle 同一段：越接近寿命尾声，方块光越暗
		float progress = Mth.clamp((this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
		int packed = super.getLightColor(partialTick);
		int block = packed & 255;
		int sky = (packed >> 16) & 255;
		int blockEmission = (int) (block + progress * 15.0F * 16.0F);

		if (blockEmission > 240) {
			blockEmission = 240;
		}

		return blockEmission | (sky << 16);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
	}

	/** 粒子工厂：挂到 sephiria_fantasy:blue_spark 上。 */
	public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
		return (type, level, x, y, z, xd, yd, zd) ->
				new BlueSparkParticle(level, x, y, z, xd, yd, zd, sprites);
	}
}
