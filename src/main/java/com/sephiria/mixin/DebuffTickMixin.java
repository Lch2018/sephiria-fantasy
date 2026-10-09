package com.sephiria.mixin;

import com.sephiria.debuff.Debuffs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 减益效果的推进器：减益挂在<b>目标实体</b>身上，ServerTickEvents 那种按玩家的循环够不着它们，
 * 所以直接从 LivingEntity 自己的 tick 里推——服务端这一侧每刻走一次。
 *
 * <p>没有减益时只有一次附件查询就返回，开销可以忽略。
 */
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
