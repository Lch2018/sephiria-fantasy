package com.sephiria.compat.torohealth.mixin;

import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthCritAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 ToroHealth 创建伤害数字粒子后，把我们的元素色与暴击标记挂上去。
 *
 * <p>粒子坐标由 ToroHealth 按目标实体中心生成；{@link ToroHealthCompatClient#consume}
 * 用服务端包里的实体 id / 坐标匹配。匹配不到（不是本模组伤害、包丢了、配置关掉了粒子）
 * 就保持 ToroHealth 原样。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.HealthChangeParticle$HealthChangeFactory")
public abstract class HealthChangeFactoryMixin {
	@Inject(method = "createParticle(Lnet/minecraft/core/particles/SimpleParticleType;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/util/RandomSource;)Lnet/minecraft/client/particle/Particle;",
			at = @At("RETURN"))
	private void sephiria$decorate(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double g, double h, double i,
			RandomSource random, CallbackInfoReturnable<Particle> callback) {
		Particle particle = callback.getReturnValue();

		if (particle == null) {
			return;
		}

		// 生命值同步时由 LivingEntitySyncMixin 记下的实体 UUID；无论这一颗是伤害还是治疗都要取走并清掉，
		// 否则治疗粒子留下的旧 UUID 会被下一次无关的伤害粒子误用。
		java.util.UUID entityUuid = ToroHealthCompatClient.takeParticleEntity();

		// ToroHealth 把生命变化值塞在 g 的 double 位里；正数是治疗，不参与我们的染色
		int healthChange = (int) Double.doubleToLongBits(g);

		if (healthChange >= 0) {
			return;
		}

		ToroHealthCompatClient.DamageInfo info = ToroHealthCompatClient.consume(level, x, y, z, entityUuid);

		if (info == null) {
			return;
		}

		int color = info.displayColor();

		if (particle instanceof SingleQuadParticle quad) {
			quad.setColor(((color >> 16) & 0xFF) / 255.0F,
					((color >> 8) & 0xFF) / 255.0F,
					(color & 0xFF) / 255.0F);
		}

		if (particle instanceof ToroHealthCritAccess access) {
			access.sephiria$setCustom(true);
			access.sephiria$setCrit(info.crit());
			access.sephiria$setSecondaryColor(ToroHealthCompatClient.secondaryColor(info.secondaryElement()));
			access.sephiria$setDisplayValue(
					com.sephiria.client.damage.DamageDisplayConfig.preTax() ? info.preTaxDamage() : -1);
		}
	}
}
