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

/** 许愿池界面：当前池切换、单抽/十连、溢出开关。 */
public class WishPoolScreen extends SephiriaMenuScreen<WishPoolMenu> {
	private boolean slatePool;

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
				button -> send(WishPoolClickPayload.draw(this.slatePool, false)))
				.bounds(this.leftPos + 82, this.topPos + 170, 84, 18).build());
		this.addRenderableWidget(Button.builder(
				Component.translatable("screen.sephiria_fantasy.wish_pool.draw_ten"),
				button -> send(WishPoolClickPayload.draw(this.slatePool, true)))
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
