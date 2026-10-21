package com.sephiria.mixin;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 根之退「轻盈」：激活时最终摔落伤害 -20%（把 {@code causeFallDamage} 的下落距离等比缩小）。
 */
@Mixin(LivingEntity.class)
public abstract class RootRetreatFallMixin {
	@ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double sephiria$lightnessFall(double fallDistance) {
		if ((Object) this instanceof ServerPlayer player) {
			return fallDistance * RootRetreat.lightnessFallMultiplier(player);
		}

		return fallDistance;
	}
}
