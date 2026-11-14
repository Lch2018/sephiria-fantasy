package com.sephiria.mixin.client;

import com.sephiria.debuff.Debuffs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 每刻结算一次减益（1.20.1 版）：把服务端实体 tick 接到 {@link Debuffs#tick}。 */
@Mixin(LivingEntity.class)
public class DebuffTickMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void sephiria$tickDebuffs(CallbackInfo callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (self.level() instanceof ServerLevel level) {
			Debuffs.tick(self, level);
		}
	}
}
