package com.sephiria.client.backpack;

import com.sephiria.backpack.EnchantMenu;
import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.RotateSlatePayload;
import com.sephiria.network.ScrollBackpackPayload;
import com.sephiria.network.UpgradeArtifactPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/** 附魔面板：滚轮翻行、悬停石板 R 旋转、左侧升级按钮。 */
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
					button -> ClientPlayNetworking.send(new UpgradeArtifactPayload(levels)))
					.bounds(this.leftPos + EnchantMenu.BUTTON_X,
							this.topPos + EnchantMenu.BUTTON_Y + index * EnchantMenu.BUTTON_SPACING,
							EnchantMenu.BUTTON_WIDTH, EnchantMenu.BUTTON_HEIGHT).build());
		}
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
