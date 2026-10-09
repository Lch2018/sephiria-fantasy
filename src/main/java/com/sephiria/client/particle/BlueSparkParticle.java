package com.sephiria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * 电蓝色电火花——把原版 ELECTRIC_SPARK 的渲染行为原样搬过来，只把颜色从白粉改成 #7EFAFF。
 *
 * <p>原版电火花 = {@code GlowParticle}（贴 glow 贴图、发光层、寿命 4~7 刻、出速×0.25、
 * 全程带平滑自发光），颜色由渲染器写死 {@code setColor(1.0F, 0.9F, 1.0F)}，服务端染不了色，
 * 所以这里复制一份类自己染色。行为参数与 {@code GlowParticle$ElectricSparkProvider} 逐行对应，
 * 保证形状和原版完全一样，只是发蓝光。
 */
public class BlueSparkParticle extends SingleQuadParticle {
	/** 电蓝 #7EFAFF：电元素主题的统一色调（原版是白粉 1.0/0.9/1.0）。 */
	private static final float R = 0x7E / 255.0F;
	private static final float G = 0xFA / 255.0F;
	private static final float B = 0xFF / 255.0F;

	private final SpriteSet sprites;

	public BlueSparkParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd, sprites.first());
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
	public Layer getLayer() {
		return Layer.OPAQUE;
	}

	@Override
	public int getLightCoords(float partialTick) {
		// 原版电火花全程自带平滑发光，越接近寿命尾声越暗——照抄，蓝色贴图才不会在暗处发灰
		return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(partialTick),
				(this.age + partialTick) / this.lifetime);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
	}

	/** 粒子工厂：由 {@code ParticleResourcesMixin} 在注册时挂到 sephiria:blue_spark 上。 */
	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new BlueSparkParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}
