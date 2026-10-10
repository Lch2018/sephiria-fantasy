package com.sephiria.client.shop;

import com.sephiria.client.ClientStats;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.shop.ShopMenu;
import com.sephiria.shop.ShopPrices;
import com.sephiria.shop.ShopStock;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 商店界面：6 个商品格（点一下买）+ 宝箱 / 药水 / 骰子 / 出售栏，右边是玩家背包。
 *
 * <p>面板不贴图，纯色块画（和背包、附魔面板一致）。每格下面写价格：买得起黄字、买不起红字，
 * 售罄的格子压一层暗色并写「已售罄」；宝箱格写剩余次数。
 *
 * <p>价格由物品自己算（{@link ShopPrices} 是纯函数，两边一致），谈判力的折扣用同步来的折扣值按
 * {@link ShopPrices#discounted} 折算；「买过几次」从菜单的数据槽读。
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int SLOT_COLOR = 0xFF2A2A30;
	private static final int SLOT_EDGE = 0xFF15151A;
	private static final int HEADER_COLOR = 0xFFFFD24A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int HINT_COLOR = 0xFF9A9AA2;
	/** 价格：买得起黄、买不起红。 */
	private static final int PRICE_OK = 0xFFFFD24A;
	private static final int PRICE_NO = 0xFFFF5555;
	/** 售罄：暗色蒙层 + 红字。 */
	private static final int SOLD_OUT_COLOR = 0xFFFF5555;
	private static final int SOLD_OUT_SHADE = 0x90101010;
	private static final int TAB_HEIGHT = 20;
	/** 小字缩放（价格与状态行）。 */
	private static final float SMALL_TEXT_SCALE = 0.75F;

	/** 「刷新」按钮：骰子栏有骰子时才可点。 */
	private Button refreshButton;

	public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ShopMenu.IMAGE_WIDTH, ShopMenu.imageHeight());
		this.inventoryLabelY = ShopMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		SephiriaTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, SephiriaTab.SHOP);

		Button refresh = Button.builder(Component.translatable("screen.sephiria_fantasy.shop.refresh_button"),
						pressed -> ClientPlayNetworking.send(RefreshShopPayload.INSTANCE))
				.bounds(this.leftPos + ShopMenu.REFRESH_BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT)
				.build();
		this.refreshButton = refresh;
		addRenderableWidget(refresh);

		Button sell = Button.builder(Component.translatable("screen.sephiria_fantasy.shop.sell_button"),
						pressed -> ClientPlayNetworking.send(SellItemPayload.INSTANCE))
				.bounds(this.leftPos + ShopMenu.BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT)
				.build();
		addRenderableWidget(sell);
	}

	/** 骰子栏空着时「刷新」置灰（服务端也会再判一次，这里只是显示）。 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		if (this.refreshButton != null) {
			this.refreshButton.active = this.menu.canRefresh();
		}

		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 与背包页同一套：容器界面原版只压暗不虚化，这里补上「属性 / 技能页」那套（虚化 + 25% 黑底纹），
		// 虚化会把 HUD 与物品栏一起糊掉（它们在界面之前抽取）
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;

		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 十二个功能槽（货架 10 + 骰子 + 出售）的底
		for (Slot slot : this.menu.slots) {
			int index = this.menu.slots.indexOf(slot);

			if (index >= ShopMenu.FUNCTION_SLOTS) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;
			extractor.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			extractor.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}
	}

	/** 标题行右边写叶子余额（标题本身由原版画在左上角）。 */
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		super.extractLabels(extractor, mouseX, mouseY);
		Component balance = Component.translatable("screen.sephiria_fantasy.shop.balance",
				Component.literal(Numbers.format(ClientStats.leaves())).withColor(HEADER_COLOR));
		// 右对齐，免得和左上角的标题挤在一起
		extractor.text(this.font, balance,
				this.leftPos + this.imageWidth - 8 - this.font.width(balance), this.topPos + 6, TEXT_COLOR);
	}

	/** 每格下面的价格 / 剩余次数，以及售罄蒙层。 */
	@Override
	protected void extractSlot(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY) {
		super.extractSlot(extractor, slot, mouseX, mouseY);

		int index = this.menu.slots.indexOf(slot);

		if (index >= ShopStock.SLOT_COUNT) {
			// 骰子栏 / 出售栏：图标下面写用途
			if (index == ShopMenu.DICE_SLOT) {
				drawSmall(extractor, Component.translatable("screen.sephiria_fantasy.shop.dice"), slot.x, slot.y + 18,
						HINT_COLOR);
			} else if (index == ShopMenu.SELL_SLOT) {
				drawSmall(extractor, Component.translatable("screen.sephiria_fantasy.shop.sell"), slot.x, slot.y + 18,
						HINT_COLOR);
			}

			return;
		}

		ItemStack stack = slot.getItem();

		if (stack.isEmpty() || this.menu.remaining(index) <= 0) {
			// 售罄，或者本来就还没货的药水格
			extractor.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SOLD_OUT_SHADE);
			boolean potion = stack.isEmpty() && index >= ShopStock.POTION_START;
			drawSmall(extractor,
					Component.translatable(potion ? "screen.sephiria_fantasy.shop.potion" : "screen.sephiria_fantasy.shop.sold_out"),
					slot.x, slot.y + 18, SOLD_OUT_COLOR);
			return;
		}

		int price = ShopPrices.discounted(ShopPrices.buy(stack), ClientStats.shopDiscount());
		drawSmall(extractor, Component.literal(String.valueOf(price)), slot.x, slot.y + 18,
				ClientStats.leaves() >= price ? PRICE_OK : PRICE_NO);

		// 宝箱格还能买几次：画在格子左上角（和背包的等级文字一样小），免得跟价格挤在一起
		if (ShopStock.isChestSlot(index)) {
			drawTiny(extractor, Component.literal("x" + this.menu.remaining(index)), slot.x + 1, slot.y + 1,
					HINT_COLOR);
		}
	}

	/** 格子下面的小字：以格子左上角为原点（容器内容是在面板坐标里画的），缩到 0.75 倍。 */
	private void drawSmall(GuiGraphicsExtractor extractor, Component text, int x, int y, int colour) {
		drawScaled(extractor, text, x, y, colour, SMALL_TEXT_SCALE);
	}

	/** 格子角上的小字：0.6 倍，不盖住图标。 */
	private void drawTiny(GuiGraphicsExtractor extractor, Component text, int x, int y, int colour) {
		drawScaled(extractor, text, x, y, colour, 0.6F);
	}

	private void drawScaled(GuiGraphicsExtractor extractor, Component text, int x, int y, int colour, float scale) {
		org.joml.Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		extractor.text(this.font, text, 0, 0, colour, true);
		pose.popMatrix();
	}

	/** 鼠标悬停在货架格上时，在下面补一行说明（价格 / 限购 / 状态）。 */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		super.extractTooltip(extractor, mouseX, mouseY);

		if (this.hoveredSlot == null) {
			return;
		}

		int index = this.menu.slots.indexOf(this.hoveredSlot);

		if (index < 0 || index >= ShopStock.SLOT_COUNT) {
			return;
		}

		ItemStack stack = this.hoveredSlot.getItem();
		Font font = this.font;
		Component line;

		if (stack.isEmpty()) {
			line = Component.translatable("screen.sephiria_fantasy.shop.sold_out").withColor(SOLD_OUT_COLOR);
		} else if (this.menu.remaining(index) <= 0) {
			line = Component.translatable("screen.sephiria_fantasy.shop.sold_out").withColor(SOLD_OUT_COLOR);
		} else {
			int price = ShopPrices.discounted(ShopPrices.buy(stack), ClientStats.shopDiscount());

			line = Component.translatable("screen.sephiria_fantasy.shop.buy_hint",
					Component.literal(String.valueOf(price)).withColor(PRICE_OK));
		}

		int width = font.width(line);
		int x = mouseX + 8;
		int y = mouseY + 12;
		extractor.fill(x - 3, y - 3, x + width + 3, y + 9, 0xF0100010);
		extractor.text(font, line, x, y, TEXT_COLOR);
	}
}
