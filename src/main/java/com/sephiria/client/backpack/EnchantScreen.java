package com.sephiria.client.backpack;

import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.ScrollBackpackPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.UpgradeArtifactPayload;

/** 附魔界面：通用面板 + 滚轮翻行（背包格区与背包页共用同一套服务端滚动）。 */
public class EnchantScreen extends SephiriaMenuScreen<EnchantMenu> {
	public EnchantScreen(EnchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, EnchantMenu.IMAGE_WIDTH, EnchantMenu.imageHeight(), true);
	}

	@Override
	protected void init() {
		super.init();

		for (int index = 0; index < EnchantMenu.UPGRADE_STEPS.length; index++) {
			int levels = EnchantMenu.UPGRADE_STEPS[index];
			this.addRenderableWidget(Button.builder(
					Component.translatable("screen.sephiria_fantasy.enchant.upgrade_n", levels),
					button -> {
						FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
						UpgradeArtifactPayload.write(buffer, new UpgradeArtifactPayload(levels));
						ClientPlayNetworking.send(UpgradeArtifactPayload.CHANNEL, buffer);
					})
					.bounds(this.leftPos + EnchantMenu.BUTTON_X,
							this.topPos + EnchantMenu.BUTTON_Y + index * EnchantMenu.BUTTON_SPACING,
							EnchantMenu.BUTTON_WIDTH, EnchantMenu.BUTTON_HEIGHT).build());
		}
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
