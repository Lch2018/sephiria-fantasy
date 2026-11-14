package com.sephiria.client.particle;

import com.sephiria.registry.ModParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/** 注册自定义粒子的客户端 Provider，逐个类型对应 0.5.x 的粒子类。 */
public final class SephiriaParticles {
	private SephiriaParticles() {
	}

	public static void register() {
		ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
		registry.register(ModParticleTypes.BLUE_SPARK, BlueSparkParticle::provider);
		registry.register(ModParticleTypes.SNOWFALL, SnowfallParticle::provider);
		registry.register(ModParticleTypes.PLASMA, PlasmaParticle::provider);
		registry.register(ModParticleTypes.ICE_GLINT, IceGlintParticle::provider);
		registry.register(ModParticleTypes.SUN_SLASH, SunSlashParticle::sunProvider);
		registry.register(ModParticleTypes.ICE_SLASH, SunSlashParticle::iceProvider);
		registry.register(ModParticleTypes.FROST_QI, FrostQiParticle::frostProvider);
		registry.register(ModParticleTypes.FLAME_QI, FrostQiParticle::flameProvider);
	}
}
