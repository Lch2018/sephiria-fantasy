package com.sephiria.client.tabs;

import net.minecraft.network.chat.Component;

/** SEPHIRIA 的五个页面标签：原版背包 / 赛菲利亚背包 / 属性 / 神器技能 / 商店。 */
public enum SephiriaTab {
	VANILLA("screen.sephiria_fantasy.tab.vanilla"),
	BACKPACK("screen.sephiria_fantasy.backpack"),
	ATTRIBUTES("screen.sephiria_fantasy.stats"),
	ARTIFACT_SKILLS("screen.sephiria_fantasy.artifact_skills"),
	SHOP("screen.sephiria_fantasy.shop");

	private final String titleKey;

	SephiriaTab(String titleKey) {
		this.titleKey = titleKey;
	}

	/** 悬停时显示的名字。 */
	public Component title() {
		return Component.translatable(this.titleKey);
	}
}
