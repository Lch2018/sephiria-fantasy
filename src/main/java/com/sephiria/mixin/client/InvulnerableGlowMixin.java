package com.sephiria.mixin.client;

import com.sephiria.client.InvulnClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 无敌可视化：无敌窗口内的玩家由原版描边管线描一圈光。 */
@Mixin(Minecraft.class)
public class InvulnerableGlowMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void sephiria$glowWhileInvulnerable(Entity entity, CallbackInfoReturnable<Boolean> callback) {
		if (entity instanceof Player && InvulnClientData.isActive(entity)) {
			callback.setReturnValue(true);
		}
	}
}
