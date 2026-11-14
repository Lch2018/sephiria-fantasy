package com.sephiria.client.render;

import com.sephiria.Sephiria;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 刀的 GeckoLib 模型。 */
public class KatanaModel extends GeoModel<SephiriaKatanaItem> {
	@Override
	public ResourceLocation getModelResource(SephiriaKatanaItem animatable) {
		// GeckoLib 4.8 的约定：geo/<路径>.geo.json，实际文件在 assets/<ns>/geo/ 下
		return new ResourceLocation(Sephiria.MOD_ID, "geo/katana.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SephiriaKatanaItem animatable) {
		// 3D 模型的 UV 排布用的是 katana.png；blade.png 只是 GUI/物品栏的平面图标
		return new ResourceLocation(Sephiria.MOD_ID, "textures/item/katana.png");
	}

	@Override
	public ResourceLocation getAnimationResource(SephiriaKatanaItem animatable) {
		// GeckoLib 4.8 的约定：animations/<路径>.animation.json，实际文件在 assets/<ns>/animations/ 下
		return new ResourceLocation(Sephiria.MOD_ID, "animations/katana.animation.json");
	}
}
