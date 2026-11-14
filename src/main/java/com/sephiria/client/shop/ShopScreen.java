package com.sephiria.client.shop;

import com.sephiria.client.ClientStats;
import com.sephiria.client.tabs.ScreenTabs;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.network.RefreshShopPayload;
import com.sephiria.network.SellItemPayload;
import com.sephiria.shop.ShopMenu;
import com.sephiria.shop.ShopPrices;
import com.sephiria.shop.ShopStock;
import com.sephiria.util.Numbers;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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
 *
 * <p>1.20.1 的 {@code renderSlot} 是私有的，格内浮层由 {@code ShopSlotOverlayMixin} 在它返回后调
 * {@link #drawSlotDecoration} 画——顺序与原版一致：图标在上、蒙层与价格压在最上面。
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
		super(menu, inventory, title);
		this.imageWidth = ShopMenu.IMAGE_WIDTH;
		this.imageHeight = ShopMenu.imageHeight();
		this.inventoryLabelY = ShopMenu.playerRowsY() - 11;
	}

	@Override
	protected void init() {
		super.init();
		ScreenTabs.add(this, this.leftPos, this.topPos - TAB_HEIGHT, SephiriaTab.SHOP);

		Button refresh = Button.builder(Component.translatable("screen.sephiria_fantasy.shop.refresh_button"),
						pressed -> ClientPlayNetworking.send(RefreshShopPayload.CHANNEL,
								new FriendlyByteBuf(Unpooled.buffer())))
				.bounds(this.leftPos + ShopMenu.REFRESH_BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT)
				.build();
		this.refreshButton = refresh;
		addRenderableWidget(refresh);

		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.shop.sell_button"),
						pressed -> ClientPlayNetworking.send(SellItemPayload.CHANNEL,
								new FriendlyByteBuf(Unpooled.buffer())))
				.bounds(this.leftPos + ShopMenu.BUTTON_X, this.topPos + ShopMenu.BUTTON_Y,
						ShopMenu.BUTTON_WIDTH, ShopMenu.BUTTON_HEIGHT)
				.build());
	}

	/** 骰子栏空着时「刷新」置灰（服务端也会再判一次，这里只是显示）。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.refreshButton != null) {
			this.refreshButton.active = this.menu.canRefresh();
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;

		graphics.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		graphics.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		// 十二个功能槽（货架 10 + 骰子 + 出售）的底
		for (Slot slot : this.menu.slots) {
			int index = this.menu.slots.indexOf(slot);

			if (index >= ShopMenu.FUNCTION_SLOTS) {
				continue;
			}

			int x = left + slot.x;
			int y = top + slot.y;
			graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
			graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
		}
	}

	/** 标题行右边写叶子余额（标题本身由原版画在左上角）。 */
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		Component balance = Component.translatable("screen.sephiria_fantasy.shop.balance",
				Component.literal(Numbers.format(ClientStats.leaves()))
						.withStyle(style -> style.withColor(TextColor.fromRgb(HEADER_COLOR))));
		// 右对齐，免得和左上角的标题挤在一起
		graphics.drawString(this.font, balance,
				this.imageWidth - 8 - this.font.width(balance), 6, TEXT_COLOR, false);
	}

	/** 每格下面的价格 / 剩余次数，以及售罄蒙层。由 {@code ShopSlotOverlayMixin} 在图标画完后调用。 */
	public void drawSlotDecoration(GuiGraphics graphics, Slot slot) {
		int index = this.menu.slots.indexOf(slot);

		if (index >= ShopStock.SLOT_COUNT) {
			// 骰子栏 / 出售栏：图标下面写用途
			if (index == ShopMenu.DICE_SLOT) {
				drawSmall(graphics, Component.translatable("screen.sephiria_fantasy.shop.dice"),
						slot.x, slot.y + 18, HINT_COLOR);
			} else if (index == ShopMenu.SELL_SLOT) {
				drawSmall(graphics, Component.translatable("screen.sephiria_fantasy.shop.sell"),
						slot.x, slot.y + 18, HINT_COLOR);
			}

			return;
		}

		ItemStack stack = slot.getItem();

		if (stack.isEmpty() || this.menu.remaining(index) <= 0) {
			// 售罄，或者本来就还没货的药水格
			graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, SOLD_OUT_SHADE);
			boolean potion = stack.isEmpty() && index >= ShopStock.POTION_START;
			drawSmall(graphics,
					Component.translatable(potion ? "screen.sephiria_fantasy.shop.potion"
							: "screen.sephiria_fantasy.shop.sold_out"),
					slot.x, slot.y + 18, SOLD_OUT_COLOR);
			return;
		}

		int price = ShopPrices.discounted(ShopPrices.buy(stack), ClientStats.shopDiscount());
		drawSmall(graphics, Component.literal(String.valueOf(price)), slot.x, slot.y + 18,
				ClientStats.leaves() >= price ? PRICE_OK : PRICE_NO);

		// 宝箱格还能买几次：画在格子左上角（和背包的等级文字一样小），免得跟价格挤在一起
		if (index >= ShopStock.CHEST_START && index < ShopStock.POTION_START) {
			drawTiny(graphics, Component.literal("x" + this.menu.remaining(index)),
					slot.x + 1, slot.y + 1, HINT_COLOR);
		}
	}

	/** 格子下面的小字：以格子左上角为原点（容器内容是在面板坐标里画的），缩到 0.75 倍。 */
	private void drawSmall(GuiGraphics graphics, Component text, int x, int y, int colour) {
		drawScaled(graphics, text, x, y, colour, SMALL_TEXT_SCALE);
	}

	/** 格子角上的小字：0.6 倍，不盖住图标。 */
	private void drawTiny(GuiGraphics graphics, Component text, int x, int y, int colour) {
		drawScaled(graphics, text, x, y, colour, 0.6F);
	}

	private void drawScaled(GuiGraphics graphics, Component text, int x, int y, int colour, float scale) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, colour, true);
		graphics.pose().popPose();
	}

	/** 鼠标悬停在货架格上时，在下面补一行说明（价格 / 限购 / 状态）。 */
	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);

		if (this.hoveredSlot == null) {
			return;
		}

		int index = this.menu.slots.indexOf(this.hoveredSlot);

		if (index < 0 || index >= ShopStock.SLOT_COUNT) {
			return;
		}

		ItemStack stack = this.hoveredSlot.getItem();
		int x = mouseX + 8;
		int y = mouseY + 12;

		if (stack.isEmpty() || this.menu.remaining(index) <= 0) {
			Component line = Component.translatable("screen.sephiria_fantasy.shop.sold_out");
			drawTooltipLine(graphics, line.getString(), -1, x, y, TEXT_COLOR, SOLD_OUT_COLOR);
			return;
		}

		int price = ShopPrices.discounted(ShopPrices.buy(stack), ClientStats.shopDiscount());
		Component line = Component.translatable("screen.sephiria_fantasy.shop.buy_hint",
				Component.literal(String.valueOf(price)));
		// 价格那一段单独上黄色：1.20.1 的 drawString 不认富文本样式，按子串位置拆开画
		drawTooltipLine(graphics, line.getString(), line.getString().indexOf(String.valueOf(price)),
				x, y, TEXT_COLOR, PRICE_OK);
	}

	/** 画一行浮窗：{@code highlight} 是从第几个字符开始的价格（-1 = 整行同色）。 */
	private void drawTooltipLine(GuiGraphics graphics, String text, int highlight, int x, int y,
			int baseColour, int highlightColour) {
		int width = this.font.width(text);
		graphics.fill(x - 3, y - 3, x + width + 3, y + 9, 0xF0100010);

		if (highlight < 0) {
			graphics.drawString(this.font, text, x, y, highlightColour, false);
			return;
		}

		String prefix = text.substring(0, highlight);
		String digits = text.substring(highlight, highlight + digitCount(text, highlight));
		String suffix = text.substring(highlight + digits.length());
		graphics.drawString(this.font, prefix, x, y, baseColour, false);
		int cursor = x + this.font.width(prefix);
		graphics.drawString(this.font, digits, cursor, y, highlightColour, false);
		cursor += this.font.width(digits);
		graphics.drawString(this.font, suffix, cursor, y, baseColour, false);
	}

	private static int digitCount(String text, int start) {
		int end = start;

		while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-')) {
			end++;
		}

		return end - start;
	}
}
