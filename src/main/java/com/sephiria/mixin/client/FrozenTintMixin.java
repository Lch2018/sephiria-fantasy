package com.sephiria.mixin.client;

import com.sephiria.client.ClientDebuffs;
import com.sephiria.client.DebuffLabelHolder;
import com.sephiria.debuff.Debuffs;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被冰冻（眩晕）的敌人染成<b>冰蓝色</b>——与「受伤时闪红」同一个思路，颜色换成冰。
 *
 * <p>原版那个红是 16×16 的 overlay 贴图（{@code OverlayTexture}）铺上去的，没有蓝色那一行、
 * 也不能按实体换色；模型本身倒有一个整块的 ARGB 乘算色
 * （{@code LivingEntityRenderer#getModelTint}，原版热带鱼与狼用它染色），所以走这里：
 * 颜色乘上冰蓝 #8FDCFF。
 *
 * <p>实体 id 是 {@code EntityRendererMixin} 在 {@code extractRenderState} 里塞进渲染状态的
 * （它比本注入点早一步跑），减益层数则是服务端推过来的 {@link ClientDebuffs} 镜像。
 *
 * <p>两点已知边界：① 只染<b>本体模型</b>，盔甲 / 手里的东西那些 RenderLayer 自带颜色，不跟着变；
 * ② 凡是不调 {@code super} 自己实现 {@code getModelTint} 的渲染器（原版只有狼与热带鱼）不会走到这里。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class FrozenTintMixin {
	/** 冰蓝：白 + 一点蓝，乘上去就是「冻住」的观感（红通道压到 0.56）。 */
	private static final int ICE_BLUE = 0xFF8FDCFF;

	@Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
	private void sephiria$iceBlueWhileFrozen(LivingEntityRenderState state,
			CallbackInfoReturnable<Integer> callback) {
		int entityId = ((DebuffLabelHolder) state).sephiria$entityId();

		if (entityId >= 0 && ClientDebuffs.has(Debuffs.FROZEN_ID, entityId)) {
			callback.setReturnValue(ARGB.multiply(callback.getReturnValueI(), ICE_BLUE));
		}
	}
}
