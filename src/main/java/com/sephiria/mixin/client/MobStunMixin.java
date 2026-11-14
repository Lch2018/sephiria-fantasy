package com.sephiria.mixin.client;

import com.sephiria.debuff.Debuffs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 冰冻（眩晕）：被冻住的生物整个 AI 停摆、也打不出伤害（1.20.1 版）。 */
@Mixin(Mob.class)
public class MobStunMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenStopsAi(CallbackInfo callback) {
		if (Debuffs.isFrozen((Mob) (Object) this)) {
			callback.cancel();
		}
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenCannotAttack(Entity target, CallbackInfoReturnable<Boolean> callback) {
		if (Debuffs.isFrozen((Mob) (Object) this)) {
			callback.setReturnValue(false);
		}
	}
}
