package com.sephiria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * 冰风暴的静止冰晶高光：外观沿用原版 END_ROD 的 glitter 序列，
 * 但把重力设为 0，粒子生成后不会向下飘。
 */
public class IceGlintParticle extends SimpleAnimatedParticle {
	private IceGlintParticle(ClientLevel level, double x, double y, double z,
			double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, sprites, 0.0F);
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.quadSize *= 0.75F;
		this.lifetime = 60 + this.random.nextInt(12);
		this.setFadeColor(15916745);
		this.setSpriteFromAge(sprites);
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
				double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new IceGlintParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}
