package com.sephiria.registry;

import com.sephiria.Sephiria;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;

/**
 * SEPHIRIA 的自定义粒子类型。
 *
 * <p>原版 ELECTRIC_SPARK 的颜色写死在客户端渲染器里（白粉色），服务端没法单独换色；
 * 想要「电火花的形状 + 指定颜色」就必须有自己的粒子类型，颜色由客户端粒子类
 * （{@code com.sephiria.client.particle.BlueSparkParticle}）写死成 #7EFAFF。
 * 形状与原版电火花完全一致：贴的就是 glow 贴图（见 particles/blue_spark.json）。
 *
 * <p>必须在 {@code Sephiria#onInitialize()} 里 {@link #initialize()} 一次——粒子类型
 * 要赶在原版注册表冻结之前注册；客户端侧的粒子渲染器由 mixin 在
 * {@code ParticleResources#registerProviders()} 末尾挂进来（Fabric 在 26.3 已经没有
 * 粒子工厂注册 API 可用了）。
 */
public final class ModParticleTypes {
	/** 电蓝色电火花：触电（挂层/每刻光环/结算）与乌云雷击（闪电线/命中爆点）共用。 */
	public static final SimpleParticleType BLUE_SPARK = register("blue_spark");

	private ModParticleTypes() {
	}

	private static SimpleParticleType register(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name);
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.simple());
	}

	/** 由 {@code Sephiria#onInitialize()} 调用：引用一次本类即触发静态字段的注册。 */
	public static void initialize() {
	}
}
