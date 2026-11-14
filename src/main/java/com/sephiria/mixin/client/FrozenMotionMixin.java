package com.sephiria.mixin.client;

import com.sephiria.debuff.Debuffs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 冻住的生物一步都挪不了：连击退、重力、水流推动都拦在 {@code Entity#move}（1.20.1 版）。 */
@Mixin(Entity.class)
public class FrozenMotionMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenCannotMove(MoverType type, Vec3 movement, CallbackInfo callback) {
		Entity self = (Entity) (Object) this;

		if (self instanceof LivingEntity living && Debuffs.isFrozen(living)) {
			callback.cancel();
		}
	}
}
