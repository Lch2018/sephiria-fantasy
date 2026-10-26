package com.sephiria.compat.modmenu;

import com.sephiria.client.config.SephiriaSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 入口：在「模组菜单」里点赛菲莉娅的齿轮，直接打开「赛菲莉娅 Mod 设置」。
 *
 * <p>这是可选兼容——ModMenu 没装时 {@code fabric.mod.json} 里的 {@code modmenu} 入口
 * 不会被加载，本类也不会被引用。编译期依赖用 {@code compileOnly}，不会打进 jar。
 */
public class SephiriaModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new SephiriaSettingsScreen(parent);
	}
}
