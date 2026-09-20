package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 「最终伤害减免」：把受到的伤害按倍率打折。
 *
 * <p>原版没有"受到的伤害乘算"这种属性，而 Fabric 的 {@code ALLOW_DAMAGE} 只能整体取消、
 * 不能改数值，所以这里直接在服务端的伤害入口 {@code hurtServer} 上把 amount 改掉。
 * 倍率由 {@link SephiriaStaffItem#damageMultiplier} 提供——非减免期间恒为 1.0，
 * 所以这个 mixin 对其它任何情况都没有影响。
 */
@Mixin(LivingEntity.class)
public class DamageReductionMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float sephiria$applyDamageReduction(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		float multiplier = SephiriaDamage.damageMultiplier(self);

		return multiplier >= 1.0F ? amount : amount * multiplier;
	}
}
