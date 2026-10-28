package com.sephiria.compat;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.20.1 的 Fabric API 没有 AFTER_DAMAGE 事件（只有伤害前的 ALLOW_DAMAGE）。
 *
 * <p>26.3 的代码全都挂在 AFTER_DAMAGE 上，语义是「扣完护甲之后真正打掉多少」。这里用
 * ALLOW_DAMAGE 做一个薄适配：伤害前触发、把传入的伤害当作实扣值，并始终放行。
 * 对溅射 / 吸血这类效果来说，差的是护甲减免那一点数值，触发时机与目标不会错。
 */
public final class DamageEvents {
	/** 参数顺序与 26.3 的 AFTER_DAMAGE 保持一致，调用方不用改 lambda 体。 */
	@FunctionalInterface
	public interface AfterDamage {
		void afterDamage(LivingEntity entity, DamageSource source, float blockedDamage, float damageTaken,
				boolean blocked);
	}

	private DamageEvents() {
	}

	public static void registerAfterDamage(AfterDamage callback) {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			callback.afterDamage(entity, source, 0.0F, amount, false);
			return true;
		});
	}
}
