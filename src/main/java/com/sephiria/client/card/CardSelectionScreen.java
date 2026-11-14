package com.sephiria.client.card;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.card.CardSelection;
import com.sephiria.client.artifact.ArtifactTooltip;
import com.sephiria.network.CardRefreshPayload;
import com.sephiria.network.CardSelectPayload;
import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 选卡界面（1.20.1 版）：三张卡并排，各带一个「刷新」按钮；点卡面就是选择。
 *
 * <p>数据全部来自服务端；这里只发「刷新第几张 / 选第几张」。
 * 渲染走 1.20.1 的 {@link GuiGraphics}，动画逻辑与 26.1.2 版一致。
 */
public class CardSelectionScreen extends Screen {
	private static final int PANEL_WIDTH = 280;
	private static final int PANEL_HEIGHT = 230;
	private static final int CARD_WIDTH = 72;
	private static final int CARD_HEIGHT = 104;
	private static final int CARD_GAP = 12;
	private static final int FILL_TICKS = 12;
	private static final int FLIP_TICKS = 20;
	private static final int STAGGER = 2;
	private static final int ICON_SIZE = 32;

	private static final int OVERLAY_DIM = 0xC0100618;
	private static final int PANEL_EDGE = 0xFF7A5CD6;
	private static final int PANEL_TOP = 0xF01B1430;
	private static final int PANEL_BOTTOM = 0xF00E0918;
	private static final int CARD_BACK = 0xFF1B1630;
	private static final int CARD_EMBLEM = 0xFF9E86E8;
	private static final int TEXT_COLOR = 0xFFE8E0FF;
	private static final int DIM_COLOR = 0xFFB0A0D0;

	private final List<ItemStack> cards = new ArrayList<>(CardSelection.CARDS);
	private final boolean[] refreshUsed = new boolean[CardSelection.CARDS];
	private final int[] reveal = new int[CardSelection.CARDS];
	private final Button[] refreshButtons = new Button[CardSelection.CARDS];
	private int chances;
	private boolean selecting;
	private int pendingRefresh = -1;
	private int panelLeft;
	private int panelTop;

	public CardSelectionScreen() {
		super(Component.translatable("screen.sephiria_fantasy.card"));

		for (int index = 0; index < CardSelection.CARDS; index++) {
			this.cards.add(ItemStack.EMPTY);
		}
	}

	@Override
	protected void init() {
		this.panelLeft = (this.width - PANEL_WIDTH) / 2;
		this.panelTop = (this.height - PANEL_HEIGHT) / 2;
		com.sephiria.client.tabs.ScreenTabs.add(this,
				this.panelLeft + (PANEL_WIDTH - com.sephiria.client.tabs.ScreenTabs.width()) / 2,
				this.panelTop - 24, com.sephiria.client.tabs.SephiriaTab.CARD);

		for (int index = 0; index < CardSelection.CARDS; index++) {
			int slot = index;
			this.refreshButtons[index] = addRenderableWidget(Button.builder(
							Component.translatable("screen.sephiria_fantasy.card.refresh"),
							button -> requestRefresh(slot))
					.bounds(refreshX(index), refreshY(), CARD_WIDTH, 18)
					.build());
		}

		syncFromCache(true);
	}

	@Override
	public void tick() {
		super.tick();

		for (int index = 0; index < CardSelection.CARDS; index++) {
			if (this.reveal[index] < FLIP_TICKS) {
				this.reveal[index]++;
			}

			Button button = this.refreshButtons[index];

			if (button != null) {
				button.visible = this.chances > 0;
				button.active = !this.selecting && !this.refreshUsed[index];
			}
		}
	}

	public void syncFromCache(boolean initial) {
		List<ItemStack> source = ClientCardSelection.cards();
		int forced = this.pendingRefresh;
		this.pendingRefresh = -1;
		this.chances = ClientCardSelection.chances();
		this.selecting = false;

		for (int index = 0; index < CardSelection.CARDS; index++) {
			ItemStack next = index < source.size() ? source.get(index) : ItemStack.EMPTY;
			boolean changed = !same(this.cards.get(index), next);

			if (initial || changed || index == forced) {
				this.cards.set(index, next.copy());
				this.reveal[index] = initial ? -index * STAGGER : 0;
			}

			this.refreshUsed[index] = ClientCardSelection.refreshed(index);
		}
	}

