package com.sephiria.client.render;

import com.sephiria.weapon.SephiriaKatanaItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** 刀的 GeckoLib 物品渲染器。 */
public class KatanaRenderer extends GeoItemRenderer<SephiriaKatanaItem> {
	public KatanaRenderer() {
		super(new KatanaModel());
	}
}
