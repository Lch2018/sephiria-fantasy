package com.sephiria.client.tabs;

import com.sephiria.Sephiria;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 原版背包上沿的图标标签。 */
public class TabButton extends Button {
	public static final ResourceLocation BACKPACK_ICON = new ResourceLocation(Sephiria.MOD_ID, "textures/gui/backpack_tab.png");
	public static final ResourceLocation ATTRIBUTES_ICON = new ResourceLocation(Sephiria.MOD_ID, "textures/gui/attributes_tab.png");
	public static final ResourceLocation TALENT_ICON = new ResourceLocation(Sephiria.MOD_ID, "textures/gui/talent_tab.png");
	public static final ResourceLocation SKILLS_ICON = new ResourceLocation(Sephiria.MOD_ID, "textures/item/haste_grimoire.png");
	public static final ResourceLocation SHOP_ICON = new ResourceLocation(Sephiria.MOD_ID, "textures/gui/shop_tab.png");
	public static final int SIZE = 20;

	private final ResourceLocation icon;

	public TabButton(int x, int y, ResourceLocation icon, Component title, OnPress press) {
		super(x, y, SIZE, SIZE, title, press, DEFAULT_NARRATION);
		this.icon = icon;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height,
				isHovered() ? 0xFF6A5AA8 : 0xFF2A2A30);
		graphics.blit(this.icon, getX() + 1, getY() + 1, 0, 0, 18, 18, 18, 18);
	}

	@Override
	protected void renderScrollingString(GuiGraphics graphics, net.minecraft.client.gui.Font font,
			int x, int y) {
	}
}
