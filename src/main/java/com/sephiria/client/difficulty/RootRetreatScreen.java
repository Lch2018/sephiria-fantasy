package com.sephiria.client.difficulty;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.difficulty.DifficultyOption;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.network.RootRetreatClickPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 根之退界面：每个难度选项一行加减号，底部「清空 / 应用」。 */
public class RootRetreatScreen extends SephiriaMenuScreen<RootRetreatMenu> {
	private static final int ROW_TOP = 24;
	private static final int ROW_HEIGHT = 22;
	private static final DifficultyOption[] OPTIONS = DifficultyOption.values();

	public RootRetreatScreen(RootRetreatMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, RootRetreatMenu.IMAGE_WIDTH, 220, false);
	}

	@Override
	protected void init() {
		super.init();

		for (int index = 0; index < OPTIONS.length; index++) {
			int optionIndex = index;
			int y = this.topPos + ROW_TOP + index * ROW_HEIGHT;
			this.addRenderableWidget(Button.builder(Component.literal("-"),
					button -> send(RootRetreatClickPayload.adjust(optionIndex, -1)))
					.bounds(this.leftPos + 120, y, 18, 18).build());
			this.addRenderableWidget(Button.builder(Component.literal("+"),
					button -> send(RootRetreatClickPayload.adjust(optionIndex, 1)))
					.bounds(this.leftPos + 142, y, 18, 18).build());
		}

		int bottom = this.topPos + 180;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.root_retreat.clear"),
				button -> send(RootRetreatClickPayload.clear()))
				.bounds(this.leftPos + 8, bottom, 70, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.root_retreat.apply"),
				button -> send(RootRetreatClickPayload.apply()))
				.bounds(this.leftPos + 84, bottom, 70, 18).build());
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);

		for (int index = 0; index < OPTIONS.length; index++) {
			DifficultyOption option = OPTIONS[index];
			int level = this.menu.blockLevelOf(option);
			int y = ROW_TOP + index * ROW_HEIGHT + 5;
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.root_retreat.option." + option.id()),
				8, y, 0xFFD0D0D0, false);
			graphics.drawString(this.font, Component.literal(String.valueOf(level)),
					108, y, 0xFFFFFFFF, false);
		}
	}

	private static void send(RootRetreatClickPayload payload) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		RootRetreatClickPayload.write(buffer, payload);
		ClientPlayNetworking.send(RootRetreatClickPayload.CHANNEL, buffer);
	}
}
