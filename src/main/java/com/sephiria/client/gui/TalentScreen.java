package com.sephiria.client.gui;

import com.sephiria.client.ClientTalents;
import com.sephiria.network.TalentClickPayload;
import com.sephiria.talent.Talent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 简易天赋页：七条天赋各一行加减号，底部重置。 */
public class TalentScreen extends Screen {
	private static final int PANEL = 0xF0101014;
	private static final int BORDER = 0xFF4A4A52;
	private static final Talent[] TALENTS = Talent.values();

	public TalentScreen() {
		super(Component.translatable("screen.sephiria_fantasy.talent"));
	}

	@Override
	protected void init() {
		super.init();
		int left = this.width / 2 - 130;
		int top = this.height / 2 - 120;

		for (int index = 0; index < TALENTS.length; index++) {
			int row = index;
			int y = top + 34 + index * 22;
			this.addRenderableWidget(Button.builder(Component.literal("-1"),
					button -> send(TalentClickPayload.adjust(row, -1)))
					.bounds(left + 170, y, 24, 18).build());
			this.addRenderableWidget(Button.builder(Component.literal("+1"),
					button -> send(TalentClickPayload.adjust(row, 1)))
					.bounds(left + 196, y, 24, 18).build());
			this.addRenderableWidget(Button.builder(Component.literal("+5"),
					button -> send(TalentClickPayload.adjust(row, 5)))
					.bounds(left + 222, y, 24, 18).build());
		}

		this.addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.talent.reset"),
				button -> send(TalentClickPayload.resetAll()))
				.bounds(left + 12, top + 210, 60, 18).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		int left = this.width / 2 - 130;
		int top = this.height / 2 - 120;

		graphics.fill(left, top, left + 260, top + 236, PANEL);
		graphics.fill(left, top, left + 260, top + 1, BORDER);
		graphics.fill(left, top + 235, left + 260, top + 236, BORDER);
		super.render(graphics, mouseX, mouseY, partialTick);

		graphics.drawString(this.font, this.title, left + 12, top + 10, 0xFFFFFFFF, false);
		graphics.drawString(this.font,
				Component.translatable("screen.sephiria_fantasy.talent.remaining", ClientTalents.remaining()),
				left + 12, top + 22, 0xFFFFD060, false);

		for (int index = 0; index < TALENTS.length; index++) {
			Talent talent = TALENTS[index];
			int y = top + 34 + index * 22 + 5;
			int points = ClientTalents.points(talent);
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.talent." + talent.id()),
					left + 12, y, 0xFFD0D0D0, false);
			graphics.drawString(this.font, Component.literal(points + "  (阶段 " + ClientTalents.stage(talent) + ")"),
					left + 100, y, 0xFFFFFFFF, false);
		}

		if (!ClientTalents.nearSpawn()) {
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.talent.not_near_spawn"),
					left + 80, top + 214, 0xFFFF5555, false);
		}
	}

	private static void send(TalentClickPayload payload) {
		ClientPlayNetworking.send(payload);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
