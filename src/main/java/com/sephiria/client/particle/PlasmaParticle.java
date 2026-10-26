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
 * 等离子体的菱形粒子：白色菱形贴图 + 每颗粒子按权重随机染色。
 *
 * <p>用户 2026-10-25 定：形状为菱形，颜色比例 20% 粉 / 60% 白 / 20% 蓝。
 * 颜色在客户端逐颗掷（{@link #applyColour}），服务端只发粒子类型，不需要额外参数。
 */
public class PlasmaParticle extends SingleQuadParticle {
	private static final float PINK_R = 1.0F;
	private static final float PINK_G = 0x8A / 255.0F;
	private static final float PINK_B = 0xD8 / 255.0F;
	private static final float BLUE_R = 0x7E / 255.0F;
	private static final float BLUE_G = 0xFA / 255.0F;
	private static final float BLUE_B = 0xFF / 255.0F;

	public PlasmaParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd, sprites.first());
		this.friction = 0.94F;
		this.speedUpWhenYMotionIsBlocked = true;
		this.hasPhysics = false;
		// 用户 2026-10-25 定：等离子体粒子体型为原来的 50%（0.65 → 0.325）
		this.quadSize *= 0.325F;
		this.setParticleSpeed(xd * 0.2D, yd * 0.2D + 0.005D, zd * 0.2D);
		this.setLifetime(6 + this.random.nextInt(5));
		this.applyColour(this.random);
	}

	/** 20% 粉、60% 白、20% 蓝（权重固定，不随等级变化）。 */
	private void applyColour(RandomSource random) {
		double roll = random.nextDouble();

		if (roll < 0.20D) {
			this.setColor(PINK_R, PINK_G, PINK_B);
		} else if (roll < 0.80D) {
			this.setColor(1.0F, 1.0F, 1.0F);
		} else {
			this.setColor(BLUE_R, BLUE_G, BLUE_B);
		}
	}

	@Override
	public Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	public int getLightCoords(float partialTick) {
		// 等离子体在暗处也要看得见：与电火花一样带平滑自发光
		return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(partialTick),
				(this.age + partialTick) / this.lifetime);
	}

	/** 粒子工厂：由 {@code ParticleResourcesMixin} 挂到 sephiria_fantasy:plasma 上。 */
	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new PlasmaParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}
