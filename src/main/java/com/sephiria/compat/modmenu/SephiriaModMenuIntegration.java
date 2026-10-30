package com.sephiria.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu 入口：点「配置」打开 HUD 布局页（与 H 键是同一个界面）。 */
public class SephiriaModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return screen -> new com.sephiria.client.hud.HudConfigScreen(screen);
	}
}
