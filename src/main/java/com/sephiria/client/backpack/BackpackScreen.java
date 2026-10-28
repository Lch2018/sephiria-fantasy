package com.sephiria.client.backpack;

import com.sephiria.backpack.ArtifactBackpackMenu;
import com.sephiria.backpack.BackpackWindow;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.ScrollBackpackPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/** 赛菲莉娅背包：滚轮翻行，悬停石板按 R 旋转。 */
public class BackpackScreen extends SephiriaMenuScreen<ArtifactBackpackMenu> {
	public BackpackScreen(ArtifactBackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ArtifactBackpackMenu.IMAGE_WIDTH,
				ArtifactBackpackMenu.imageHeight(BackpackWindow.ROWS), true);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0D) {
			ClientPlayNetworking.send(new ScrollBackpackPayload(scrollY > 0.0D ? -1 : 1));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null) {
			ClientPlayNetworking.send(new RotateSlatePayload(hovered.index));
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
