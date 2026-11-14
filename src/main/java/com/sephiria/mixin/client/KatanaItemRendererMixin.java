package com.sephiria.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.client.render.KatanaRenderer;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.client.RenderProvider;

/**
 * 让刀在手里拿的是 GeckoLib 的 3D 骨骼模型。
 *
 * <p>1.20.1 只有 {@code builtin/entity} 物品模型才会走到 BEWLR，而那样会让 GUI、地面、
 * 展示框也变成 3D——26.x 的 {@code minecraft:select} 能按 {@code display_context} 分开，
 * 1.20.1 不能。所以这里不动物品模型（GUI 继续用平面贴图），只在
 * {@code ItemRenderer.render} 开头截住四种「手持」上下文，改调刀的 GeckoLib 渲染器。
 *
 * <p>摆放顺序照抄原版剩余的流程：先套 {@code blade_in_hand} 的显示变换，再平移到
 * {@code -0.5}（让模型以原点为中心）——不这么做模型会悬在手上方半个方块。
 */
@Mixin(ItemRenderer.class)
public class KatanaItemRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void sephiria$renderBladeInHand(ItemStack stack, ItemDisplayContext context, boolean leftHand,
			PoseStack pose, MultiBufferSource buffer, int light, int overlay, BakedModel model, CallbackInfo ci) {
		if (stack.isEmpty() || !(stack.getItem() instanceof SephiriaKatanaItem item)) {
			return;
		}

		if (!isHandContext(context)) {
			return;
		}

		if (!(item.getRenderProvider().get() instanceof RenderProvider provider)) {
			return;
		}

		BlockEntityWithoutLevelRenderer renderer = provider.getCustomRenderer();

		if (renderer == null) {
			return;
		}

		pose.pushPose();
		KatanaRenderer.applyHandTransform(context, leftHand, pose);
		renderer.renderByItem(stack, context, pose, buffer, light, overlay);
		pose.popPose();
		ci.cancel();
	}

	private static boolean isHandContext(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
	}
}
