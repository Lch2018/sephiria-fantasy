package com.sephiria.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sephiria.client.render.WeaponGlint;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把匕首 / 刀的物品光效换成自定义颜色。
 *
 * <p>26.1.2 的附魔光效不是烘焙进模型的数据：{@code ItemFeatureRenderer.renderItem} 在画每个
 * quad 的 glint 那一遍时，才按 quad 的 {@code itemRenderType} 去 {@code RenderTypes.glint()}
 * 取紫色渲染类型。所以在 {@code getFoilBuffer} 这一调用点按<b>quad 的贴图名</b>分流，
 * 命中 {@link WeaponGlint} 就用它自己建的渲染类型；其它物品原样走原版。
 *
 * <p>关不显示由物品自己的 {@code isFoil} 控制：匕首有专注、刀满剑意时返回 true，
 * 渲染器才会走 glint 那一遍。
 */
@Mixin(ItemFeatureRenderer.class)
public class WeaponGlintMixin {
	// require = 0：万一以后原版改了这个调用的形状，退化成「没有自定义光效」而不是整局起不来
	@Redirect(method = "renderItem",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
			require = 0)
	private static VertexConsumer sephiria$replaceGlint(MultiBufferSource buffers, RenderType itemType,
			PoseStack.Pose decalPose, MultiBufferSource.BufferSource bufferSource,
			OutlineBufferSource outlineBufferSource, SubmitNodeStorage.ItemSubmit submit) {
		RenderType glint = sephiria$glintFor(submit, itemType);

		if (glint == null) {
			// 原版三参 getFoilBuffer 的逻辑：物品光效永远走 getFoilRenderType(type, true)
			glint = ItemFeatureRenderer.getFoilRenderType(itemType, true);
		}

		VertexConsumer consumer = buffers.getBuffer(glint);

		// 原版对 SPECIAL foil 会套一层贴花坐标生成器（见 renderItem 里的 computeFoilDecalPose）
		return decalPose == null ? consumer : new SheetedDecalTextureGenerator(consumer, decalPose, 0.0078125F);
	}

	/** 在这一 quad 的渲染类型对应的贴图里找自定义微光。 */
	private static RenderType sephiria$glintFor(SubmitNodeStorage.ItemSubmit submit, RenderType itemType) {
		for (BakedQuad quad : submit.quads()) {
			BakedQuad.MaterialInfo info = quad.materialInfo();

			if (info.itemRenderType() == itemType) {
				RenderType glint = WeaponGlint.forTexture(info.sprite().contents().name());

				if (glint != null) {
					return glint;
				}
			}
		}

		return null;
	}
}
