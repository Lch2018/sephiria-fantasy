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
 * 玩家与 {@code Enemy}。唯一的例外是本模组的投掷物（重型弩的弩矢，实现
 * {@link SephiriaDamage.Source} 标记）——按用户要求「完全不吃这一套」，对任何目标都不做帧判定。
 */
@Mixin(LivingEntity.class)
public class SephiriaDamageFramesMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void sephiria$ignoreInvulnerableFrames(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		// 本模组的投掷物（重型弩的弩矢）完全不吃受击无敌帧：任何目标都一样，
		// 穿透连射时每一发都满额结算（原版箭矢不受影响——它们不是 SephiriaDamage.Source）
		if (source.getDirectEntity() instanceof SephiriaDamage.Source) {
			self.setInvulnerableTime(0);
			return;
		}

		if (SephiriaDamage.ignoresInvulnerableFrames(self) && SephiriaDamage.fromSephiria(source)) {
			self.setInvulnerableTime(0);
		}
	}

	/**
	 * 结算完再把本模组伤害留下的无敌帧清掉——**这一条才是关键**：原版只要走了「正常结算」那一支
	 * 就会把 {@code damageCooldownTime} 刷成 20 刻（带 bypasses_cooldown 的伤害也一样），而灼伤
	 * 每 10 刻、触电每 5~15 刻就跳一次，于是目标被永久罩在无敌窗里——玩家随后的攻击要么整下被吞、
	 * 要么只结算「差额」（用户 2026-10 报的「打有减益的敌人打不出伤害」就是这个）。
	 * 本模组的伤害只在当刻生效，不留帧。
	 */
	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void sephiria$clearInvulnerableFrames(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (SephiriaDamage.fromSephiria(source)) {
			self.setInvulnerableTime(0);
		}
	}
}
