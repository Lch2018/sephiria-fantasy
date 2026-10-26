package com.sephiria.mixin;

import com.sephiria.artifact.FlickeringEyeItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 闪烁的眼睛「海市蜃楼」满层时的无条件闪避。
 *
 * <p>在 {@code hurtServer} 的最前面拦下：这时 {@code amount} 还没经过护甲 / 本模组减伤，
 * 正是用户口径里的「税前伤害」；闪避后用它 ×150% 反弹成混沌伤害。
 *
 * <p>放在单独一个 mixin 而不是并进 {@code DamageReductionMixin}，是因为它要<b>取消整次伤害</b>
 * （返回 true 表示已处理），和「改数值」那几件事不在一条注入链上。
 */
@Mixin(value = LivingEntity.class, priority = 900)
public class MirageDodgeMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void sephiria$mirageDodge(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (!(self instanceof ServerPlayer player)) {
			return;
		}

		if (FlickeringEyeItem.onIncomingDamage(player, amount)) {
			callback.setReturnValue(true);
			callback.cancel();
		}
	}
}
