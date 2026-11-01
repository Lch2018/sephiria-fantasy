package com.sephiria.client.card;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.card.CardSelection;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.CardRefreshPayload;
import com.sephiria.network.CardSelectPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 选卡界面：三张卡并排，各带一个「刷新」按钮；点卡面就是选择。
 *
 * <p>动画照 1.21.1 / 1.20.1 许愿池的翻牌套路（26.3 用 {@link GuiGraphicsExtractor} 重写）：
 * 卡背从下往上填满，填充前沿带一条流光，填满后从中间向两侧翻出卡面。
 *
 * <p>所有数据（卡面、刷新状态、剩余次数）都来自服务端；这里只发「刷新第几张 / 选第几张」，
 * 点击过快或客户端状态过期时服务端会把权威状态重发回来，界面按它对齐。
 */
public class CardSelectionScreen extends Screen {
	/** 面板尺寸（比原版箱子窄一点，刚好放三张卡 + 标签栏）。 */
	private static final int PANEL_WIDTH = 280;
	private static final int PANEL_HEIGHT = 230;
	/** 卡面尺寸与间距。 */
	private static final int CARD_WIDTH = 72;
	private static final int CARD_HEIGHT = 104;
	private static final int CARD_GAP = 12;
	/** 卡背填充占前 12 tick，剩下 8 tick 用来「翻」出卡面。 */
	private static final int FILL_TICKS = 12;
	private static final int FLIP_TICKS = 20;
	/** 三张卡依次晚 2 tick 开始，看起来有节奏。 */
	private static final int STAGGER = 2;
	private static final int ICON_SIZE = 32;

	private static final int OVERLAY_DIM = 0xC0100618;
	private static final int PANEL_EDGE = 0xFF7A5CD6;
	private static final int PANEL_TOP = 0xF01B1430;
	private static final int PANEL_BOTTOM = 0xF00E0918;
	private static final int CARD_BACK = 0xFF1B1630;
	private static final int CARD_EDGE = 0xFF7A5CD6;
	private static final int CARD_FILL = 0xFF3A2E5A;
	private static final int CARD_EMBLEM = 0xFF9E86E8;
	private static final int TEXT_COLOR = 0xFFE8E0FF;
	private static final int DIM_COLOR = 0xFFB0A0D0;

	/** 三张卡的本地副本（服务端包一到就整份换掉）。 */
	private final List<ItemStack> cards = new ArrayList<>(CardSelection.CARDS);
	private final boolean[] refreshUsed = new boolean[CardSelection.CARDS];
	/** 每张卡自己的翻牌进度：负数 = 还没开始（错开启动）。 */
	private final int[] reveal = new int[CardSelection.CARDS];
	private final Button[] refreshButtons = new Button[CardSelection.CARDS];
	private int chances;
	/** 已经发了选择请求，等服务端的关闭包——期间别再点。 */
	private boolean selecting;
	/** 刚点了刷新的是哪一张（服务端回包后强制重播这张的翻牌）。 */
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
		SephiriaTabs.add(this, this.panelLeft + (PANEL_WIDTH - SephiriaTabs.width()) / 2,
				this.panelTop - 24, SephiriaTab.CARD);

		for (int index = 0; index < CardSelection.CARDS; index++) {
			int slot = index;
			this.refreshButtons[index] = addRenderableWidget(Button.builder(
							Component.translatable("screen.sephiria_fantasy.card.refresh"),
							button -> requestRefresh(slot))
					.bounds(refreshX(index), refreshY(), CARD_WIDTH, 18)
					.build());
		}

		// 界面每次重建都按缓存重来一次：打开时缓存里已经是服务端刚掷好的三张
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

