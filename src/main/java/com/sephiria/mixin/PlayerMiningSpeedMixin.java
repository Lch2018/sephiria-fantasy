package com.sephiria.mixin;

import com.sephiria.artifact.BookEffectData;
import com.sephiria.artifact.BookItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 书「效率」对挖掘速度的最终倍率。
 *
 * <p>乘在 {@code Player.getDestroySpeed} 的返回值上，也就是在原版效率附魔、 haste、
 * 挖掘疲劳等全部结算之后；这样方块破坏时间严格变成「原时间 ÷ 倍率」，与用户例子一致。
 *
 * <p>服务端直接读书与属性；客户端没有背包附件，读服务端同步过来的倍率
 * （{@link BookEffectData}），否则客户端预测的挖掘进度会和实际不一致。
 */
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
