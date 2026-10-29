package com.sephiria.client.wishpool;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.WishPoolClickPayload;
import com.sephiria.wishpool.WishPoolMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 许愿池界面：当前池切换、单抽/十连、溢出开关。
 *
 * <p>抽卡时播一小段纯客户端的翻牌动画：点「单抽 / 十连」后盖一层卡背，卡背从下往上填满，
 * 填满后显示「抽到了」。这一版没有服务端回传的抽卡结果（{@code WishPoolResultPayload} 还没移植），
 * 所以动画只是装饰、不显示任何具体奖励；20 tick 后自动消失，点一下也能立刻跳过。
 */
public class WishPoolScreen extends SephiriaMenuScreen<WishPoolMenu> {
	/** 翻牌动画持续多少 tick（20 tick = 1 秒）。 */
	private static final int FLIP_TICKS = 20;
	/** 卡背填充占前多少 tick，剩下的是「抽到了」的展示时间。 */
	private static final int FLIP_FILL_TICKS = 13;
	/** 卡背尺寸与十连时并排的张数。 */
	private static final int CARD_WIDTH = 56;
	private static final int CARD_HEIGHT = 72;
	private static final int CARD_GAP = 6;
	private static final int CARD_COUNT_TEN = 3;
	/** 卡背配色：深底 + 亮边框 + 填充色 + 中央菱形。 */
	private static final int CARD_BACK = 0xFF1B1630;
	private static final int CARD_EDGE = 0xFF7A5CD6;
	private static final int CARD_FILL = 0xFF3A2E5A;
	private static final int CARD_EMBLEM = 0xFF9E86E8;
	/** 盖住整个面板的暗色。 */
	private static final int OVERLAY_DIM = 0xB0100010;

	private boolean slatePool;
	/** 翻牌动画还剩多少 tick（0 = 不显示）。 */
	private int flipTicks;
	/** 这次动画是十连吗（决定并排画几张卡背）。 */
	private boolean flipTen;

