package com.sephiria.client.skill;

import com.sephiria.client.ClientSkills;
import com.sephiria.client.tabs.ScreenTabs;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.network.ArtifactSkillsPayload;
import com.sephiria.network.AssignArtifactSkillPayload;
import com.sephiria.network.OpenArtifactSkillsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** 简易神器技能页：6 个栏位 + 可用技能列表，先选栏位再点技能。 */
public class ArtifactSkillScreen extends Screen {
	private boolean backgroundDrawn;
	private static final int PANEL = 0xF0101014;
	private static final int BORDER = 0xFF4A4A52;
	private static final int PANEL_WIDTH = 320;
	private static final int PANEL_HEIGHT = 260;
	/** 面板上沿相对屏幕中心的偏移（现状：上面留 110、下面留 150）。 */
	private static final int PANEL_TOP_OFFSET = 110;
	/** 标签条与面板之间的空隙。 */
	private static final int TAB_GAP = 4;
	private int selectedSlot;

	public ArtifactSkillScreen() {
		super(Component.translatable("screen.sephiria_fantasy.artifact_skills"));
	}

	@Override
	protected void init() {
		super.init();
		ClientPlayNetworking.send(OpenArtifactSkillsPayload.INSTANCE);

		int left = panelLeft();
		int top = panelTop();

		// 顶部标签栏：高亮「神器技能」；放在面板上沿再往上 24 像素处，不压面板内容
		ScreenTabs.add(this, left + (PANEL_WIDTH - ScreenTabs.width()) / 2,
				top - ScreenTabs.HEIGHT - TAB_GAP, SephiriaTab.ARTIFACT_SKILLS);

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

	/** 面板左边（水平居中，宽 320）。 */
	private int panelLeft() {
		return this.width / 2 - PANEL_WIDTH / 2;
	}

	/**
	 * 面板上沿。屏幕够高时仍是原来的 {@code height / 2 - 110}；
	 * 太矮时往下挪，保证顶部标签条（{@link ScreenTabs#TOP_MARGIN} + 20 高）放得下、不压到面板。
	 */
	private int panelTop() {
		return Math.max(this.height / 2 - PANEL_TOP_OFFSET,
				ScreenTabs.TOP_MARGIN + ScreenTabs.HEIGHT + TAB_GAP);
	}

	private void refreshButtons() {
		this.clearWidgets();
		this.init();
	}

	private void assign(String entry) {
		ClientPlayNetworking.send(new AssignArtifactSkillPayload(this.selectedSlot, entry));
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!this.backgroundDrawn) {
			this.backgroundDrawn = true;
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.backgroundDrawn = false;
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		int left = panelLeft();
		int top = panelTop();
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PANEL);
		graphics.fill(left, top, left + PANEL_WIDTH, top + 1, BORDER);
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
