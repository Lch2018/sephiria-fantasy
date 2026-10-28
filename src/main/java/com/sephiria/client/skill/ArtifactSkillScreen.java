package com.sephiria.client.skill;

import com.sephiria.client.ClientSkills;
import com.sephiria.network.ArtifactSkillsPayload;
import com.sephiria.network.AssignArtifactSkillPayload;
import com.sephiria.network.OpenArtifactSkillsPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** 简易神器技能页：6 个栏位 + 可用技能列表，先选栏位再点技能。 */
public class ArtifactSkillScreen extends Screen {
	private static final int PANEL = 0xF0101014;
	private static final int BORDER = 0xFF4A4A52;
	private int selectedSlot;

	public ArtifactSkillScreen() {
		super(Component.translatable("screen.sephiria_fantasy.artifact_skills"));
	}

	@Override
	protected void init() {
		super.init();
		ClientPlayNetworking.send(OpenArtifactSkillsPayload.CHANNEL, new FriendlyByteBuf(Unpooled.buffer()));

		int left = this.width / 2 - 160;
		int top = this.height / 2 - 110;

		for (int index = 0; index < 6; index++) {
			int slot = index;
			this.addRenderableWidget(Button.builder(slotLabel(index),
					button -> {
						this.selectedSlot = slot;
						refreshButtons();
					}).bounds(left + 8 + index * 50, top + 28, 48, 20).build());
		}

		int y = top + 60;

		for (ArtifactSkillsPayload.Entry entry : ClientSkills.skills()) {
			if (y > top + 210) {
				break;
			}

			this.addRenderableWidget(Button.builder(
					Component.literal(ClientSkills.encode(entry)),
					button -> assign(ClientSkills.encode(entry)))
					.bounds(left + 8, y, 304, 18).build());
			y += 20;
		}

		this.addRenderableWidget(Button.builder(Component.literal("清空栏位"),
				button -> assign(""))
				.bounds(left + 8, top + 232, 80, 18).build());
	}

	private Component slotLabel(int index) {
		String entry = index < ClientSkills.slots().size() ? ClientSkills.slots().get(index) : "";
		Item item = ClientSkills.itemOf(entry);
		return Component.literal((index + 1) + (item == null ? ": -" : ": " + item.getDescription().getString()));
	}

	private void refreshButtons() {
		this.clearWidgets();
		this.init();
	}

	private void assign(String entry) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		AssignArtifactSkillPayload.write(buffer, new AssignArtifactSkillPayload(this.selectedSlot, entry));
		ClientPlayNetworking.send(AssignArtifactSkillPayload.CHANNEL, buffer);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		int left = this.width / 2 - 160;
		int top = this.height / 2 - 110;
		graphics.fill(left, top, left + 320, top + 260, PANEL);
		graphics.fill(left, top, left + 320, top + 1, BORDER);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawString(this.font, this.title, left + 8, top + 8, 0xFFFFFFFF, false);
		graphics.drawString(this.font, Component.literal("先选栏位，再点可用技能"), left + 100, top + 244,
				0xFFA0A0A0, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
