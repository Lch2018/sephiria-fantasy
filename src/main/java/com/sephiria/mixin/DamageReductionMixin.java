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
		SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

		// 真实伤害绕开减伤（这是它存在的意义），但和别的伤害一样吃暴击与黄金之手（2026-10 起）
		if (kind == SephiriaDamage.Kind.TRUE) {
			return SephiriaDamage.applyGoldenHands(source, SephiriaDamage.applyCrit(self, source, amount));
		}

		// 电属性伤害与灼伤（减益伤害）同样不吃减伤与黄金之手，但照掷暴击：
		// 电属性那边是「通用 + 电属性攻击的暴击几率（麒麟的角）」，灼伤只用通用那一项
		if (kind == SephiriaDamage.Kind.ELECTRIC || kind == SephiriaDamage.Kind.FIRE) {
			return SephiriaDamage.applyCrit(self, source, amount);
		}

		float result = SephiriaDamage.applyCrit(self, source, amount);
		float multiplier = SephiriaDamage.damageMultiplier(self);

		// 黄金之手是攻击方的增益，接在减伤之后统一放大
		return SephiriaDamage.applyGoldenHands(source, multiplier >= 1.0F ? result : result * multiplier);
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
