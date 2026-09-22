package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 赛菲利亚的伤害无视无敌帧（对玩家与敌对生物）。
 *
 * <p>原版 {@code hurtServer} 里，受到伤害后 20 刻内再来一次，只有伤害更高才结算差额——
 * 这就是高攻速武器兑现不了秒伤的原因。这里在伤害入口把 {@code invulnerableTime} 清零，
 * 让这一下必定满额结算；结算后原版会重新给它上帧，所以只影响"来自 SEPHIRIA 的这一下"。
 *
 * <p>友好生物（牛羊猪、村民等）不动：{@link SephiriaDamage#ignoresInvulnerableFrames} 只放行
 * 玩家与 {@code Enemy}。
 */
@Mixin(LivingEntity.class)
public class SephiriaDamageFramesMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void sephiria$ignoreInvulnerableFrames(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (SephiriaDamage.ignoresInvulnerableFrames(self) && SephiriaDamage.fromSephiria(source)) {
			self.setInvulnerableTime(0);
		}
	}
}