	private void requestRefresh(int index) {
		if (this.selecting || index < 0 || index >= CardSelection.CARDS || this.refreshUsed[index]) {
			return;
		}

		this.refreshUsed[index] = true;
		this.pendingRefresh = index;
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		CardRefreshPayload.write(buffer, new CardRefreshPayload(index));
		ClientPlayNetworking.send(CardRefreshPayload.CHANNEL, buffer);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		drawPanel(graphics);
		drawCards(graphics, mouseX, mouseY);
		drawHeaderAndHint(graphics);
		showCardTooltip(graphics, mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void drawPanel(GuiGraphics graphics) {
		graphics.fill(0, 0, this.width, this.height, OVERLAY_DIM);
		graphics.fill(this.panelLeft - 2, this.panelTop - 2,
				this.panelLeft + PANEL_WIDTH + 2, this.panelTop + PANEL_HEIGHT + 2, PANEL_EDGE);
		graphics.fillGradient(this.panelLeft, this.panelTop,
				this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, PANEL_TOP, PANEL_BOTTOM);
	}

	private void drawHeaderAndHint(GuiGraphics graphics) {
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelTop + 8, TEXT_COLOR);

		int colour = this.chances > 0 ? 0xFFFFD24A : 0xFFFF6B6B;
		Component chancesText = Component.translatable("screen.sephiria_fantasy.card.chances",
				Component.literal(String.valueOf(this.chances)).withStyle(style -> style.withColor(colour)));
		graphics.drawCenteredString(this.font, chancesText, this.width / 2, this.panelTop + 22, DIM_COLOR);

		int barWidth = 160;
		int barX = this.width / 2 - barWidth / 2;
		int barY = this.panelTop + PANEL_HEIGHT - 20;
		float progress = ClientCardSelection.chargeProgress();
		graphics.drawCenteredString(this.font,
				Component.translatable("screen.sephiria_fantasy.card.charge"),
				this.width / 2, barY - 10, DIM_COLOR);
		graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF1A1428);
		graphics.fill(barX, barY, barX + Math.round(barWidth * progress), barY + 6, 0xFF9E86E8);

		if (this.chances > 0) {
			graphics.drawCenteredString(this.font, Component.translatable("screen.sephiria_fantasy.card.hint"),
					this.width / 2, this.panelTop + PANEL_HEIGHT - 50, DIM_COLOR);
		} else {
			graphics.drawCenteredString(this.font,
					Component.translatable("screen.sephiria_fantasy.card.no_chance"),
					this.width / 2, cardsTop() + CARD_HEIGHT / 2, DIM_COLOR);
		}

		if (this.selecting) {
			graphics.drawCenteredString(this.font,
					Component.translatable("screen.sephiria_fantasy.card.selecting"),
					this.width / 2, this.panelTop + PANEL_HEIGHT - 62, 0xFF9E86E8);
		}
	}

	private void drawCards(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.chances <= 0) {
			return;
		}

