package com.sephiria.client.gui;

import com.sephiria.client.ClientStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * 属性面板：从背包顶部的「属性」标签切进来，顶部的「背包」标签切回去。
 *
 * <p>只用原版部件拼（文字用 StringWidget、切换用 Button），不自己写渲染——这样界面渲染管线的
 * 变动不会影响它。生命值直接读玩家的原版血量，其余读客户端的属性镜像。
 */
public class StatsScreen extends Screen {
	private static final int ROW_HEIGHT = 14;

	public StatsScreen() {
		super(Component.translatable("screen.sephiria.stats"));
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = 34;

		// 切回背包的标签：与背包界面里的「属性」标签位置对应
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria.stats.inventory"),
						button -> backToInventory())
				.bounds(left, 14, 60, 18)
				.build());

		var player = Minecraft.getInstance().player;
		String health = player == null
				? "-"
				: format(player.getHealth()) + " / " + format(player.getMaxHealth());

		row(left, y, "screen.sephiria.stats.hp", health);
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.mp", format(ClientStats.mp()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.mp_regen", format(ClientStats.mpRegen()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.physical", format(ClientStats.physical()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.fire", format(ClientStats.fire()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.ice", format(ClientStats.ice()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.lightning", format(ClientStats.lightning()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.defense", format(ClientStats.defense()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.attack_speed", format(ClientStats.attackSpeed()) + "%");
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.melee_range", format(ClientStats.meleeRange()) + "%");
	}

	private void row(int left, int y, String key, String value) {
		addRenderableWidget(new StringWidget(left, y, 200, ROW_HEIGHT,
				Component.translatable(key, value), this.font));
	}

	private void backToInventory() {
		Minecraft client = Minecraft.getInstance();

		if (client.player != null) {
			client.setScreenAndShow(new InventoryScreen(client.player));
		}
	}

	/** 属性都按整数显示（当前数值都是整的），需要小数时再改。 */
	private static String format(double value) {
		return String.valueOf(Math.round(value));
	}
}
