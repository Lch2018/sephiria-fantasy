package com.sephiria.client.tabs;

import net.minecraft.network.chat.Component;

/** SEPHIRIA 的五个页面标签：原版背包 / 赛菲利亚背包 / 属性 / 神器技能 / 商店。 */
public enum SephiriaTab {
	VANILLA("screen.sephiria.tab.vanilla"),
	BACKPACK("screen.sephiria.backpack"),
	ATTRIBUTES("screen.sephiria.stats"),
	ARTIFACT_SKILLS("screen.sephiria.artifact_skills"),
	RECIPES("screen.sephiria.recipes"),
	SHOP("screen.sephiria.shop");

	private final String titleKey;

	SephiriaTab(String titleKey) {
		this.titleKey = titleKey;
	}

	/** 悬停时显示的名字。 */
	public Component title() {
		return Component.translatable(this.titleKey);
	}
}
