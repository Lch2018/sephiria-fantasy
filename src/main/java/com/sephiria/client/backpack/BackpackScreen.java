package com.sephiria.client.backpack;

import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.ScrollBackpackPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import com.sephiria.network.RotateSlatePayload;

/** 赛菲莉娅背包：通用面板 + 滚轮翻行。 */
public class BackpackScreen extends SephiriaMenuScreen<ArtifactBackpackMenu> {
	public BackpackScreen(ArtifactBackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ArtifactBackpackMenu.IMAGE_WIDTH,
				ArtifactBackpackMenu.imageHeight(BackpackWindow.ROWS), true);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			RotateSlatePayload.write(buffer, new RotateSlatePayload(hovered.index));
			ClientPlayNetworking.send(RotateSlatePayload.CHANNEL, buffer);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
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