		for (int index = 0; index < CardSelection.CARDS; index++) {
			int x = cardX(index);
			int y = cardsTop();
			int progress = Math.max(0, this.reveal[index]);
			boolean hovered = insideCard(index, mouseX, mouseY);
			boolean face = progress >= FILL_TICKS;

			drawCardBack(graphics, x, y, index, progress);

			if (face && !this.cards.get(index).isEmpty()) {
				float flip = Math.min(1.0F, (progress - FILL_TICKS) / (float) (FLIP_TICKS - FILL_TICKS));
				flip = 1.0F - (1.0F - flip) * (1.0F - flip);
				drawCardFace(graphics, x, y, index, hovered, Math.max(0.02F, flip));
			}

			if (hovered && face && this.chances > 0 && !this.selecting) {
				graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x18FFFFFF);
			}
		}
	}

	private void drawCardBack(GuiGraphics graphics, int x, int y, int index, int progress) {
		int filled = CARD_HEIGHT * Math.min(progress, FILL_TICKS) / FILL_TICKS;
		ItemStack card = this.cards.get(index);
		int rarityColor = card.isEmpty() ? 0xFFFFFFFF
				: 0xFF000000 | (ArtifactLoot.rarityOf(card.getItem()).color() & 0xFFFFFF);
		int borderColor = 0xFF000000 | (rarityColor & 0xFFFFFF);
		int fill = 0xFF000000 | (dim(rarityColor, 0.45F) & 0xFFFFFF);
		graphics.fill(x - 1, y - 1, x + CARD_WIDTH + 1, y + CARD_HEIGHT + 1, borderColor);
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, CARD_BACK);

		if (filled > 0) {
			graphics.fillGradient(x, y + CARD_HEIGHT - filled, x + CARD_WIDTH, y + CARD_HEIGHT,
					dim(rarityColor, 0.25F), fill);
		}

		if (progress < FILL_TICKS && filled > 0) {
			int edge = y + CARD_HEIGHT - filled;
			int bandTop = Math.max(y, edge - 2);

			if (bandTop < edge) {
				graphics.fillGradient(x, bandTop, x + CARD_WIDTH, edge, 0x00E8D8FF, 0x90E8D8FF);
			}
		}

		if (progress < FILL_TICKS) {
			graphics.drawCenteredString(this.font, Component.literal("?"), x + CARD_WIDTH / 2,
					y + CARD_HEIGHT / 2 - 4, CARD_EMBLEM);
		}
	}

	private static int dim(int color, float factor) {
		int r = (int) (((color >> 16) & 0xFF) * factor);
		int g = (int) (((color >> 8) & 0xFF) * factor);
		int b = (int) ((color & 0xFF) * factor);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private void drawCardFace(GuiGraphics graphics, int x, int y, int index, boolean hovered, float flipScale) {
		ItemStack stack = this.cards.get(index);
		ArtifactRarity rarity = ArtifactLoot.rarityOf(stack.getItem());
		int edge = 0xFF000000 | (rarity.color() & 0xFFFFFF);
		int centreX = x + CARD_WIDTH / 2;

		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centreX, 0.0F, 0.0F);
		pose.scale(flipScale, 1.0F, 1.0F);
		pose.translate(-centreX, 0.0F, 0.0F);

		graphics.fill(x - 1, y - 1, x + CARD_WIDTH + 1, y + CARD_HEIGHT + 1,
				hovered ? 0xFFFFFFFF : edge);
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF151022);
		graphics.fill(x, y, x + CARD_WIDTH, y + 2, edge);
		graphics.drawCenteredString(this.font, Component.translatable(rarity.translationKey()),
				centreX, y + 6, edge);
		drawIcon(graphics, stack.getItem(), x + (CARD_WIDTH - ICON_SIZE) / 2, y + 24, ICON_SIZE);
		drawFittedText(graphics, stack.getHoverName(), centreX, y + CARD_HEIGHT - 24, CARD_WIDTH - 8, TEXT_COLOR);

		if (this.refreshUsed[index]) {
			graphics.drawCenteredString(this.font,
					Component.translatable("screen.sephiria_fantasy.card.refreshed"),
					centreX, y + CARD_HEIGHT - 11, 0xFF8C7CB8);
		}

		pose.popPose();

		if (flipScale < 0.16F) {
			graphics.fill(centreX - 1, y, centreX + 1, y + CARD_HEIGHT, 0xFFFFFFFF);
		}
	}

	private void drawFittedText(GuiGraphics graphics, Component text, int centreX, int y,
			int maxWidth, int colour) {
		int width = this.font.width(text);
		float scale = width <= maxWidth ? 1.0F : maxWidth / (float) Math.max(1, width);
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centreX, y, 0.0F);
		pose.scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, -width / 2, 0, colour, false);
		pose.popPose();
	}

	private static void drawIcon(GuiGraphics graphics, Item item, int x, int y, int size) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
		ResourceLocation texture = new ResourceLocation(id.getNamespace(),
				"textures/item/" + id.getPath() + ".png");
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x, y, 0.0F);
		pose.scale(size / 16.0F, size / 16.0F, 1.0F);
		graphics.blit(texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
		pose.popPose();
	}

	private void showCardTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.chances <= 0 || this.selecting) {
			return;
		}

		int index = cardAt(mouseX, mouseY);

		if (index < 0 || this.reveal[index] < FLIP_TICKS) {
			return;
		}

		ItemStack stack = this.cards.get(index);

		if (stack.isEmpty()) {
			return;
		}

		ArtifactTooltip.setSlotLevelHint(0);

		try {
			List<Component> lines = new ArrayList<>();
			lines.add(stack.getHoverName());
			List<Component> artifactLines = ArtifactTooltip.lines(stack);

			if (!artifactLines.isEmpty()) {
				lines.add(Component.empty());
				lines.addAll(artifactLines);
			}

			graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
		} finally {
			ArtifactTooltip.clearSlotLevelHint();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}

		int index = cardAt(mouseX, mouseY);

		if (index < 0) {
			return false;
		}

		if (this.reveal[index] < FLIP_TICKS) {
			this.reveal[index] = FLIP_TICKS;
			return true;
		}

		if (this.selecting || this.chances <= 0 || this.cards.get(index).isEmpty()) {
			return true;
		}

		this.selecting = true;
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		CardSelectPayload.write(buffer, new CardSelectPayload(index));
		ClientPlayNetworking.send(CardSelectPayload.CHANNEL, buffer);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		ClientCardSelection.suppressAutoOpen();
		super.onClose();
	}

	private int cardAt(double mouseX, double mouseY) {
		for (int index = 0; index < CardSelection.CARDS; index++) {
			if (insideCard(index, mouseX, mouseY)) {
				return index;
			}
		}

		return -1;
	}

	private boolean insideCard(int index, double mouseX, double mouseY) {
		int x = cardX(index);
		int y = cardsTop();
		return mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT;
	}

	private int cardsTop() {
		return this.panelTop + 44;
	}

	private int cardX(int index) {
		int total = CARD_WIDTH * CardSelection.CARDS + CARD_GAP * (CardSelection.CARDS - 1);
		return this.panelLeft + (PANEL_WIDTH - total) / 2 + index * (CARD_WIDTH + CARD_GAP);
	}

	private int refreshY() {
		return cardsTop() + CARD_HEIGHT + 6;
	}

	private int refreshX(int index) {
		return cardX(index);
	}

	private static boolean same(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty()) {
			return left.isEmpty() && right.isEmpty();
		}

		return ItemStack.isSameItemSameTags(left, right);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
