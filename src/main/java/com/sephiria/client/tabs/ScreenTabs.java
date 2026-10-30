package com.sephiria.client.tabs;

import com.sephiria.client.gui.StatsScreen;
import com.sephiria.client.gui.TalentScreen;
import com.sephiria.client.skill.ArtifactSkillScreen;
import com.sephiria.network.OpenBackpackPayload;
import com.sephiria.network.OpenShopPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 属性页 / 天赋页 / 神器技能页顶部的六格标签栏，与 26.3 的 {@code SephiriaTabs} 对应：
 * 原版背包（木箱）/ 赛菲莉娅背包 / 属性 / 天赋 / 神器技能 / 商店。
 *
 * <p>三个页面各自在 {@code init()} 里调一次 {@link #add(Screen, SephiriaTab)}，
 * 传入自己这一页当高亮项；这样任意一页都能直接切到另外几页。
 */
public final class ScreenTabs {
	private static final int GAP = 1;
	/** 标签条到屏幕顶端的距离：属性页 / 天赋页的内容从 y=34 起，20 高刚好不压到内容。 */
	public static final int TOP_MARGIN = 6;
	/** 一排标签的高度（技能页要按它给标签留位置）。 */
	public static final int HEIGHT = TabButton.SIZE;

	private ScreenTabs() {
	}

	/** 在屏幕顶部居中画一排标签，{@code current} 是当前所在的页面。 */
	public static void add(Screen screen, SephiriaTab current) {
		add(screen, (screen.width - width()) / 2, TOP_MARGIN, current);
	}

	/** 在 (x, y) 处画一排标签；{@code current} 传 null 表示都不高亮。 */
	public static void add(Screen screen, int x, int y, SephiriaTab current) {
		int slot = 0;

		for (SephiriaTab tab : SephiriaTab.values()) {
			int tabX = x + slot * (TabButton.SIZE + GAP);
			boolean active = current != null && tab == current;
			// 用 Fabric 的 getButtons 而不是 addRenderableWidget：后者是 protected，
			// 这个辅助类不在 Screen 的子类里调不到（与 InventoryTabs 同一做法）。
			Screens.getButtons(screen).add(create(tabX, y, tab, active));
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
			case TALENT -> TabButton.texture(x, y, TabButton.TALENT_ICON, tab.title(), active, press);
			case ARTIFACT_SKILLS -> TabButton.texture(x, y, TabButton.SKILLS_ICON, tab.title(), active, press);
			case SHOP -> TabButton.texture(x, y, TabButton.SHOP_ICON, tab.title(), active, press);
		};
	}

	/**
	 * 切到某一页。
	 *
	 * <p>离开容器菜单（背包 / 商店）前必须先把容器在<b>服务端</b>也关掉：只换客户端界面的话，
	 * 服务端还认为玩家开着那个菜单，会继续往里同步格子，客户端菜单对不上号就直接崩
	 * （26.3 踩过：服务端按背包菜单发 slot 62，而客户端开着创造物品栏只有 46 格 → IndexOutOfBounds）。
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
			case VANILLA -> client.setScreen(client.player.isCreative()
					? new CreativeModeInventoryScreen(client.player,
							client.player.connection.enabledFeatures(), false)
					: new InventoryScreen(client.player));
			// 背包与商店都是容器菜单，只能由服务端打开，所以这里发包
			case BACKPACK -> ClientPlayNetworking.send(OpenBackpackPayload.INSTANCE);
			case ATTRIBUTES -> client.setScreen(new StatsScreen());
			case TALENT -> client.setScreen(new TalentScreen());
			// 技能列表由服务端算（客户端没有背包内容）；ArtifactSkillScreen.init() 会发
			// OpenArtifactSkillsPayload，这里只负责开页面，别重复发
			case ARTIFACT_SKILLS -> client.setScreen(new ArtifactSkillScreen());
			case SHOP -> ClientPlayNetworking.send(OpenShopPayload.INSTANCE);
		}
	}
}
