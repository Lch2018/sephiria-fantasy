package com.sephiria.mixin.client;

import com.sephiria.artifact.BookEffectData;
import com.sephiria.artifact.BookItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 书「效率」对挖掘速度的最终倍率（1.20.1 版）。 */
@Mixin(Player.class)
public class PlayerMiningSpeedMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void sephiria$bookMiningSpeed(BlockState state, CallbackInfoReturnable<Float> callback) {
		Player self = (Player) (Object) this;
		float factor;

		if (self instanceof ServerPlayer player) {
			factor = BookItem.miningSpeedFactor(player);
		} else if (self.level().isClientSide()) {
			factor = BookEffectData.miningSpeedFactor();
		} else {
			return;
		}

		if (factor > 1.0F) {
			callback.setReturnValue(callback.getReturnValueF() * factor);
		}
	}
}
