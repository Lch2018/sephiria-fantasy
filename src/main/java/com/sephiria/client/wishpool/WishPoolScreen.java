package com.sephiria.client.wishpool;

import com.sephiria.client.menu.SephiriaMenuScreen;
import com.sephiria.network.WishPoolClickPayload;
import com.sephiria.wishpool.WishPoolMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 许愿池界面：当前池切换、单抽/十连、溢出开关。 */
public class WishPoolScreen extends SephiriaMenuScreen<WishPoolMenu> {
	private static final int FLIP_TICKS = 20;
	private static final int FLIP_FILL_TICKS = 13;
	private boolean slatePool;
	private int flipTicks;
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
					ClientWishPool.clear();
					this.flipTicks = FLIP_TICKS;
					this.flipTen = false;
					send(WishPoolClickPayload.draw(this.slatePool, false));
				})
				.bounds(this.leftPos + 82, this.topPos + 170, 84, 18).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.wish_pool.draw_ten"),
				button -> {
					ClientWishPool.clear();
					this.flipTicks = FLIP_TICKS;
					this.flipTen = true;
					send(WishPoolClickPayload.draw(this.slatePool, true));
				})
				.bounds(this.leftPos + 82, this.topPos + 192, 84, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("锚定主手"),
				button -> send(WishPoolClickPayload.anchor(this.slatePool, mainHandItemId())))
				.bounds(this.leftPos + 8, this.topPos + 52, 64, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("清除锚定"),
				button -> send(new WishPoolClickPayload(WishPoolClickPayload.ACTION_ANCHOR, this.slatePool, 0)))
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

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		if (this.flipTicks <= 0) {
			return;
		}

		this.flipTicks--;
		int progress = FLIP_TICKS - this.flipTicks;
		boolean revealed = progress >= FLIP_FILL_TICKS;
		int cards = this.flipTen ? 3 : 1;
		int cardWidth = 56;
		int cardHeight = 72;
		int gap = 6;
		int total = cards * cardWidth + (cards - 1) * gap;
		int originX = this.leftPos + (this.imageWidth - total) / 2;
		int originY = this.topPos + (this.imageHeight - cardHeight) / 2;
		int fill = cardHeight * Math.min(progress, FLIP_FILL_TICKS) / FLIP_FILL_TICKS;

		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
				this.topPos + this.imageHeight, 0xB0100010);

		for (int index = 0; index < cards; index++) {
			int x = originX + index * (cardWidth + gap);
			graphics.fill(x - 1, originY - 1, x + cardWidth + 1, originY + cardHeight + 1, 0xFF7A5CD6);
			graphics.fill(x, originY, x + cardWidth, originY + cardHeight, 0xFF1B1630);
			graphics.fill(x, originY + cardHeight - fill, x + cardWidth, originY + cardHeight, 0xFF3A2E5A);

			if (revealed) {
				java.util.List<net.minecraft.world.item.ItemStack> items = ClientWishPool.items();

				if (index < items.size() && !items.get(index).isEmpty()) {
					graphics.renderItem(items.get(index), x + cardWidth / 2 - 8, originY + cardHeight / 2 - 8);
				} else {
					graphics.fill(x + cardWidth / 2 - 8, originY + cardHeight / 2 - 8,
							x + cardWidth / 2 + 8, originY + cardHeight / 2 + 8, 0xFF9E86E8);
				}
			}
		}

		if (revealed) {
			Component title = Component.translatable("screen.sephiria_fantasy.wish_pool.flip");
			graphics.drawString(this.font, title, this.leftPos + (this.imageWidth - this.font.width(title)) / 2,
					originY - 14, 0xFFFFFFFF, true);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.flipTicks > 0) {
			this.flipTicks = 0;
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
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
		ClientPlayNetworking.send(payload);
	}
}
