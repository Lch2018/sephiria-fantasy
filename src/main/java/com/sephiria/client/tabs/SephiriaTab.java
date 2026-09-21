package com.sephiria.client.tabs;

import net.minecraft.network.chat.Component;

/** SEPHIRIA 的三个页面标签：原版背包 / 赛菲利亚背包 / 属性。 */
public enum SephiriaTab {
	VANILLA("screen.sephiria.tab.vanilla"),
	BACKPACK("screen.sephiria.backpack"),
	ATTRIBUTES("screen.sephiria.stats");

	private final String titleKey;

	SephiriaTab(String titleKey) {
		this.titleKey = titleKey;
	}

	/** 悬停时显示的名字。 */
	public Component title() {
		return Component.translatable(this.titleKey);
	}
}
