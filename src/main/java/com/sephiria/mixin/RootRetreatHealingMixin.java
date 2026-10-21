package com.sephiria.mixin;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 根之退「衰退」+「紧张」：所有走 {@code LivingEntity#heal} 的恢复都按比例缩小（或直接归零）。
 *
 * <p>自然回复、再生药水、瞬间治疗、金苹果最终都会调这个方法，所以一处就够。
 */
@Mixin(LivingEntity.class)
public abstract class RootRetreatHealingMixin {
	@ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
	private float sephiria$scaleHealing(float amount) {
		if (amount > 0.0F && (Object) this instanceof ServerPlayer player) {
			return amount * RootRetreat.healingMultiplier(player);
		}

		return amount;
	}
}
