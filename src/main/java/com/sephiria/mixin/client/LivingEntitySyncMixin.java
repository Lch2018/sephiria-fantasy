package com.sephiria.mixin.client;

import com.sephiria.compat.torohealth.ToroHealthCompatClient;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在实体同步数据更新时记下「当前是哪个实体」。
 *
 * <p>ToroHealth 的伤害数字粒子是在 {@code LivingEntity#onSyncedDataUpdated} 的健康值分支里
 * 同步创建的；粒子工厂本身只拿得到坐标，拿不到实体身份，所以同一次攻击的附加伤害 / 减益跳数
 * 很容易把主伤害的显示信息抢走。这里在方法开头记下实体 UUID，粒子工厂紧接着取走，
 * 从而能按 UUID 精确匹配。
 *
 * <p><b>不能用 {@code Entity#getId()}</b>：{@code LivingEntity} 构造期间就会通过
 * {@code setHealth} 触发同步数据更新，那时实体 id 还没分配，{@code getId()} 会抛
 * {@code IllegalStateException}；UUID 在 {@code Entity} 构造里更早赋值，构造期间也可用。
 *
 * <p>混入目标 {@code LivingEntity} 是原版类，没装 ToroHealth 时也会被应用，但这里只写一个
 * 我们自己类的静态字段，不碰 ToroHealth 的任何类型，所以没有类加载风险。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySyncMixin {
	@Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",
			at = @At("HEAD"))
	private void sephiria$rememberSyncedEntity(EntityDataAccessor<?> accessor, CallbackInfo callback) {
		LivingEntity entity = (LivingEntity) (Object) this;

		// 单人游戏里客户端与服务端共用同一个类：只记客户端侧的同步，免得服务端线程
		// 把 particleEntityUuid 改写成另一只实体。level 在 Entity 构造里比 setHealth 更早赋值，
		// 构造期触发同步时这里也是安全的。
		if (entity.level().isClientSide) {
			ToroHealthCompatClient.setParticleEntity(entity.getUUID());
		}
	}
}
