package com.sephiria.mixin.client;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 根之退「轻盈」：激活时最终摔落伤害 -20%（缩小下落距离）。 */
@Mixin(LivingEntity.class)
public abstract class RootRetreatFallMixin {
	@ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float sephiria$lightnessFall(float fallDistance) {
		if ((Object) this instanceof ServerPlayer player) {
			return (float) (fallDistance * RootRetreat.lightnessFallMultiplier(player));
		}

		return fallDistance;
	}
}
