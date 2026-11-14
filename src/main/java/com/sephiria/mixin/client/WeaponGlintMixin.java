package com.sephiria.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sephiria.client.render.WeaponGlint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把匕首 / 刀的物品光效换成自定义颜色。
 *
 * <p>1.20.1 的附魔光效由 {@code ItemRenderer.getFoilBuffer / getFoilBufferDirect} 取
 * {@code RenderType.glint()}（贴图写死紫色），这两个静态方法本身不知道是哪个物品；
 * 所以在 {@code ItemRenderer.render} 头上记下当前 ItemStack，再把里面这两处调用按物品类
 * 分流到 {@link WeaponGlint} 自建的渲染类型。两种分支（direct / 非 direct）都换，
 * 否则物品栏与手持会一半紫一半红。
 *
 * <p>关不显示由物品自己的 {@code isFoil} 控制：匕首有专注、刀满剑意时返回 true，
 * 渲染器才会走 glint 那一遍。
 */
@Mixin(ItemRenderer.class)
public class WeaponGlintMixin {
	@Unique
	private static final ThreadLocal<ItemStack> SEPHIRIA_CURRENT_STACK = new ThreadLocal<>();

	@Inject(method = "render", at = @At("HEAD"))
	private void sephiria$captureStack(ItemStack stack, ItemDisplayContext context, boolean leftHand,
			PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		SEPHIRIA_CURRENT_STACK.set(stack);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void sephiria$clearStack(ItemStack stack, ItemDisplayContext context, boolean leftHand,
			PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
		SEPHIRIA_CURRENT_STACK.remove();
	}

	// require = 0：万一以后原版改了这个调用的形状，退化成「没有自定义光效」而不是整局起不来
	@Redirect(method = "render",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
			require = 0)
	private static VertexConsumer sephiria$foilBuffer(MultiBufferSource buffers, RenderType type,
			boolean isItem, boolean glint) {
		RenderType custom = glint ? WeaponGlint.forStack(SEPHIRIA_CURRENT_STACK.get(), false) : null;
		return custom != null ? buffers.getBuffer(custom)
				: ItemRenderer.getFoilBuffer(buffers, type, isItem, glint);
	}

	@Redirect(method = "render",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
			require = 0)
	private static VertexConsumer sephiria$foilBufferDirect(MultiBufferSource buffers, RenderType type,
			boolean isItem, boolean glint) {
		RenderType custom = glint ? WeaponGlint.forStack(SEPHIRIA_CURRENT_STACK.get(), true) : null;
		return custom != null ? buffers.getBuffer(custom)
				: ItemRenderer.getFoilBufferDirect(buffers, type, isItem, glint);
	}
}
