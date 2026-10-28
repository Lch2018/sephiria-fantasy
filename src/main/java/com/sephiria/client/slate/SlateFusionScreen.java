package com.sephiria.client.slate;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.FuseSlatesPayload;
import com.sephiria.network.RotateFusionSlatePayload;
import com.sephiria.slate.SlateFusionMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/** 石板合成：合成按钮 + 悬停输入格按 R 旋转。 */
public class SlateFusionScreen extends SephiriaMenuScreen<SlateFusionMenu> {
	public SlateFusionScreen(SlateFusionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SlateFusionMenu.IMAGE_WIDTH, SlateFusionMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.slate_fusion.craft"),
				button -> ClientPlayNetworking.send(FuseSlatesPayload.INSTANCE))
				.bounds(this.leftPos + SlateFusionMenu.BUTTON_X, this.topPos + SlateFusionMenu.BUTTON_Y,
						SlateFusionMenu.BUTTON_WIDTH, SlateFusionMenu.BUTTON_HEIGHT).build());
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null
				&& (hovered.index == SlateFusionMenu.INPUT_A || hovered.index == SlateFusionMenu.INPUT_B)) {
			ClientPlayNetworking.send(new RotateFusionSlatePayload(
					hovered.index == SlateFusionMenu.INPUT_A ? 0 : 1));
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
