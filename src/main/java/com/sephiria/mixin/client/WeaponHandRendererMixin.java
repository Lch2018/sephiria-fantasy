package com.sephiria.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sephiria.client.render.WeaponHandModels;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 基础武器在手里拿 3D 模型、在物品栏 / 掉落时拿平面像素图标。
 *
 * <p>1.20.1 的物品模型没法按 {@code display_context} 分流，所以物品模型改成平面图标；
 * 这里只在四种「手持」上下文里换用 {@code *_in_hand} 的烘焙模型，递归调回
 * {@code ItemRenderer.render} 走原版同一套显示变换。递归调用会再次进入本 mixin，
 * 用 {@link ThreadLocal} 守卫放行内层。
 *
 * <p>刀不在这里：它走 GeckoLib 的 {@code KatanaItemRendererMixin}，模型是骨骼动画，
 * 不能当普通烘焙模型渲染。
 */
@Mixin(ItemRenderer.class)
public class WeaponHandRendererMixin {
	private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> false);

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void sephiria$renderWeaponInHand(ItemStack stack, ItemDisplayContext context, boolean leftHand,
			PoseStack pose, MultiBufferSource buffer, int light, int overlay, BakedModel model, CallbackInfo ci) {
		if (RENDERING.get() || stack.isEmpty() || stack.getItem() instanceof SephiriaKatanaItem
				|| !WeaponHandModels.isHandItem(stack.getItem())) {
			return;
		}

		if (!sephiria$isHandContext(context)) {
			return;
		}

		String name = WeaponHandModels.modelFor(stack, context);

		if (name == null) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		BakedModel handModel = client.getModelManager().getModel(WeaponHandModels.location(name));

		if (handModel == null || handModel == client.getModelManager().getMissingModel()) {
			return;
		}

		RENDERING.set(true);

		try {
			// 递归回到原版完整流程：显示变换、GUI 外发光、模型绘制都由它处理
			((ItemRenderer) (Object) this).render(stack, context, leftHand, pose, buffer, light, overlay, handModel);
		}
		finally {
			RENDERING.set(false);
		}

		ci.cancel();
	}

	private static boolean sephiria$isHandContext(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
	}
}
