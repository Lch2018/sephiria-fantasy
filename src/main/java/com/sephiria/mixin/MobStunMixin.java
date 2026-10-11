package com.sephiria.mixin;

import com.sephiria.debuff.Debuffs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 冰冻（眩晕）的真正实现：<b>被冻住的生物整个 AI 停摆</b>——模型一动不动、也不做出任何行为
 * （用户口径 2026-10-11：「冰冻不是禁锢，被冰冻的敌人模型会静止，一动不动，无法做出任何行为
 * （包括攻击、移动等等）」）。
 *
 * <p>停的是 {@code Mob#serverAiStep}：原版 {@code LivingEntity#aiStep} 里调的那一步，
 * 里面装着传感、目标选择、目标（攻击 / 移动 / 跳跃）与寻路、转向、跳跃控制——
 * 整个取消之后，怪不再转头看人、不再迈腿、不会突然起跳，也点不出攻击（攻击本身在
 * {@link #sephiria$frozenCannotAttack} 里再挡一道，防止不经过 AI 的调用路径）。
 *
 * <p>取消的只有<b>行为</b>那一层：{@code LivingEntity#aiStep} 里剩下的物理（重力、摩擦、
 * 被推、着火、呼吸）照旧，所以悬空的怪照样往下掉，也不会卡在水里。
 * 移动速度 ×0 那条修饰符因此只是保险（见 {@code Debuffs}），真正让它站住的是这里。
 *
 * <p>附件只在服务端有（运行时不进存档），这一层守卫在客户端恒为 false，不影响客户端表现。
 * 不用原版的 {@code setNoAi(true)}：那个会写进存档（NBT 的 NoAI），服务端崩在冰冻期间
 * 那只怪就永久变成雕像了；这里拦的是 tick 里的调用，不落任何存档数据。
 */
@Mixin(Mob.class)
public class MobStunMixin {
	/** 被冻住时整个 AI 步骤不执行（目标、寻路、转向、跳跃控制都在这里面）。 */
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenStopsAi(CallbackInfo callback) {
		if (Debuffs.isFrozen((Mob) (Object) this)) {
			callback.cancel();
		}
	}

	/** 第二道保险：被冻住的生物打不出伤害（万一有绕过 AI 步骤的攻击路径）。 */
	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenCannotAttack(ServerLevel level, Entity target,
			CallbackInfoReturnable<Boolean> callback) {
		Mob self = (Mob) (Object) this;

		if (Debuffs.isFrozen(self)) {
			callback.setReturnValue(false);
		}
	}
}
