package com.sephiria.mixin.client;

import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import com.sephiria.compat.torohealth.ToroHealthCritAccess;
import com.sephiria.compat.torohealth.ToroHealthTextRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 ToroHealth 创建伤害数字粒子后，把我们的元素色与暴击标记挂上去。
 *
 * <p>1.20.1 的工厂签名是 {@code createParticle(SimpleParticleType, ClientLevel, double x,
 * double y, double z, double xSpeed, double ySpeed, double zSpeed)}，没有 {@code RandomSource}
 * （那是 26.3 才加的）。「生命变化值藏在第 4 个 double（xSpeed）的位模式里」这一点没变：
 * ToroHealth 自己的 LivingEntityMixin 用 {@code Double.longBitsToDouble(healthChangeLast &
 * 0xFFFFFFFFL)} 把值塞进 addParticle 的 xSpeed。正数是治疗，不参与我们的染色。
 *
 * <p>粒子坐标由 ToroHealth 按目标实体中心生成；{@link ToroHealthCompatClient#consume}
 * 用服务端包里的实体 id / 坐标（以及同步数据更新时记下的 UUID）匹配。匹配不到
 * （不是本模组伤害、包丢了、配置关掉了粒子）就保持 ToroHealth 原样。
 */
@Mixin(targets = "net.kairost.torohealth.client.particle.HealthChangeParticle$HealthChangeFactory")
public abstract class HealthChangeFactoryMixin {
	@Inject(method = "createParticle(Lnet/minecraft/core/particles/SimpleParticleType;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;",
			at = @At("RETURN"))
	private void sephiria$decorate(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double g, double h, double i,
			CallbackInfoReturnable<Particle> callback) {
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
		// 1.20.1 的 Particle#setColor 是公开方法；粒子 render 的文字色由我们按 rCol/gCol/bCol
		// 现算，所以这一步同时决定数字颜色与粒子小方块的色调。
		particle.setColor(((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F,
				(color & 0xFF) / 255.0F);

		if (particle instanceof ToroHealthCritAccess access) {
			access.sephiria$setCustom(true);
			access.sephiria$setCrit(info.crit());
			access.sephiria$setSecondaryColor(ToroHealthCompatClient.secondaryColor(info.secondaryElement()));
			access.sephiria$setDisplayValue(
					ToroHealthTextRenderer.preTax() ? info.preTaxDamage() : -1);
		}
	}
}
