package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 赛菲莉娅的伤害无视无敌帧（对玩家与敌对生物）。
 *
 * <p>原版 {@code hurtServer} 里，受到伤害后 20 刻内再来一次，只有伤害更高才结算差额——
 * 这就是高攻速武器兑现不了秒伤的原因。这里在伤害入口把 {@code invulnerableTime} 清零，
 * 让这一下必定满额结算；结算后原版会重新给它上帧，所以只影响"来自 SEPHIRIA 的这一下"。
 *
 * <p>友好生物（牛羊猪、村民等）不动：{@link SephiriaDamage#ignoresInvulnerableFrames} 只放行
 * 玩家与 {@code Enemy}。唯一的例外是本模组的投掷物（重型弩的弩矢，实现
 * {@link SephiriaDamage.Source} 标记）——按用户要求「完全不吃这一套」，对任何目标都不做帧判定。
 *
 * <p><b>但击退不能跟着变成「每一下都推」</b>（2026-10-15 用户报的「高频攻击让敌人被连续击退、
 * 飞出去很远」）：本模组的伤害既然清掉了无敌帧，原版那套「10 刻内只推一次」的限制也就一起没了。
 * 所以这里再给本模组伤害的击退单独上一道冷却——间隔与原版受击无敌一致（10 刻）、每个敌人各算各的
 * （{@link SephiriaDamage#allowKnockback}），非本模组的伤害一律放行。
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
			SephiriaDamage.clearDamageCooldown(self);
			return;
		}

		if (SephiriaDamage.ignoresInvulnerableFrames(self) && SephiriaDamage.fromSephiria(source)) {
			SephiriaDamage.clearDamageCooldown(self);
		}
	}

	/**
	 * 击退冷却：本模组伤害的击退最多每 10 刻一次（与原版受击无敌间隔一致），每个敌人各算各的。
	 *
	 * <p>挂的是 6 参的 {@code knockback(DDDLDamageSource;FZ)V}——它是真正的实现，
	 * 5 参的重载（{@code hurtServer} 用的那个）与 {@code Player#attack} 的冲刺击退都会走到它，
	 * 所以「一次攻击最多推一下」在这一个点上就管住了。
	 * 取消掉的只是「推」这个动作（那个方法里只有 {@code setDeltaMovement} 与 {@code needsSync}），
	 * 受伤的红闪、音效、粒子都在别处，不受影响。
	 */
	@Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
			at = @At("HEAD"), cancellable = true)
	private void sephiria$limitKnockback(double strength, double x, double z, DamageSource source, float amount,
			boolean flag, CallbackInfo callback) {
		if (!SephiriaDamage.allowKnockback((LivingEntity) (Object) this, source)) {
			callback.cancel();
		}
	}

	/**
	 * 结算完再把本模组伤害留下的无敌帧清掉——**这一条才是关键**：原版只要走了「正常结算」那一支
	 * 就会把 {@code damageCooldownTime} 刷成 20 刻（带 bypasses_cooldown 的伤害也一样），而灼伤
	 * 每 10 刻、触电 / 等离子体的触电那一路每 20 / 40 刻就跳一次，于是目标被永久罩在无敌窗里——玩家随后的攻击要么整下被吞、
	 * 要么只结算「差额」（用户 2026-10 报的「打有减益的敌人打不出伤害」就是这个）。
	 * 本模组的伤害只在当刻生效，不留帧。
	 */
	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void sephiria$clearInvulnerableFrames(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (SephiriaDamage.fromSephiria(source)) {
			SephiriaDamage.clearDamageCooldown(self);
		}
	}
}
