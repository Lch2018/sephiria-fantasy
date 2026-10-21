package com.sephiria.client.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * 刀拿在手里时的蓝白微光。
 *
 * <p>GeckoLib 的模型不走原版物品 quad，所以 {@code MaterialInfo} 那套按贴图换 glint 的方式
 * 够不到手持模型。这里用 GeckoLib 自己的渲染层再画一遍模型：只有在
 * {@link DataTickets#HAS_GLINT} 为真（物品 {@code isFoil} 返回 true）时才提交第二个 pass，
 * 渲染类型换成 {@link WeaponGlint#katanaGlint()}。
 *
 * <p>写法照 GeckoLib 内置的 {@code TextureLayerGeoLayer}：`collector.order(1)` 让它排在
 * 正常模型之后，几何体与骨骼沿用同一个 {@link RenderPassInfo}，所以光效会跟着挥刀动画走。
 */
public final class WeaponGlintLayer extends GeoRenderLayer<SephiriaKatanaItem, GeoItemRenderer.RenderData,
		GeoRenderState> {
	public WeaponGlintLayer(GeoItemRenderer<SephiriaKatanaItem> renderer) {
		super(renderer);
	}

	@Override
	public void submitRenderTask(RenderPassInfo<GeoRenderState> pass, SubmitNodeCollector collector) {
		if (!pass.willRender()
				|| !Boolean.TRUE.equals(pass.renderState().getGeckolibData(DataTickets.HAS_GLINT))) {
			return;
		}

		getRenderer().submitRenderTasks(pass, collector.order(1), WeaponGlint.katanaGlint());
	}
}
