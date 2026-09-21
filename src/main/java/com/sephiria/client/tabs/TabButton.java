package com.sephiria.client.tabs;

import com.sephiria.Sephiria;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 标签页按钮：一个只画图标的小方块，观感照抄原版创造模式物品栏的标签页（只有图标，悬停给名字）。
 *
 * <p>图标两种来源：贴图（背包 / 属性，均由 {@code tools/import-artifact-icons.ps1} 从参考图生成），
 * 或者一个原版物品（原版背包那一页直接用木箱的图标）。
 */
public class TabButton extends Button {
	/** 赛菲利亚背包的图标。 */
	public static final Identifier BACKPACK_ICON =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/backpack_tab.png");

	/** 属性页的图标。 */
	public static final Identifier ATTRIBUTES_ICON =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/attributes_tab.png");

	public static final int SIZE = 20;
	private static final int ICON_SIZE = 20;
	/** 当前页那一格的高亮描边。 */
	private static final int ACTIVE_FRAME = 0xFFE8E8E8;

	/** 木箱图标（原版物品，直接用原版渲染，不用自己画）。 */
	private static final ItemStack CHEST = new ItemStack(Items.CHEST);

	private final Identifier texture;
	private final ItemStack stack;
	private final boolean active;

	private TabButton(int x, int y, Identifier texture, ItemStack stack, Component tooltip, boolean active, OnPress onPress) {
		super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
		this.texture = texture;
		this.stack = stack;
		this.active = active;
		setTooltip(Tooltip.create(tooltip));
	}

	/** 贴图图标（背包 / 属性）。 */
	public static TabButton texture(int x, int y, Identifier icon, Component tooltip, boolean active, OnPress onPress) {
		return new TabButton(x, y, icon, null, tooltip, active, onPress);
	}

	/** 物品图标（原版背包用木箱）。 */
	public static TabButton item(int x, int y, ItemStack icon, Component tooltip, boolean active, OnPress onPress) {
		return new TabButton(x, y, null, icon, tooltip, active, onPress);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		int x = getX() + (getWidth() - ICON_SIZE) / 2;
		int y = getY() + (getHeight() - ICON_SIZE) / 2;

		if (this.stack != null) {
			extractor.item(this.stack, x, y);
		} else {
			extractor.blit(RenderPipelines.GUI_TEXTURED, this.texture, x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE,
					ICON_SIZE, ICON_SIZE);
		}

		if (this.active) {
			// 当前页：描一圈亮边，鼠标移上去也不会像可点的样子
			int left = getX();
			int top = getY();
			int right = left + getWidth() - 1;
			int bottom = top + getHeight() - 1;
			extractor.fill(left, top, right, top + 1, ACTIVE_FRAME);
			extractor.fill(left, bottom, right, bottom + 1, ACTIVE_FRAME);
			extractor.fill(left, top, left + 1, bottom, ACTIVE_FRAME);
			extractor.fill(right, top, right + 1, bottom, ACTIVE_FRAME);
		}
	}
}
