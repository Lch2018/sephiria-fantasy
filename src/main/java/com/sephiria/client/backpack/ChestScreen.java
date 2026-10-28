package com.sephiria.client.backpack;

import com.sephiria.backpack.ChestMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.ChestRerollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 宝箱界面：右上角刷新按钮。 */
public class ChestScreen extends SephiriaMenuScreen<ChestMenu> {
	public ChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ChestMenu.IMAGE_WIDTH, ChestMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.chest.reroll"),
				button -> ClientPlayNetworking.send(ChestRerollPayload.INSTANCE))
				.bounds(this.leftPos + ChestMenu.REROLL_BUTTON_X, this.topPos + ChestMenu.REROLL_BUTTON_Y,
						18, 18).build());
	}
}
