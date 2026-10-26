package com.sephiria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SnowflakeParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * 暴风雪云的雪：直接继承原版 {@link SnowflakeParticle} 的下落 / 漂移 / 寿命行为，
 * 只把贴图换成从原版天气雪 {@code environment/snow.png} 裁出来的 16×16 小雪花。
 *
 * <p>为什么不用 {@code ParticleTypes.SNOWFLAKE}：那颗用的是 {@code particle/snowflake}
 * 的雪花图标，看起来比原版降雪大且形状明显；这里要的是原版天气雪那种细碎小白点。
 */
public class SnowfallParticle extends SnowflakeParticle {
	public SnowfallParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd, sprites);
	}

	/** 粒子工厂：由 {@code ParticleResourcesMixin} 在注册时挂到 sephiria_fantasy:snowfall 上。 */
	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new SnowfallParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}
