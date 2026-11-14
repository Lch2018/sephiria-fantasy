package com.sephiria.mixin.client;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 根之退「心之重担」：死亡时先吞掉部分背包物品，再走原版掉落。 */
@Mixin(ServerPlayer.class)
public abstract class RootRetreatDeathMixin {
	@Inject(method = "die", at = @At("HEAD"))
	private void sephiria$heartBurden(DamageSource source, CallbackInfo ci) {
		RootRetreat.applyDeathBurden((ServerPlayer) (Object) this);
	}
}
