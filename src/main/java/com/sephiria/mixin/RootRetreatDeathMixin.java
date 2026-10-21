package com.sephiria.mixin;

import com.sephiria.difficulty.RootRetreat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 根之退「心之重担」：死亡时先吞掉那一部分背包物品，再走原版的死亡掉落。
 *
 * <p>挂在 {@code ServerPlayer#die} 的 HEAD——比原版掉东西早，所以消失的物品不会掉到地上；
 * 也不区分 keepInventory。
 */
@Mixin(ServerPlayer.class)
public abstract class RootRetreatDeathMixin {
	@Inject(method = "die", at = @At("HEAD"))
	private void sephiria$heartBurden(DamageSource source, CallbackInfo ci) {
		RootRetreat.applyDeathBurden((ServerPlayer) (Object) this);
	}
}
