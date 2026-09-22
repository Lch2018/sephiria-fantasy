package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 服务端伤害入口上的三件事：暴击、最终减伤、无视防御伤害。
 *
 * <p>原版没有"受到的伤害乘算"这种属性，而 Fabric 的 {@code ALLOW_DAMAGE} 只能整体取消、
 * 不能改数值，所以这里直接在 {@code hurtServer} 上改 amount。倍率由
 * {@link SephiriaStaffItem#damageMultiplier} 提供——非减免期间恒为 1.0，所以减伤对其它情况没有影响。
 *
 * <p>三件事必须分开在两个注入点做：
 * <ul>
 *   <li>暴击与减伤在 <b>HEAD</b>，都是"改这一下的数值"；</li>
 *   <li>无视防御伤害在 <b>TAIL</b>：它是额外打的一次真实伤害，如果在 HEAD 打，就会先把目标的
 *       无敌帧顶起来，紧接着原来那一下反而被无敌帧吞掉。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class DamageReductionMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float sephiria$adjustDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;

		// 真实伤害自带「不吃减伤」的语义，也不再参与暴击与新的真伤
		if (SephiriaDamage.kindOf(source) == SephiriaDamage.Kind.TRUE) {
			return amount;
		}

		float result = SephiriaDamage.applyCrit(self, source, amount);
		float multiplier = SephiriaDamage.damageMultiplier(self);

		return multiplier >= 1.0F ? result : result * multiplier;
	}

	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void sephiria$applyTrueDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		// 只有这一下真的打中了才补真伤：被无敌帧挡掉、被取消的都不算
		if (callback.getReturnValueZ()) {
			SephiriaDamage.applyTrueDamage((LivingEntity) (Object) this, source, amount);
		}
	}
}
