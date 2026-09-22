package com.sephiria.client.tabs;

import com.sephiria.client.gui.StatsScreen;
import com.sephiria.client.skill.ArtifactSkillScreen;
import com.sephiria.network.OpenArtifactSkillsPayload;
import net.minecraft.client.gui.components.Button;
import com.sephiria.network.OpenBackpackPayload;
import com.sephiria.network.OpenShopPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 四个页面的标签栏：原版背包（木箱）/ 赛菲利亚背包 / 属性 / 商店。
 *
 * <p>原版背包、赛菲利亚背包与商店（后两者都是容器菜单）、属性面板是四个不同的界面，所以每个界面
 * 都在同一位置画出这一排标签，当前页那一格高亮；这样从任意一页都能直接切到另外几页。
 */
public final class SephiriaTabs {
	private static final int GAP = 1;

	private SephiriaTabs() {
	}

	/** 在 (x, y) 处画一排标签；{@code current} 是当前所在的页面，传 null 表示都不是（三个都可点）。 */
	public static void add(Screen screen, int x, int y, SephiriaTab current) {
		int slot = 0;

		for (SephiriaTab tab : SephiriaTab.values()) {
			int tabX = x + slot * (TabButton.SIZE + GAP);
			boolean active = current != null && tab == current;
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
			// 技能页的图标直接用「急速」那本魔法书的贴图，不额外画一张
			case ARTIFACT_SKILLS -> TabButton.texture(x, y, TabButton.SKILLS_ICON, tab.title(), active, press);
			// 配方页用工作台图标（原版物品，不用另画图）
			case SHOP -> TabButton.texture(x, y, TabButton.SHOP_ICON, tab.title(), active, press);
		};
	}

	/**
	 * 切到某一页。
	 *
	 * <p>离开容器菜单（背包 / 附魔面板 / 商店）前必须先把容器在<b>服务端</b>也关掉：只换客户端界面的话，
	 * 服务端还认为玩家开着那个菜单，会继续往里同步格子，客户端菜单对不上号就直接崩
	 * （踩过：服务端按背包菜单发 slot 62，而客户端开着创造物品栏只有 46 格 → IndexOutOfBounds）。
	 */
	public static void open(SephiriaTab tab) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		// 目标是容器菜单（背包 / 商店）时不用先关：服务端会用 openMenu 顶掉当前那个
		boolean containerTarget = tab == SephiriaTab.BACKPACK || tab == SephiriaTab.SHOP;

		if (!containerTarget && client.player.containerMenu != client.player.inventoryMenu) {
			// closeContainer() 会发关闭包、顺手关掉当前界面，下面再切到目标界面
			client.player.closeContainer();
		}

		switch (tab) {
			case VANILLA -> client.setScreenAndShow(client.player.isCreative()
					? new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false)
					: new InventoryScreen(client.player));
			// 背包与商店都是容器菜单，只能由服务端打开，所以这里发包
			case BACKPACK -> ClientPlayNetworking.send(OpenBackpackPayload.INSTANCE);
			case ATTRIBUTES -> client.setScreenAndShow(new StatsScreen());
			// 技能列表由服务端算（客户端没有背包内容），所以先开页面再发请求，包一到就填上
			case ARTIFACT_SKILLS -> {
				client.setScreenAndShow(new ArtifactSkillScreen());
				ClientPlayNetworking.send(OpenArtifactSkillsPayload.INSTANCE);
			}
			case SHOP -> ClientPlayNetworking.send(OpenShopPayload.INSTANCE);
		}
	}
}
