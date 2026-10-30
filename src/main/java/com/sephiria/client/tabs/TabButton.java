package com.sephiria.client.tabs;

import com.sephiria.Sephiria;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 标签页按钮：一个只画图标的小方块，当前页那一格描一圈亮边
 * （观感照抄 26.3 的 TabButton：只有图标，当前页有高亮描边）。
 *
 * <p>图标两种来源：贴图（背包 / 属性 / 天赋 / 技能 / 商店），或者一个原版物品
 * （原版背包那一页直接用木箱的图标，与 26.3 一致）。
 */
public class TabButton extends Button {
	public static final ResourceLocation BACKPACK_ICON = ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/backpack_tab.png");
	public static final ResourceLocation ATTRIBUTES_ICON = ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/attributes_tab.png");
	public static final ResourceLocation TALENT_ICON = ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/talent_tab.png");
	public static final ResourceLocation SKILLS_ICON = ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/item/haste_grimoire.png");
	public static final ResourceLocation SHOP_ICON = ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/shop_tab.png");
	public static final int SIZE = 20;

	private static final int ICON_SIZE = 18;
	/** 原版物品图标是按 16×16 画的，居中对齐。 */
	private static final int ITEM_SIZE = 16;
	/** 当前页那一格的高亮描边（与 26.3 一致）。 */
	private static final int ACTIVE_FRAME = 0xFFE8E8E8;
	private static final int BACKGROUND = 0xFF2A2A30;
	private static final int HOVER_BACKGROUND = 0xFF6A5AA8;

	private final ResourceLocation icon;
	private final ItemStack stack;
	private final boolean active;

	/** 贴图图标（保留旧构造：InventoryTabs 等现有调用点用）。 */
	public TabButton(int x, int y, ResourceLocation icon, Component title, OnPress press) {
		this(x, y, icon, null, title, false, press);
	}

	private TabButton(int x, int y, ResourceLocation icon, ItemStack stack, Component title,
			boolean active, OnPress press) {
		super(x, y, SIZE, SIZE, title, press, DEFAULT_NARRATION);
		this.icon = icon;
		this.stack = stack;
		this.active = active;
	}

	/** 贴图图标，{@code active} 时描亮边表示「当前就在这一页」。 */
	public static TabButton texture(int x, int y, ResourceLocation icon, Component title,
			boolean active, OnPress press) {
		return new TabButton(x, y, icon, null, title, active, press);
	}

	/** 原版物品图标（原版背包用木箱，省一张贴图）。 */
	public static TabButton item(int x, int y, ItemStack icon, Component title,
			boolean active, OnPress press) {
		return new TabButton(x, y, null, icon, title, active, press);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height,
				isHovered() && !this.active ? HOVER_BACKGROUND : BACKGROUND);

		if (this.stack != null) {
			graphics.renderItem(this.stack, getX() + (this.width - ITEM_SIZE) / 2,
					getY() + (this.height - ITEM_SIZE) / 2);
		} else {
			graphics.blit(this.icon, getX() + (this.width - ICON_SIZE) / 2,
					getY() + (this.height - ICON_SIZE) / 2, 0, 0, ICON_SIZE, ICON_SIZE,
					ICON_SIZE, ICON_SIZE);
		}

		if (this.active) {
			// 当前页：描一圈亮边，鼠标移上去也不会像可点的样子
			int left = getX();
			int top = getY();
			int right = left + this.width - 1;
			int bottom = top + this.height - 1;
			graphics.fill(left, top, right, top + 1, ACTIVE_FRAME);
			graphics.fill(left, bottom, right, bottom + 1, ACTIVE_FRAME);
			graphics.fill(left, top, left + 1, bottom, ACTIVE_FRAME);
			graphics.fill(right, top, right + 1, bottom, ACTIVE_FRAME);
		}
	}

	@Override
	protected void renderScrollingString(GuiGraphics graphics, net.minecraft.client.gui.Font font,
			int x, int y) {
	}
}
