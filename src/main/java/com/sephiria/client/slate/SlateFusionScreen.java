package com.sephiria.client.slate;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.FuseSlatesPayload;
import com.sephiria.network.RotateFusionSlatePayload;
import com.sephiria.slate.SlateFusionMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/** 石板合成界面：通用面板 + 「合成」按钮，鼠标悬停输入格时 R 旋转。 */
public class SlateFusionScreen extends SephiriaMenuScreen<SlateFusionMenu> {
	public SlateFusionScreen(SlateFusionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SlateFusionMenu.IMAGE_WIDTH, SlateFusionMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.slate_fusion.craft"),
				button -> ClientPlayNetworking.send(FuseSlatesPayload.CHANNEL,
						new FriendlyByteBuf(Unpooled.buffer())))
				.bounds(this.leftPos + SlateFusionMenu.BUTTON_X, this.topPos + SlateFusionMenu.BUTTON_Y,
						SlateFusionMenu.BUTTON_WIDTH, SlateFusionMenu.BUTTON_HEIGHT).build());
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Slot hovered = this.hoveredSlot;

		if (keyCode == GLFW.GLFW_KEY_R && hovered != null
				&& (hovered.index == SlateFusionMenu.INPUT_A || hovered.index == SlateFusionMenu.INPUT_B)) {
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			RotateFusionSlatePayload.write(buffer,
					new RotateFusionSlatePayload(hovered.index == SlateFusionMenu.INPUT_A ? 0 : 1));
			ClientPlayNetworking.send(RotateFusionSlatePayload.CHANNEL, buffer);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
