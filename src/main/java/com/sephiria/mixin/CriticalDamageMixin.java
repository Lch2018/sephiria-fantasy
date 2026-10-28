package com.sephiria.mixin;

import com.sephiria.damage.SephiriaDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 把本模组伤害接到 applyCrit（暴击 + ToroHealth 元数据）上。 */
@Mixin(LivingEntity.class)
public abstract class CriticalDamageMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float sephiria$crit(float amount, ServerLevel level, DamageSource source) {
		return SephiriaDamage.applyCrit((LivingEntity) (Object) this, source, amount);
	}
}
