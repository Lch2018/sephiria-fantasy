package com.sephiria.client.gui;

import com.sephiria.client.skill.ClientSkills;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/**
 * 配方页：左边列出六把基础武器，鼠标指到哪一把就在右边画出它的合成表（原版配方书那种 3x3 格子）。
 *
 * <p>原版「进度」界面只能画图标与文字，画不了合成格，所以合成表单独开了这一页——和属性页、
 * 技能页同一排标签，按背包键（默认 E）退出。
 *
 * <p>这里的图样是<b>照着</b> {@code data/sephiria/recipe/*.json} 抄的一份，改合成表时要两边一起改
 * （三个材料符号的含义：W = 原版铁制武器、I = 铁锭、S = 任意树苗；展示时用铁剑 / 铁锭 / 橡树苗
 * 代表「任意」）。之所以不在客户端读数据包，是为了不依赖玩家当前世界的配方管理器。
 */
public class RecipeScreen extends Screen {
	/** 一把武器的配方：物品 id、图样（三行，空格 = 空位）。 */
	private record Recipe(String id, String[] pattern) {
	}

	private static final List<Recipe> RECIPES = List.of(
			new Recipe("sephiria:default_sword_and_shield", new String[]{ " W ", "IWI", " S " }),
			new Recipe("sephiria:steel_greatsword", new String[]{ "WWW", " I ", " S " }),
			new Recipe("sephiria:dagger", new String[]{ "IW", " S" }),
			new Recipe("sephiria:colossal_crossbow", new String[]{ "SIS", "WWW", " S " }),
			new Recipe("sephiria:blade", new String[]{ " WW", " I ", "S  " }),
			new Recipe("sephiria:quarterstaff", new String[]{ "IWI", " S ", " S " }));

	/** 一个格子 20x20，格子间距 2。 */
	private static final int CELL = 22;
	private static final int LIST_X = 40;
	private static final int LIST_TOP = 56;
	private static final int GRID_X = 190;
	private static final int GRID_TOP = 60;

	/** 鼠标指着第几条（-1 = 没指）。 */
	private int hovered = -1;

	public RecipeScreen() {
		super(Component.translatable("screen.sephiria.recipes"));
	}

	@Override
	protected void init() {
		SephiriaTabs.add(this, this.width / 2 - 100, 14, SephiriaTab.RECIPES);
		addRenderableWidget(new StringWidget(LIST_X, LIST_TOP - 16, 140, 12,
				Component.translatable("screen.sephiria.recipes.list"), this.font));
		addRenderableWidget(new StringWidget(GRID_X, GRID_TOP - 16, 160, 12,
				Component.translatable("screen.sephiria.recipes.preview"), this.font));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
		this.hovered = -1;

		// 左列：六个武器的图标 + 名字
		for (int index = 0; index < RECIPES.size(); index++) {
			Recipe recipe = RECIPES.get(index);
			int y = LIST_TOP + index * 22;
			boolean over = mouseX >= LIST_X && mouseX < LIST_X + 130 && mouseY >= y && mouseY < y + 20;

			if (over) {
				this.hovered = index;
			}

			drawSlot(extractor, LIST_X, y, over ? 0xFF3A4A5A : 0xFF202030);
			drawItem(extractor, item(recipe.id()), LIST_X + 3, y + 3);
			extractor.text(this.font, item(recipe.id()).getName(item(recipe.id()).getDefaultInstance()),
					LIST_X + 26, y + 6, over ? 0xFFFFFF55 : 0xFFAAAAAA);
		}

		// 右边：鼠标指着的那把武器的 3x3 合成表
		Recipe show = RECIPES.get(this.hovered < 0 ? 0 : this.hovered);

		if (this.hovered < 0) {
			extractor.text(this.font, Component.translatable("screen.sephiria.recipes.hint"), GRID_X, GRID_TOP,
					0xFF808080);
			return;
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				int x = GRID_X + column * CELL;
				int y = GRID_TOP + row * CELL;
				drawSlot(extractor, x, y, 0xFF202030);

				String pattern = show.pattern()[row];
				char symbol = column < pattern.length() ? pattern.charAt(column) : ' ';

				if (symbol != ' ') {
					drawItem(extractor, material(symbol), x + 3, y + 3);
				}
			}
		}

		// 箭头 + 结果格
		int arrowX = GRID_X + 3 * CELL + 8;
		int arrowY = GRID_TOP + CELL;
		extractor.fill(arrowX, arrowY + 8, arrowX + 20, arrowY + 12, 0xFFAAAAAA);
		extractor.fill(arrowX + 16, arrowY + 4, arrowX + 20, arrowY + 16, 0xFFAAAAAA);
		drawSlot(extractor, arrowX + 28, GRID_TOP + CELL, 0xFF202030);
		drawItem(extractor, item(show.id()), arrowX + 31, GRID_TOP + CELL + 3);
	}

	/** 材料符号 → 展示用的代表物品。 */
	private static Item material(char symbol) {
		return switch (symbol) {
			case 'W' -> Items.IRON_SWORD;
			case 'I' -> Items.IRON_INGOT;
			default -> Items.OAK_SAPLING;
		};
	}

	private static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
	}

	/** 格子的底色 + 描边（照抄物品栏格子的画法）。 */
	private void drawSlot(GuiGraphicsExtractor extractor, int x, int y, int colour) {
		extractor.fill(x, y, x + CELL - 2, y + CELL - 2, 0xFF373737);
		extractor.fill(x + 1, y + 1, x + CELL - 3, y + CELL - 3, colour);
	}

	private void drawItem(GuiGraphicsExtractor extractor, Item item, int x, int y) {
		if (item != Items.AIR) {
			extractor.blit(RenderPipelines.GUI_TEXTURED, ClientSkills.iconOf(item), x, y, 0.0F, 0.0F, 16, 16, 16, 16);
		}
	}

	/** 按「背包键」（默认 E）关掉这一页回到游戏，与另外几页保持一致。 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (Minecraft.getInstance().options.keyInventory.matches(event)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(event);
	}

}

