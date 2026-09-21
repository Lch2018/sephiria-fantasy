package com.sephiria.client.tabs;

import com.sephiria.client.gui.StatsScreen;
import net.minecraft.client.gui.components.Button;
import com.sephiria.network.OpenBackpackPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 三个页面的标签栏：原版背包（木箱）/ 赛菲利亚背包 / 属性。
 *
 * <p>原版背包、赛菲利亚背包（容器菜单）、属性面板是三个不同的界面，所以每个界面都在同一位置
 * 画出这三个标签，当前页那一格高亮；这样从任意一页都能直接切到另外两页。
 */
public final class SephiriaTabs {
	private static final int GAP = 1;

	private SephiriaTabs() {
	}

	/** 在 (x, y) 处画一排标签；{@code current} 是当前所在的页面。 */
	public static void add(Screen screen, int x, int y, SephiriaTab current) {
		int slot = 0;

		for (SephiriaTab tab : SephiriaTab.values()) {
			int tabX = x + slot * (TabButton.SIZE + GAP);
			boolean active = tab == current;
			Screens.getWidgets(screen).add(create(tabX, y, tab, active));
			slot++;
		}
	}

	/** 一排标签的总宽度（用来居中）。 */
	public static int width() {
		return SephiriaTab.values().length * TabButton.SIZE + (SephiriaTab.values().length - 1) * GAP;
	}

	private static TabButton create(int x, int y, SephiriaTab tab, boolean active) {
		// 当前页那一格点了不做事（否则会把自己重新打开一遍，界面闪一下）
		Button.OnPress press = active ? button -> { } : button -> open(tab);

		return switch (tab) {
			case VANILLA -> TabButton.item(x, y, new ItemStack(Items.CHEST), tab.title(), active, press);
			case BACKPACK -> TabButton.texture(x, y, TabButton.BACKPACK_ICON, tab.title(), active, press);
			case ATTRIBUTES -> TabButton.texture(x, y, TabButton.ATTRIBUTES_ICON, tab.title(), active, press);
		};
	}

	/** 切到某一页。 */
	public static void open(SephiriaTab tab) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		switch (tab) {
			case VANILLA -> client.setScreenAndShow(client.player.isCreative()
					? new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false)
					: new InventoryScreen(client.player));
			// 背包是容器菜单，只能由服务端打开，所以这里发包
			case BACKPACK -> ClientPlayNetworking.send(OpenBackpackPayload.INSTANCE);
			case ATTRIBUTES -> client.setScreenAndShow(new StatsScreen());
		}
	}
}
