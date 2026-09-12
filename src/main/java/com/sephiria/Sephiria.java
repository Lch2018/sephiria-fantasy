package com.sephiria;

import com.sephiria.registry.ModCreativeTabs;
import com.sephiria.registry.ModItems;
import com.sephiria.weapon.WeaponBranch;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sephiria implements ModInitializer {
	public static final String MOD_ID = "sephiria";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModCreativeTabs.initialize();
		OffHandGuard.register();

		LOGGER.info("[SEPHIRIA] 武器系统已载入：{} 个分支，{} 把基础武器（锻造系统尚未实现）",
				WeaponBranch.values().length, ModItems.BASE_WEAPONS.size());
	}
}