	public WishPoolScreen(WishPoolMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WishPoolMenu.IMAGE_WIDTH, WishPoolMenu.IMAGE_HEIGHT, false);
	}

	@Override
	protected void init() {
		super.init();

		this.addRenderableWidget(Button.builder(poolLabel(), button -> {
			this.slatePool = !this.slatePool;
			button.setMessage(poolLabel());
		}).bounds(this.leftPos + 8, this.topPos + 24, 64, 24).build());

		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.wish_pool.draw_single"),
				button -> {
					send(WishPoolClickPayload.draw(this.slatePool, false));
					startFlip(false);
				})
				.bounds(this.leftPos + 82, this.topPos + 170, 84, 18).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.wish_pool.draw_ten"),
				button -> {
					send(WishPoolClickPayload.draw(this.slatePool, true));
					startFlip(true);
				})
				.bounds(this.leftPos + 82, this.topPos + 192, 84, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("锚定主手"),
				button -> send(WishPoolClickPayload.anchor(this.slatePool, mainHandItemId())))
				.bounds(this.leftPos + 8, this.topPos + 52, 64, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("清除锚定"),
				button -> send(WishPoolClickPayload.clearAnchors(this.slatePool)))
				.bounds(this.leftPos + 8, this.topPos + 72, 64, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("神铸主手"),
				button -> send(WishPoolClickPayload.forge(mainHandItemId())))
				.bounds(this.leftPos + 8, this.topPos + 170, 64, 18).build());

		this.addRenderableWidget(Button.builder(overflowLabel(),
				button -> {
					send(WishPoolClickPayload.toggleOverflow());
					button.setMessage(overflowLabel());
				}).bounds(this.leftPos + 8, this.topPos + 192, 64, 18).build());
	}

	/** 翻牌动画按 tick 走：20 tick 后自动消失。 */
	@Override
	protected void containerTick() {
		super.containerTick();

		if (this.flipTicks > 0) {
			this.flipTicks--;
		}
	}

	/** 面板画完后盖上翻牌层：它压住按钮，点击由 mouseClicked 吃掉，不会误触下面的按钮。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		if (this.flipTicks > 0) {
			renderFlipOverlay(graphics);
		}
	}

	/** 动画中点一下立刻跳过（主要给等不及 / 想连抽的玩家）。 */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.flipTicks > 0) {
			this.flipTicks = 0;
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** 开始（或重启动画）：再点一次抽卡会重新从 0 开始填。 */
	private void startFlip(boolean ten) {
		this.flipTicks = FLIP_TICKS;
		this.flipTen = ten;
		ClientWishPool.clear();
	}

	private void renderFlipOverlay(GuiGraphics graphics) {
		int progress = FLIP_TICKS - this.flipTicks;
		boolean revealed = progress >= FLIP_FILL_TICKS;
		int cards = this.flipTen ? CARD_COUNT_TEN : 1;
		int totalWidth = cards * CARD_WIDTH + (cards - 1) * CARD_GAP;
		int originX = this.leftPos + (this.imageWidth - totalWidth) / 2;
		int originY = this.topPos + (this.imageHeight - CARD_HEIGHT) / 2;
		int fill = CARD_HEIGHT * Math.min(progress, FLIP_FILL_TICKS) / FLIP_FILL_TICKS;

		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
				this.topPos + this.imageHeight, OVERLAY_DIM);

		for (int index = 0; index < cards; index++) {
			int x = originX + index * (CARD_WIDTH + CARD_GAP);
			// 卡背：外描边 + 深底 + 从下往上填满的亮色
			graphics.fill(x - 1, originY - 1, x + CARD_WIDTH + 1, originY + CARD_HEIGHT + 1, CARD_EDGE);
			graphics.fill(x, originY, x + CARD_WIDTH, originY + CARD_HEIGHT, CARD_BACK);
			graphics.fill(x, originY + CARD_HEIGHT - fill, x + CARD_WIDTH,
					originY + CARD_HEIGHT, CARD_FILL);

			if (revealed) {
				java.util.List<net.minecraft.world.item.ItemStack> items = ClientWishPool.items();

				if (index < items.size() && !items.get(index).isEmpty()) {
					graphics.renderItem(items.get(index), x + CARD_WIDTH / 2 - 8,
							originY + CARD_HEIGHT / 2 - 8);
				} else {
					drawDiamond(graphics, x + CARD_WIDTH / 2, originY + CARD_HEIGHT / 2, 12, CARD_EMBLEM);
				}
			}
		}

		if (revealed) {
			Component title = Component.translatable("screen.sephiria_fantasy.wish_pool.flip");
			graphics.drawString(this.font, title,
					this.leftPos + (this.imageWidth - this.font.width(title)) / 2,
					originY - 14, 0xFFFFFFFF, true);
		}

		// 提示一直显示：动画 20 tick 后自动结束，但点一下能立刻跳过
		Component skip = Component.translatable("screen.sephiria_fantasy.wish_pool.drawing_skip");
		graphics.drawString(this.font, skip,
				this.leftPos + (this.imageWidth - this.font.width(skip)) / 2,
				originY + CARD_HEIGHT + 8, HINT, false);
	}

	/** 卡背中央的菱形花纹：一行行横条拼出来，免得为一张装饰图再添贴图。 */
	private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY, int radius, int colour) {
		for (int offset = -radius; offset <= radius; offset++) {
			int half = radius - Math.abs(offset);
			graphics.fill(centerX - half, centerY + offset, centerX + half + 1, centerY + offset + 1, colour);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		graphics.drawString(this.font,
				Component.translatable("screen.sephiria_fantasy.wish_pool.energy", this.menu.energy()),
				82, 12, 0xFFD0D0D0, false);
	}

	private Component poolLabel() {
		return Component.translatable(this.slatePool
				? "screen.sephiria_fantasy.wish_pool.pool_slate"
				: "screen.sephiria_fantasy.wish_pool.pool_artifact");
	}

	private Component overflowLabel() {
		return Component.translatable("screen.sephiria_fantasy.wish_pool.overflow_toggle",
				this.menu.overflowToClover()
						? Component.translatable("screen.sephiria_fantasy.wish_pool.overflow_on")
						: Component.translatable("screen.sephiria_fantasy.wish_pool.overflow_off"));
	}

	private static int mainHandItemId() {
		net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();

		if (client.player == null) {
			return 0;
		}

		return net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(
				client.player.getMainHandItem().getItem());
	}

	private static void send(WishPoolClickPayload payload) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		WishPoolClickPayload.write(buffer, payload);
		ClientPlayNetworking.send(WishPoolClickPayload.CHANNEL, buffer);
	}
}
