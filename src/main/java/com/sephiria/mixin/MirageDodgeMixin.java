package com.sephiria.mixin;

import com.sephiria.artifact.BrokenSapphireItem;
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
 * 闪烁的眼睛「海市蜃楼」与破碎的蓝宝石「帷幕」的无条件闪避。
 *
 * <p>在 {@code hurtServer} 的最前面拦下；两者都直接取消整次伤害。帷幕只挡有来源实体的攻击，
 * 海市蜃楼保持原来的「任何伤害」口径。
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

		// 「破碎的蓝宝石」的帷幕只挡有来源实体的攻击（与 Dodge 同一条环境伤害排除口径）
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
