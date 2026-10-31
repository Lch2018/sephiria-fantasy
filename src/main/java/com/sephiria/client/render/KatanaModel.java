package com.sephiria.client.render;

import com.sephiria.Sephiria;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 刀的 GeckoLib 模型：模型/动画/贴图路径按 1.21.1 的 geckolib 目录规则。 */
public class KatanaModel extends GeoModel<SephiriaKatanaItem> {
	@Override
	public ResourceLocation getModelResource(SephiriaKatanaItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "geo/katana.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SephiriaKatanaItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/item/blade.png");
	}

	@Override
	public ResourceLocation getAnimationResource(SephiriaKatanaItem animatable) {
		return ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "animations/katana.animation.json");
	}
}
