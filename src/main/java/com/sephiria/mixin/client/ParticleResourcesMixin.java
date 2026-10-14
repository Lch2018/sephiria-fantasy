package com.sephiria.mixin.client;

import com.sephiria.client.particle.BlueSparkParticle;
import com.sephiria.client.particle.FrostQiParticle;
import com.sephiria.client.particle.SunSlashParticle;
import com.sephiria.registry.ModParticleTypes;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.particle.ParticleResources.MutableSpriteSet;
import net.minecraft.core.registries.BuiltInRegistries;import net.minecraft.resources.Identifier;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 把 sephiria_fantasy 的自定义粒子工厂挂进原版粒子渲染器。
 *
 * <p>26.3 的 Fabric API 删掉了旧的 ParticleFactoryRegistry，原版改成在
 * {@code ParticleResources#registerProviders()}（构造器末尾调用）里把渲染器写死进
 * 私有表——mod 想加自己的粒子只能在这一步的尾巴上插一手。原版
 * {@code register(type, registration)} 私有重载干两件事，照抄：
 * <ol>
 *   <li>给粒子 id 建一个贴图组放进 {@code spriteSets}（资源重载时按
 *       {@code particles/<id>.json} 填充贴图）；</li>
 *   <li>工厂放进按注册表数字 id 索引的 {@code providers}。</li>
 * </ol>
 * 这两步要动的私有字段与包私有贴图组类已用访问拓宽打开（见 sephiria_fantasy.accesswidener）。
 */
@Mixin(ParticleResources.class)
public abstract class ParticleResourcesMixin {
	@Shadow
	private Map<Identifier, MutableSpriteSet> spriteSets;

	@Shadow
	private Int2ObjectMap<ParticleProvider<?>> providers;

	@Inject(method = "registerProviders", at = @At("TAIL"))
	private void sephiria$registerParticles(CallbackInfo ci) {
		MutableSpriteSet spark = new MutableSpriteSet();
		this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticleTypes.BLUE_SPARK), spark);
		this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(ModParticleTypes.BLUE_SPARK),
				new BlueSparkParticle.Provider(spark));

		MutableSpriteSet slash = new MutableSpriteSet();
		this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticleTypes.SUN_SLASH), slash);
		this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(ModParticleTypes.SUN_SLASH),
				new SunSlashParticle.Provider(slash));

		// 冰霜武具那道弧状剑气：与剑痕同一套渲染，只是贴图是弧、工厂是自己的
		MutableSpriteSet qi = new MutableSpriteSet();
		this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticleTypes.FROST_QI), qi);
		this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(ModParticleTypes.FROST_QI),
				new FrostQiParticle.Provider(qi));

		// 永恒蚀那两版换色特效：形状与上面的剑气 / 剑痕完全一样，只是贴图换成了焰黄 / 冰蓝
		// （工厂也是同一个——参数形状一样，见 ModParticleTypes.QiOptions / SlashOptions）
		MutableSpriteSet flameQi = new MutableSpriteSet();
		this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticleTypes.FLAME_QI), flameQi);
		this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(ModParticleTypes.FLAME_QI),
				new FrostQiParticle.Provider(flameQi));

		MutableSpriteSet iceSlash = new MutableSpriteSet();
		this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticleTypes.ICE_SLASH), iceSlash);
		this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(ModParticleTypes.ICE_SLASH),
				new SunSlashParticle.Provider(iceSlash));
	}
}
