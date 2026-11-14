package com.sephiria.mixin.client;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 赛菲莉娅的伤害无视无敌帧（对玩家与敌对生物），并给它的击退单独上冷却。
 *
 * <p>1.20.1 的 {@code LivingEntity.hurt} 里，10 刻内再来一次只有伤害更高才结算差额——
 * 这是高攻速武器兑现不了秒伤的原因。这里在伤害入口把 {@code invulnerableTime} 清零，
 * 让这一下必定满额结算；结算后原版会重新给它上帧，所以只影响「来自 SEPHIRIA 的这一下」。
 *
 * <p>但击退不能跟着变成「每一下都推」（2026-10-15 用户报的「高频攻击让敌人被连续击退、
 * 飞出去很远」）：本模组的伤害既然清掉了无敌帧，原版那套「10 刻内只推一次」也就一起没了。
 * 所以这里再给本模组伤害的击退单独上一道冷却——间隔与原版受击无敌一致（10 刻）、每个敌人各算各的
 * （{@link SephiriaDamage#allowKnockback}），非本模组的伤害一律放行。
 *
 * <p>1.20.1 的 {@code hurt} 内部调的是不带来源的 {@code knockback(DDD)}，所以在 HEAD 用
 * ThreadLocal 记下「这一下允不允许推」，再 redirect 那个调用点。方法返回时清掉，
 * 不会影响之后别处自己调的击退。
 */
@Mixin(LivingEntity.class)
public class SephiriaDamageFramesMixin {
	@Unique
	private static final ThreadLocal<Boolean> SEPHIRIA_ALLOW_KNOCKBACK = ThreadLocal.withInitial(() -> Boolean.TRUE);

	@Inject(method = "hurt", at = @At("HEAD"))
	private void sephiria$ignoreInvulnerableFrames(DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;
		boolean fromSephiria = SephiriaDamage.fromSephiria(source);

		SEPHIRIA_ALLOW_KNOCKBACK.set(!fromSephiria || SephiriaDamage.allowKnockback(self, source));

		if (!fromSephiria) {
			return;
		}

		// 本模组的投掷物（重型弩的弩矢）完全不吃受击无敌帧：任何目标都一样，
		// 穿透连射时每一发都满额结算（原版箭矢不受影响——它们不是 SephiriaDamage.Source）
		if (source.getDirectEntity() instanceof SephiriaDamage.Source
				|| SephiriaDamage.ignoresInvulnerableFrames(self)) {
			SephiriaDamage.clearDamageCooldown(self);
		}
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void sephiria$clearKnockbackFlag(DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		SEPHIRIA_ALLOW_KNOCKBACK.remove();
	}

	@Redirect(method = "hurt",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
	private void sephiria$gatedKnockback(LivingEntity self, double strength, double x, double z) {
		if (SEPHIRIA_ALLOW_KNOCKBACK.get()) {
			self.knockback(strength, x, z);
		}
	}
}
