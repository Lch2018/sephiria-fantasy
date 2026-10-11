package com.sephiria.mixin;

import com.sephiria.debuff.Debuffs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 冻住的生物<b>一步都挪不了</b>：把位置变化的总闸门拉住。
 *
 * <p>拦住 AI（{@code MobStunMixin} 取消 {@code serverAiStep}）只解决了「它自己想动」，
 * 解决不了「别人让它动」——击退（受伤那一下的 {@code knockback}）、重力、水流推动、
 * 被别的实体挤开、史莱姆那种自己 {@code setDeltaMovement} 的跳动，全都绕开 AI。
 * 而它们最终都要走 {@link Entity#move(MoverType, Vec3)} 才能真的改变坐标，所以在这里拦：
 * 冻住期间位置<b>完全不变</b>，连被打飞都不动——用户要的就是「完全静止」。
 *
 * <p>悬空被冻住的怪会停在半空（定格），解冻那一刻才继续掉；要改成「照常下落」就放行竖直分量
 * （这里直接 {@code return} 不取消，或按 {@code movement.y} 判断）。
 *
 * <p>附件只在服务端有，客户端这一层恒为 false；而且客户端本来就不给远程生物跑物理，不受影响。
 */
@Mixin(Entity.class)
public class FrozenMotionMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void sephiria$frozenCannotMove(MoverType type, Vec3 movement, CallbackInfo callback) {
		Entity self = (Entity) (Object) this;

		if (self instanceof LivingEntity living && Debuffs.isFrozen(living)) {
			callback.cancel();
		}
	}
}
