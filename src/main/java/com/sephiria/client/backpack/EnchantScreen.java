package com.sephiria.client.backpack;

import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.ScrollBackpackPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 附魔界面：通用面板 + 滚轮翻行（背包格区与背包页共用同一套服务端滚动）。 */
public class EnchantScreen extends SephiriaMenuScreen<EnchantMenu> {
	public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight(), true);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (delta != 0.0D) {
			int rows = delta > 0.0D ? -1 : 1;
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			ScrollBackpackPayload.write(buffer, new ScrollBackpackPayload(rows));
			ClientPlayNetworking.send(ScrollBackpackPayload.CHANNEL, buffer);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}
}
