package com.sephiria.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 刀拿在手里时的蓝白微光。
 *
 * <p>GeckoLib 的模型不走原版物品 quad，所以 {@link WeaponGlint} 那套按贴图换 glint 的方式
 * 够不到手持模型。这里用 GeckoLib 自己的渲染层再画一遍模型：只有物品的 {@code isFoil}
 * （剑意满层）为真时才提交第二个 pass，渲染类型换成 {@link WeaponGlint#katanaGlint()}。
 *
 * <p>写法与 GeckoLib 内置的 {@code AutoGlowingGeoLayer} 同构：复用同一个 {@link BakedGeoModel}
 * 与当前姿势，所以光效会跟着挥刀动画走。
 */
public final class WeaponGlintLayer extends GeoRenderLayer<SephiriaKatanaItem> {
	public WeaponGlintLayer(GeoRenderer<SephiriaKatanaItem> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack pose, SephiriaKatanaItem animatable, BakedGeoModel model,
			RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
			int light, int overlay) {
		if (!(getRenderer() instanceof GeoItemRenderer<SephiriaKatanaItem> itemRenderer)) {
			return;
		}

		ItemStack stack = itemRenderer.getCurrentItemStack();

		if (stack == null || !animatable.isFoil(stack)) {
			return;
		}

		RenderType glint = WeaponGlint.katanaGlint();
		getRenderer().reRender(model, pose, bufferSource, animatable, glint,
				bufferSource.getBuffer(glint), partialTick, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
	}
}
