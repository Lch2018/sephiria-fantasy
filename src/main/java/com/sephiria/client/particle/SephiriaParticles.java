package com.sephiria.client.particle;

import com.sephiria.registry.ModParticleTypes;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/** 注册自定义粒子的客户端 Provider；不做 26.3 那套方向/尺寸参数，只保证粒子可见。 */
public final class SephiriaParticles {
	private SephiriaParticles() {
	}

	public static void register() {
		ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
		registry.register(ModParticleTypes.BLUE_SPARK, SephiriaParticle::simple);
		registry.register(ModParticleTypes.SNOWFALL, SephiriaParticle::simple);
		registry.register(ModParticleTypes.PLASMA, SephiriaParticle::simple);
		registry.register(ModParticleTypes.SUN_SLASH, SephiriaParticle::sunSlash);
		registry.register(ModParticleTypes.FROST_QI, SephiriaParticle::frostQi);
		registry.register(ModParticleTypes.FLAME_QI, SephiriaParticle::flameQi);
		registry.register(ModParticleTypes.ICE_SLASH, SephiriaParticle::iceSlash);
	}
}
