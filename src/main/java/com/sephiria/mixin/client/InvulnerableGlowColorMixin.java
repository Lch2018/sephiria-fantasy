package com.sephiria.mixin.client;

import com.sephiria.client.InvulnClientData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 无敌描边颜色：根之退 40/50 阶段金/红，其余白色。 */
@Mixin(Entity.class)
public class InvulnerableGlowColorMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void sephiria$glowColor(CallbackInfoReturnable<Integer> callback) {
		Entity self = (Entity) (Object) this;

		if (self instanceof Player && InvulnClientData.isActive(self)) {
			callback.setReturnValue(InvulnClientData.color(self));
		}
	}
}
