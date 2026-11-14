package com.sephiria.mixin.client;

import com.sephiria.artifact.BrokenSapphireItem;
import com.sephiria.artifact.FlickeringEyeItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 闪烁的眼睛「海市蜃楼」与破碎的蓝宝石「帷幕」的无条件闪避（1.20.1 版）。 */
@Mixin(value = LivingEntity.class, priority = 900)
public class MirageDodgeMixin {
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void sephiria$mirageDodge(DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (!(self instanceof ServerPlayer player)) {
			return;
		}

		if (source.getEntity() != null && BrokenSapphireItem.onIncomingDamage(player)) {
			callback.setReturnValue(true);
			callback.cancel();
			return;
		}

		if (FlickeringEyeItem.onIncomingDamage(player, amount)) {
			callback.setReturnValue(true);
			callback.cancel();
		}
	}
}
