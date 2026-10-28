package com.sephiria.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import com.sephiria.registry.ModParticleTypes;

/** 统一的轻量粒子：贴图由 particles/*.json 决定，生命周期短、无重力、不碰撞。 */
public class SephiriaParticle extends TextureSheetParticle {
	protected SephiriaParticle(ClientLevel level, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
		super(level, x, y, z, velocityX, velocityY, velocityZ);
		this.pickSprite(sprites);
		this.lifetime = 10 + this.random.nextInt(8);
		this.quadSize = 0.16F;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.friction = 0.98F;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static ParticleProvider<SimpleParticleType> simple(SpriteSet sprites) {
		return (type, level, x, y, z, vx, vy, vz) ->
				new SephiriaParticle(level, x, y, z, vx, vy, vz, sprites);
	}

	public static ParticleProvider<ModParticleTypes.SunSlashOptions> sunSlash(SpriteSet sprites) {
		return (options, level, x, y, z, vx, vy, vz) ->
				new SephiriaParticle(level, x, y, z, vx, vy, vz, sprites);
	}

	public static ParticleProvider<ModParticleTypes.FrostQiOptions> frostQi(SpriteSet sprites) {
		return (options, level, x, y, z, vx, vy, vz) ->
				new SephiriaParticle(level, x, y, z, vx, vy, vz, sprites);
	}

	public static ParticleProvider<ModParticleTypes.FlameQiOptions> flameQi(SpriteSet sprites) {
		return (options, level, x, y, z, vx, vy, vz) ->
				new SephiriaParticle(level, x, y, z, vx, vy, vz, sprites);
	}

	public static ParticleProvider<ModParticleTypes.IceSlashOptions> iceSlash(SpriteSet sprites) {
		return (options, level, x, y, z, vx, vy, vz) ->
				new SephiriaParticle(level, x, y, z, vx, vy, vz, sprites);
	}
}