	/** 服务端推来新状态：变化的卡重播翻牌，其余保持原样。 */
	public void syncFromCache(boolean initial) {
		List<ItemStack> source = ClientCardSelection.cards();
		int forced = this.pendingRefresh;
		this.pendingRefresh = -1;
		this.chances = ClientCardSelection.chances();
		this.selecting = false;

		for (int index = 0; index < CardSelection.CARDS; index++) {
			ItemStack next = index < source.size() ? source.get(index) : ItemStack.EMPTY;
			boolean changed = !same(this.cards.get(index), next);

			// initial = 第一次打开（三张一起翻）；forced = 刚刷新的那一张，哪怕抽到同一件也重播
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

		// 先本地置灰，避免一次点击发两遍；服务端包回来时会以它为准覆盖
		this.refreshUsed[index] = true;
		this.pendingRefresh = index;
		ClientPlayNetworking.send(new CardRefreshPayload(index));
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (net.minecraft.client.Minecraft.getInstance().options.keyInventory.matches(event)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 先画底板与卡面，super 里的按钮 / 标签再盖在上面（26.3 的调用顺序就是图层顺序）
		drawPanel(extractor);
		drawCards(extractor, mouseX, mouseY);
		drawHeaderAndHint(extractor);
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	/** 整屏暗底 + 面板。 */
	private void drawPanel(GuiGraphicsExtractor extractor) {
		extractor.fill(0, 0, this.width, this.height, OVERLAY_DIM);
		extractor.fill(this.panelLeft - 2, this.panelTop - 2,
				this.panelLeft + PANEL_WIDTH + 2, this.panelTop + PANEL_HEIGHT + 2, PANEL_EDGE);
		extractor.fillGradient(this.panelLeft, this.panelTop,
				this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, PANEL_TOP, PANEL_BOTTOM);
	}

	private void drawHeaderAndHint(GuiGraphicsExtractor extractor) {
		extractor.centeredText(this.font, this.title, this.width / 2, this.panelTop + 8, TEXT_COLOR);

		int colour = this.chances > 0 ? 0xFFFFD24A : 0xFFFF6B6B;
		Component chancesText = Component.translatable("screen.sephiria_fantasy.card.chances",
				Component.literal(String.valueOf(this.chances)).withColor(colour));
		extractor.centeredText(this.font, chancesText, this.width / 2, this.panelTop + 22, DIM_COLOR);

		// 充能进度：距离下一次选卡机会的时间比例
		int barWidth = 160;
		int barX = this.width / 2 - barWidth / 2;
		int barY = this.panelTop + PANEL_HEIGHT - 20;
		float progress = ClientCardSelection.chargeProgress();
		extractor.centeredText(this.font,
				Component.translatable("screen.sephiria_fantasy.card.charge"),
				this.width / 2, barY - 10, DIM_COLOR);
		extractor.fill(barX, barY, barX + barWidth, barY + 6, 0xFF1A1428);
		extractor.fill(barX, barY, barX + Math.round(barWidth * progress), barY + 6, 0xFF9E86E8);

		if (this.chances > 0) {
			Component hint = Component.translatable("screen.sephiria_fantasy.card.hint");
			extractor.centeredText(this.font, hint, this.width / 2, this.panelTop + PANEL_HEIGHT - 50,
					DIM_COLOR);
		}

		if (this.selecting) {
			extractor.centeredText(this.font,
					Component.translatable("screen.sephiria_fantasy.card.selecting"),
					this.width / 2, this.panelTop + PANEL_HEIGHT - 62, 0xFF9E86E8);
		}
	}

	private void drawCards(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		if (this.chances <= 0) {
			return;
		}

		for (int index = 0; index < CardSelection.CARDS; index++) {
			int x = cardX(index);
			int y = cardsTop();
			int progress = Math.max(0, this.reveal[index]);
			boolean hovered = insideCard(index, mouseX, mouseY);
			boolean face = progress >= FILL_TICKS;

			// 卡背一直垫在下面：翻面的前半段还能看到它
			drawCardBack(extractor, x, y, index, progress);

			// 空卡（池子异常时才会出现）不画卡面，免得去找 air 的贴图
			if (face && !this.cards.get(index).isEmpty()) {
				float flip = Math.min(1.0F,
						(progress - FILL_TICKS) / (float) (FLIP_TICKS - FILL_TICKS));
				// 缓出：一开始翻得快、贴到 1 时慢下来
				flip = 1.0F - (1.0F - flip) * (1.0F - flip);
				drawCardFace(extractor, x, y, index, hovered, Math.max(0.02F, flip));
			}

			if (hovered && face && this.chances > 0 && !this.selecting) {
				extractor.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x18FFFFFF);
			}
		}
	}

	/** 卡背：外框 + 深底 + 从下往上填的亮色 + 填充前沿的流光。 */
	private void drawCardBack(GuiGraphicsExtractor extractor, int x, int y, int index, int progress) {
		int filled = CARD_HEIGHT * Math.min(progress, FILL_TICKS) / FILL_TICKS;
		net.minecraft.world.item.ItemStack card = this.cards.get(index);
		int rarityColor = card.isEmpty() ? 0xFFFFFFFF
				: 0xFF000000 | (ArtifactLoot.rarityOf(card.getItem()).color() & 0xFFFFFF);
		int borderColor = 0xFF000000 | (rarityColor & 0xFFFFFF);
		int fill = 0xFF000000 | (dim(rarityColor, 0.45F) & 0xFFFFFF);
		extractor.fill(x - 1, y - 1, x + CARD_WIDTH + 1, y + CARD_HEIGHT + 1, borderColor);
		extractor.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, CARD_BACK);

		if (filled > 0) {
			extractor.fillGradient(x, y + CARD_HEIGHT - filled, x + CARD_WIDTH, y + CARD_HEIGHT,
					dim(rarityColor, 0.25F), fill);
		}

		if (progress < FILL_TICKS && filled > 0) {
			// 流光贴在填充前沿，随着卡背升高往上扫
			int edge = y + CARD_HEIGHT - filled;
			int bandTop = Math.max(y, edge - 2);

			if (bandTop < edge) {
				extractor.fillGradient(x, bandTop, x + CARD_WIDTH, edge, 0x00E8D8FF, 0x90E8D8FF);
			}
		}

		if (progress < FILL_TICKS) {
			extractor.centeredText(this.font, Component.literal("?"), x + CARD_WIDTH / 2,
					y + CARD_HEIGHT / 2 - 4, CARD_EMBLEM);
		}
	}

	private static int dim(int color, float factor) {
		int r = (int) (((color >> 16) & 0xFF) * factor);
		int g = (int) (((color >> 8) & 0xFF) * factor);
		int b = (int) ((color & 0xFF) * factor);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	/** 卡面：品质色外框 + 品质名 + 神器图标 + 名字（太长就缩放贴进卡宽）。 */
	private void drawCardFace(GuiGraphicsExtractor extractor, int x, int y, int index, boolean hovered,
			float flipScale) {
		ItemStack stack = this.cards.get(index);
		ArtifactRarity rarity = ArtifactLoot.rarityOf(stack.getItem());
		int edge = 0xFF000000 | (rarity.color() & 0xFFFFFF);
		int centreX = x + CARD_WIDTH / 2;

		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(centreX, 0.0F);
		pose.scale(flipScale, 1.0F);
		pose.translate(-centreX, 0.0F);

		extractor.fill(x - 1, y - 1, x + CARD_WIDTH + 1, y + CARD_HEIGHT + 1,
				hovered ? 0xFFFFFFFF : edge);
		extractor.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF151022);
		extractor.fill(x, y, x + CARD_WIDTH, y + 2, edge);
		extractor.centeredText(this.font, Component.translatable(rarity.translationKey()),
				centreX, y + 6, edge);
		drawIcon(extractor, stack.getItem(), x + (CARD_WIDTH - ICON_SIZE) / 2, y + 24, ICON_SIZE);
		drawFittedText(extractor, stack.getHoverName(), centreX, y + CARD_HEIGHT - 24,
				CARD_WIDTH - 8, TEXT_COLOR);

		// 刷新过的卡在左下角打一个记号，一眼能看出这张不能再刷
		if (this.refreshUsed[index]) {
			extractor.centeredText(this.font,
					Component.translatable("screen.sephiria_fantasy.card.refreshed"),
					centreX, y + CARD_HEIGHT - 11, 0xFF8C7CB8);
		}

		pose.popMatrix();

		if (flipScale < 0.16F) {
			// 翻面刚开始时中间是一道亮线，翻转感更明显
			extractor.fill(centreX - 1, y, centreX + 1, y + CARD_HEIGHT, 0xFFFFFFFF);
		}
	}

	private void drawFittedText(GuiGraphicsExtractor extractor, Component text, int centreX, int y,
			int maxWidth, int colour) {
		int width = this.font.width(text);
		float scale = width <= maxWidth ? 1.0F : maxWidth / (float) Math.max(1, width);
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(centreX, y);
		pose.scale(scale, scale);
		extractor.text(this.font, text, -width / 2, 0, colour, false);
		pose.popMatrix();
	}

	/** 画物品图标：只画本模组物品（贴图一定在 textures/item/&lt;id&gt;.png）。 */
	private static void drawIcon(GuiGraphicsExtractor extractor, Item item, int x, int y, int size) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		Identifier texture = Identifier.fromNamespaceAndPath(id.getNamespace(),
				"textures/item/" + id.getPath() + ".png");
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(size / 16.0F, size / 16.0F);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
		pose.popMatrix();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		// 26.3 的 button() 是 HID usage（左 1 / 中 2 / 右 3），不能用 0 当左键
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}

		int index = cardAt(event.x(), event.y());

		if (index < 0) {
			return false;
		}

		// 还在翻牌：先点一下直接看牌，第二下才是选择
		if (this.reveal[index] < FLIP_TICKS) {
			this.reveal[index] = FLIP_TICKS;
			return true;
		}

		if (this.selecting || this.chances <= 0 || this.cards.get(index).isEmpty()) {
			return true;
		}

		this.selecting = true;
		ClientPlayNetworking.send(new CardSelectPayload(index));
		return true;
	}

	@Override
	public void onClose() {
		// 关掉界面后，可能还有一个「刷新回包」在路上；别让它把刚关掉的界面又弹回来
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

	/** 刷新按钮与卡面同宽。 */
	private int refreshX(int index) {
		return cardX(index);
	}

	private static boolean same(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty()) {
			return left.isEmpty() && right.isEmpty();
		}

		return ItemStack.isSameItemSameComponents(left, right);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}
