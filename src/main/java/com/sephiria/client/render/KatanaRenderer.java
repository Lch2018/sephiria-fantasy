package com.sephiria.client.render;

import com.sephiria.weapon.SephiriaKatanaItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** 刀的 GeckoLib 物品渲染器。 */
public class KatanaRenderer extends GeoItemRenderer<SephiriaKatanaItem> {
	public KatanaRenderer() {
		super(new KatanaModel());
		// 剑意满层时再画一遍蓝白微光（物品 isFoil 为 true 才提交）
		addRenderLayer(new WeaponGlintLayer(this));
	}
}
