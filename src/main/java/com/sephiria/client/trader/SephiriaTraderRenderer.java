package com.sephiria.client.trader;

import com.sephiria.Sephiria;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * 赛菲莉娅流浪商人的渲染器：模型、图层、动画全部用原版流浪商人那一套，只换贴图——
 * 贴图是把原版商人的蓝袍换成红袍（{@code tools/gen-red-trader-texture.ps1} 生成）。
 *
 * <p>羊驼不用专门的渲染器：披红毯是装备层画的（见 {@code trader.SephiriaTraderLlama}），
 * 直接用原版的 {@code LlamaRenderer} 配商队羊驼的模型层就行。
 */
public class SephiriaTraderRenderer extends WanderingTraderRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID,
			"textures/entity/sephiria_wandering_trader.png");

	public SephiriaTraderRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}
}
