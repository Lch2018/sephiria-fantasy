package com.sephiria.client.wishpool;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.network.WishPoolClickPayload;
import com.sephiria.wishpool.WishPool;
import com.sephiria.wishpool.WishPoolMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 许愿池界面：一个面板装四种视图（用户 2026-10-15 的口径）。
 *
 * <pre>
 *  卡池（POOL）                       锚定 / 神铸（ANCHOR / FORGE）      抽卡动画（DRAWING）
 *  ┌──────┬──────────────────────┐    ┌────────────────────────────┐   ┌──────────────────┐
 *  │神器池│      [锚定物品]      │    │ 锚定神器 / 神铸            │   │   [物品] [物品]  │
 *  │石板池│      [ 锚定 ]        │    │ [全部][普通][高级][稀有]…  │   │   一张张亮出来    │
 *  │      │                      │    │ [连击图标 ×7 ×2 行]        │   │   +x 能量         │
 *  │许愿池│  [单抽] [十连]       │    │ ┌────────────────────────┐ │   │                  │
 *  │      │           [ 神铸 ]   │    │ │  物品网格（7×4，分页） │ │   │                  │
 *  │      │  能量：12            │    │ └────────────────────────┘ │   │                  │
 *  └──────┴──────────────────────┘    │ [返回][取消锚定] [◀][▶]    │   └──────────────────┘
 * </pre>
 *
 * <p>要点：
 * <ul>
 *   <li>左侧两个卡池标签（神器池 / 石板池），右边是卡池内容；</li>
 *   <li>卡池中间是锚定物品图标与「锚定」按钮，下面「单抽」「十连」，右下角能量与它上面的「神铸」；</li>
 *   <li>锚定 / 神铸的选择页按<b>品质</b>（都是按钮）与<b>连击</b>（神器池才有，用连击图标当按钮）筛；</li>
 *   <li>神铸只列黄与红两档（用户口径「只能换取黄和红品质的神器或石板」，黄 80 / 红 200 能量）；</li>
 *   <li>抽卡动画一张张亮出来，播完自动回到卡池页（点一下可以立刻跳过）。</li>
 * </ul>
 *
 * <p>所有数值（能量、四叶草、锚定件）都读菜单同步过来的数据；物品图标一律<b>直接 blit 本模组物品的贴图</b>
 * （{@code textures/item/<id>.png}）——这个阶段不能画原版物品，会崩（见手册 §三）。
 */
public class WishPoolScreen extends AbstractContainerScreen<WishPoolMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int BOX_COLOR = 0x30000000;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int DIM_COLOR = 0xFF9A9AA8;
	private static final int HEADER_COLOR = 0xFFAAAAAA;
	private static final int ENERGY_COLOR = 0xFF7EFAFF;
	private static final int CLOVER_COLOR = 0xFF55FF55;
	private static final int TAB_ACTIVE = 0x40FFFFFF;
	/** 「选中」的白框（锚定件、连击筛选选中的那个）。 */
	private static final int SELECT_COLOR = 0xFFFFFFFF;

	/** 选择页的物品网格：7 列 × 4 行，每格 20。 */
	private static final int GRID_COLS = 7;
	private static final int GRID_ROWS = 4;
	private static final int CELL = 20;
	private static final int GRID_WIDTH = GRID_COLS * CELL;
	private static final int GRID_X = (WishPoolMenu.IMAGE_WIDTH - GRID_WIDTH) / 2;
	private static final int GRID_Y = 100;
	/** 品质筛选那一行的 y 与连击筛选两行的 y。 */
	private static final int RARITY_ROW_Y = 26;
	private static final int COMBO_ROW_Y = 52;
	private static final int COMBO_ROW_HEIGHT = 22;
	/** 选择页底部按钮那一行的 y。 */
	private static final int PICKER_BUTTON_Y = WishPoolMenu.IMAGE_HEIGHT - 24;
	/** 抽卡动画：每张之间隔几刻（**不自动返回**，等玩家点一下——用户 2026-10-15 要求）。 */
	private static final int REVEAL_TICKS = 3;

	private enum Mode {
		/** 卡池页：抽卡。 */
		POOL,
		/** 锚定选择页。 */
		ANCHOR,
		/** 神铸选择页。 */
		FORGE,
		/** 抽卡动画。 */
		DRAWING
	}

	private Mode mode = Mode.POOL;
	/** 当前是不是石板池（左侧标签页）。 */
	private boolean slatePool;
	/** 选择页的筛选：品质（null = 全部）与连击（null = 全部）。 */
	private ArtifactRarity rarityFilter;
	private ArtifactCombo comboFilter;
	/** 连击筛选里选中的是不是「无连击」那一格（「书」这类不吃连击的神器；与 comboFilter 互斥）。 */
	private boolean noComboFilter;
	private int page;
	/** 抽卡动画：要展示的东西与已经过去的刻数。 */
	private List<ItemStack> drawn = List.of();
	private int drawnTick;

	private Button artifactTab;
	private Button slateTab;
	private Button anchorButton;
	private Button singleButton;
	private Button tenButton;
	private Button forgeButton;
	private Button backButton;
	private Button clearAnchorButton;
	/** 神铸页的「确认」：点物品只是选中，按它才真的花能量（用户 2026-10-15 要求防误点）。 */
	private Button confirmButton;
	/** 神铸页当前选中的那件（null = 没选）。 */
	private Item forgeSelection;
	private Button prevPageButton;
	private Button nextPageButton;
	private final List<Button> rarityButtons = new ArrayList<>();
	private final List<ArtifactRarity> rarityButtonValues = new ArrayList<>();
	/**
	 * 连击筛选：**自己画的图标按钮**（原版按钮的底纹会把图标盖住，用户 2026-10-15 报「只有按钮没有图标」）。
	 * 每个元素是 {x, y}（绝对坐标），与 {@link #comboButtonValues} 一一对应。
	 */
	private final List<int[]> comboRects = new ArrayList<>();
	/** 与 {@link #comboRects} 一一对应的筛选值：最后一格是「无连击」，值记 null。 */
	private final List<ArtifactCombo> comboButtonValues = new ArrayList<>();

	public WishPoolScreen(WishPoolMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WishPoolMenu.IMAGE_WIDTH, WishPoolMenu.IMAGE_HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		this.rarityButtons.clear();
		this.rarityButtonValues.clear();
		this.comboRects.clear();
		this.comboButtonValues.clear();

		// 左侧两个卡池标签
		this.artifactTab = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.pool_artifact"),
						pressed -> switchPool(false))
				.bounds(this.leftPos + WishPoolMenu.TAB_X, this.topPos + WishPoolMenu.TAB_Y,
						WishPoolMenu.TAB_WIDTH, WishPoolMenu.TAB_HEIGHT)
				.build();
		this.slateTab = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.pool_slate"),
						pressed -> switchPool(true))
				.bounds(this.leftPos + WishPoolMenu.TAB_X,
						this.topPos + WishPoolMenu.TAB_Y + WishPoolMenu.TAB_HEIGHT + WishPoolMenu.TAB_GAP,
						WishPoolMenu.TAB_WIDTH, WishPoolMenu.TAB_HEIGHT)
				.build();
		addRenderableWidget(this.artifactTab);
		addRenderableWidget(this.slateTab);

		// 卡池页：锚定 / 单抽 / 十连 / 神铸
		this.anchorButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.anchor"),
						pressed -> openMode(Mode.ANCHOR))
				.bounds(this.leftPos + WishPoolMenu.ANCHOR_BUTTON_X, this.topPos + WishPoolMenu.ANCHOR_BUTTON_Y,
						WishPoolMenu.POOL_BUTTON_WIDTH, WishPoolMenu.POOL_BUTTON_HEIGHT)
				.build();
		this.singleButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.draw_single"),
						pressed -> ClientPlayNetworking.send(WishPoolClickPayload.draw(this.slatePool, false)))
				.bounds(this.leftPos + WishPoolMenu.SINGLE_BUTTON_X, this.topPos + WishPoolMenu.DRAW_BUTTON_Y,
						WishPoolMenu.DRAW_BUTTON_WIDTH, WishPoolMenu.DRAW_BUTTON_HEIGHT)
				.build();
		this.tenButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.draw_ten"),
						pressed -> ClientPlayNetworking.send(WishPoolClickPayload.draw(this.slatePool, true)))
				.bounds(this.leftPos + WishPoolMenu.TEN_BUTTON_X, this.topPos + WishPoolMenu.DRAW_BUTTON_Y,
						WishPoolMenu.DRAW_BUTTON_WIDTH, WishPoolMenu.DRAW_BUTTON_HEIGHT)
				.build();
		this.forgeButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.forge"),
						pressed -> openMode(Mode.FORGE))
				.bounds(this.leftPos + WishPoolMenu.FORGE_BUTTON_X, this.topPos + WishPoolMenu.FORGE_BUTTON_Y,
						WishPoolMenu.POOL_BUTTON_WIDTH, WishPoolMenu.POOL_BUTTON_HEIGHT)
				.build();
		addRenderableWidget(this.anchorButton);
		addRenderableWidget(this.singleButton);
		addRenderableWidget(this.tenButton);
		addRenderableWidget(this.forgeButton);

		// 选择页：品质筛选（按当前池子里实际有的档位生成）
		List<ArtifactRarity> rarities = raritiesOf(currentPool());
		int rarityWidth = Math.min(40, (WishPoolMenu.IMAGE_WIDTH - 16) / Math.max(1, rarities.size() + 1));

		rarityButtonsAdd(Component.translatable("screen.sephiria_fantasy.wish_pool.filter_all"), null,
				this.leftPos + 8, this.topPos + RARITY_ROW_Y, rarityWidth);

		for (int index = 0; index < rarities.size(); index++) {
			ArtifactRarity rarity = rarities.get(index);
			rarityButtonsAdd(Component.translatable(rarity.translationKey()).withColor(rarity.color()), rarity,
					this.leftPos + 8 + (index + 1) * rarityWidth, this.topPos + RARITY_ROW_Y, rarityWidth);
		}

		// 选择页：连击筛选（神器池才有）——格子位置记下来，图标在 renderPicker 里自己画
		if (!this.slatePool) {
			for (ArtifactCombo combo : ArtifactCombo.values()) {
				int index = this.comboButtonValues.size();
				int column = index % GRID_COLS;
				int row = index / GRID_COLS;
				this.comboRects.add(new int[] {
						this.leftPos + GRID_X + column * CELL,
						this.topPos + COMBO_ROW_Y + row * COMBO_ROW_HEIGHT });
				this.comboButtonValues.add(combo);
			}

			// 最后一格是「无连击」（「书」这类一件连击都不吃的神器）：值记 null，
			// 选中状态在 noComboFilter 上；图标是自带的那张小圆盘（combo_none.png）
			int index = this.comboButtonValues.size();
			this.comboRects.add(new int[] {
					this.leftPos + GRID_X + (index % GRID_COLS) * CELL,
					this.topPos + COMBO_ROW_Y + (index / GRID_COLS) * COMBO_ROW_HEIGHT });
			this.comboButtonValues.add(null);
		}

		// 选择页底部：返回 / 取消锚定 / 翻页
		this.backButton = Button.builder(Component.translatable("gui.back"), pressed -> openMode(Mode.POOL))
				.bounds(this.leftPos + 8, this.topPos + PICKER_BUTTON_Y, 56, 16)
				.build();
		this.clearAnchorButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.anchor_clear"),
						pressed -> ClientPlayNetworking.send(WishPoolClickPayload.anchor(this.slatePool, 0)))
				.bounds(this.leftPos + 70, this.topPos + PICKER_BUTTON_Y, 60, 16)
				.build();
		this.prevPageButton = Button.builder(Component.literal("<"), pressed -> {
					this.page = Math.max(0, this.page - 1);
					refreshButtons();
				})
				.bounds(this.leftPos + WishPoolMenu.IMAGE_WIDTH - 48, this.topPos + PICKER_BUTTON_Y, 18, 16)
				.build();
		this.nextPageButton = Button.builder(Component.literal(">"), pressed -> {
					this.page++;
					refreshButtons();
				})
				.bounds(this.leftPos + WishPoolMenu.IMAGE_WIDTH - 26, this.topPos + PICKER_BUTTON_Y, 18, 16)
				.build();
		this.confirmButton = Button.builder(Component.translatable("screen.sephiria_fantasy.wish_pool.confirm"),
						pressed -> confirmForge())
				.bounds(this.leftPos + 70, this.topPos + PICKER_BUTTON_Y, 56, 16)
				.build();
		addRenderableWidget(this.backButton);
		addRenderableWidget(this.clearAnchorButton);
		addRenderableWidget(this.confirmButton);
		addRenderableWidget(this.prevPageButton);
		addRenderableWidget(this.nextPageButton);

		refreshButtons();
	}

	private void rarityButtonsAdd(Component label, ArtifactRarity value, int x, int y, int width) {
		Button button = Button.builder(label, pressed -> {
					this.rarityFilter = value;
					this.page = 0;
					refreshButtons();
				})
				.bounds(x, y, width, 18)
				.build();
		this.rarityButtons.add(button);
		this.rarityButtonValues.add(value);
		addRenderableWidget(button);
	}

	private void switchPool(boolean slate) {
		this.slatePool = slate;
		this.rarityFilter = null;
		this.comboFilter = null;
		this.noComboFilter = false;
		this.forgeSelection = null;
		this.page = 0;
		// 池子换了、品质档位可能不同 → 重建筛选按钮（重新 init 最省事）
		rebuildWidgets();
	}

	private void openMode(Mode next) {
		this.mode = next;
		this.page = 0;
		refreshButtons();
	}

	/** 按当前模式与筛选刷新所有按钮的可见性 / 可用状态。 */
	private void refreshButtons() {
		boolean pool = this.mode == Mode.POOL;
		boolean picker = this.mode == Mode.ANCHOR || this.mode == Mode.FORGE;

		this.artifactTab.visible = pool;
		this.slateTab.visible = pool;
		this.anchorButton.visible = pool;
		this.singleButton.visible = pool;
		this.tenButton.visible = pool;
		this.forgeButton.visible = pool;
		// 四叶草数量读菜单同步过来的值（服务端权威）；不够就置灰
		this.singleButton.active = this.menu.clovers() >= WishPool.CLOVERS_PER_DRAW;
		this.tenButton.active = this.menu.clovers() >= WishPool.TEN_DRAW * WishPool.CLOVERS_PER_DRAW;

		for (Button button : this.rarityButtons) {
			button.visible = picker;
		}

		this.backButton.visible = picker;
		this.clearAnchorButton.visible = this.mode == Mode.ANCHOR;
		this.confirmButton.visible = this.mode == Mode.FORGE;
		// 没选中、或者能量不够，都不给按（防误点把能量花掉）
		this.confirmButton.active = this.mode == Mode.FORGE && this.forgeSelection != null
				&& this.menu.energy() >= WishPool.forgeCost(ArtifactLoot.rarityOf(this.forgeSelection));
		this.prevPageButton.visible = picker;
		this.nextPageButton.visible = picker;
		int pages = pageCount();

		this.prevPageButton.active = this.page > 0;
		this.nextPageButton.active = this.page + 1 < pages;
	}

	/** 选择页当前要列出的物品（锚定 = 整个池子；神铸 = 只要黄与红）。 */
	private List<Item> filteredItems() {
		List<Item> pool = currentPool();
		List<Item> result = new ArrayList<>();

		for (Item item : pool) {
			if (this.mode == Mode.FORGE && WishPool.forgeCost(ArtifactLoot.rarityOf(item)) <= 0) {
				continue;
			}

			if (this.rarityFilter != null && ArtifactLoot.rarityOf(item) != this.rarityFilter) {
				continue;
			}

			if (this.comboFilter != null
					&& (!(item instanceof SephiriaArtifact artifact) || !artifact.combos().contains(this.comboFilter))) {
				continue;
			}

			// 「无连击」筛的是「一件连击都不吃」的神器（书）
			if (this.noComboFilter
					&& (!(item instanceof SephiriaArtifact artifact) || !artifact.combos().isEmpty())) {
				continue;
			}

			result.add(item);
		}

		return result;
	}

	private List<Item> currentPool() {
		return WishPool.poolOf(this.slatePool);
	}

	private static List<ArtifactRarity> raritiesOf(List<Item> pool) {
		List<ArtifactRarity> rarities = new ArrayList<>();

		for (ArtifactRarity rarity : ArtifactRarity.values()) {
			for (Item item : pool) {
				if (ArtifactLoot.rarityOf(item) == rarity) {
					rarities.add(rarity);
					break;
				}
			}
		}

		return rarities;
	}

	private static int perPage() {
		return GRID_COLS * GRID_ROWS;
	}

	private int pageCount() {
		return Math.max(1, (filteredItems().size() + perPage() - 1) / perPage());
	}

	// ------------------------------------------------------------------ 每帧

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 服务端抽完把结果发过来 → 这里接上开始播动画
		if (this.mode != Mode.DRAWING) {
			List<ItemStack> pending = ClientWishPool.takePending();

			if (pending != null && !pending.isEmpty()) {
				this.drawn = pending;
				this.drawnTick = 0;
				this.mode = Mode.DRAWING;
				refreshButtons();
			}
		} else {
			// 只推时间轴：**不自动返回**（用户 2026-10-15：要让玩家自己看够了再点一下返回）
			this.drawnTick++;
		}

		refreshButtons();
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	// ------------------------------------------------------------------ 绘制

	@Override
	public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		extractBlurredBackground(extractor);
		extractMenuBackground(extractor);

		int left = this.leftPos;
		int top = this.topPos;
		extractor.fill(left - 1, top - 1, left + this.imageWidth + 1, top + WishPoolMenu.IMAGE_HEIGHT + 1, BORDER_DARK);
		extractor.fill(left, top, left + this.imageWidth, top + WishPoolMenu.IMAGE_HEIGHT, PANEL_COLOR);
		extractor.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		switch (this.mode) {
			case POOL -> renderPool(extractor, mouseX, mouseY);
			case ANCHOR, FORGE -> renderPicker(extractor, mouseX, mouseY);
			case DRAWING -> renderDrawing(extractor);
		}
	}

	/** 卡池页：锚定物品图标、能量与四叶草，以及神铸下面的「溢出转化」开关。 */
	private void renderPool(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;

		// 当前标签页的高亮（按钮本身是原版控件，这里在它后面铺一层底）
		boolean slate = this.slatePool;
		int tabY = top + WishPoolMenu.TAB_Y + (slate ? WishPoolMenu.TAB_HEIGHT + WishPoolMenu.TAB_GAP : 0);
		extractor.fill(left + WishPoolMenu.TAB_X - 2, tabY - 2, left + WishPoolMenu.TAB_X + WishPoolMenu.TAB_WIDTH + 2,
				tabY + WishPoolMenu.TAB_HEIGHT + 2, TAB_ACTIVE);

		// 锚定物品的图标（没锚定就画一个空框）
		int iconX = left + WishPoolMenu.ANCHOR_ICON_X;
		int iconY = top + WishPoolMenu.ANCHOR_ICON_Y;
		extractor.fill(iconX - 2, iconY - 2, iconX + 22, iconY + 22, BOX_COLOR);
		Item anchor = anchorItem();

		if (anchor != null) {
			drawIcon(extractor, anchor, iconX + 2, iconY + 2, 16);
		} else {
			Component none = Component.translatable("screen.sephiria_fantasy.wish_pool.anchor_none");
			extractor.text(this.font, none, iconX + 10 - this.font.width(none) / 2, iconY + 6, DIM_COLOR);
		}

		// 四叶草与能量同一行：**四叶草在前、能量在后**（用户 2026-10-15 的口径）
		Component clovers = Component.translatable("screen.sephiria_fantasy.wish_pool.clovers",
				Component.literal(String.valueOf(this.menu.clovers())).withColor(CLOVER_COLOR));
		int statsY = top + WishPoolMenu.STATS_TEXT_Y;
		extractor.text(this.font, clovers, left + WishPoolMenu.SINGLE_BUTTON_X, statsY, TEXT_COLOR);
		Component energy = Component.translatable("screen.sephiria_fantasy.wish_pool.energy",
				Component.literal(String.valueOf(this.menu.energy())).withColor(ENERGY_COLOR));
		extractor.text(this.font, energy,
				left + WishPoolMenu.SINGLE_BUTTON_X + this.font.width(clovers) + 12, statsY, TEXT_COLOR);

		// 最左上角：方块名字（用户 2026-10-15 要求；这个界面没有物品栏，标题自己画）
		extractor.text(this.font, this.title, left + WishPoolMenu.TITLE_X, top + WishPoolMenu.TITLE_Y, TEXT_COLOR);

		renderOverflowToggle(extractor, mouseX, mouseY);
	}

	/**
	 * 神铸按钮下面那一行：「溢出的升级宝箱自动转化为四叶草：开 / 关」。
	 *
	 * <p>整行可点（界面自己画的控件，没有用原版按钮——这句话太长，84 像素的按钮装不下）。
	 * 「开 / 关」两个字跟着状态换色（开 = 四叶草绿、关 = 灰），鼠标划上去整行描边提亮。
	 */
	private void renderOverflowToggle(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		int x = this.leftPos + WishPoolMenu.OVERFLOW_TOGGLE_X;
		int y = this.topPos + WishPoolMenu.OVERFLOW_TOGGLE_Y;
		int width = WishPoolMenu.OVERFLOW_TOGGLE_WIDTH;
		int height = WishPoolMenu.OVERFLOW_TOGGLE_HEIGHT;
		boolean on = this.menu.overflowToClover();
		boolean hovered = isOverOverflowToggle(mouseX, mouseY);

		extractor.fill(x, y, x + width, y + height, BOX_COLOR);
		extractor.fill(x, y, x + width, y + 1, hovered ? BORDER_LIGHT : BORDER_DARK);

		Component state = Component.translatable(on
				? "screen.sephiria_fantasy.wish_pool.overflow_on"
				: "screen.sephiria_fantasy.wish_pool.overflow_off").withColor(on ? CLOVER_COLOR : DIM_COLOR);
		Component label = Component.translatable("screen.sephiria_fantasy.wish_pool.overflow_toggle", state);

		extractor.text(this.font, label, x + (width - this.font.width(label)) / 2, y + 5, TEXT_COLOR);
	}

	/** 鼠标在这一行开关上吗（点击与描边提亮都用它）。 */
	private boolean isOverOverflowToggle(int mouseX, int mouseY) {
		int x = this.leftPos + WishPoolMenu.OVERFLOW_TOGGLE_X;
		int y = this.topPos + WishPoolMenu.OVERFLOW_TOGGLE_Y;

		return this.mode == Mode.POOL
				&& mouseX >= x && mouseX < x + WishPoolMenu.OVERFLOW_TOGGLE_WIDTH
				&& mouseY >= y && mouseY < y + WishPoolMenu.OVERFLOW_TOGGLE_HEIGHT;
	}

	/** 选择页：标题 + 连击筛选图标的底色 + 物品网格。 */
	private void renderPicker(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;
		// 左上角同样是方块名字（与卡池页一致），这一页的标题居中画在它右边
		extractor.text(this.font, this.title, left + WishPoolMenu.TITLE_X, top + WishPoolMenu.TITLE_Y, HEADER_COLOR);
		// 神铸页的标题要带上已积攒的愿力（用户 2026-10-15 的口径：神铸：许愿池已积攒愿力：x）
		Component title = this.mode == Mode.FORGE
				? Component.translatable("screen.sephiria_fantasy.wish_pool.forge_title",
						Component.literal(String.valueOf(this.menu.energy())).withColor(ENERGY_COLOR))
				: Component.translatable("screen.sephiria_fantasy.wish_pool.anchor_title");
		extractor.text(this.font, title, left + (this.imageWidth - this.font.width(title)) / 2,
				top + WishPoolMenu.TITLE_Y, TEXT_COLOR);

		// 连击筛选：自己画的图标按钮——选中的那个加白框（与物品网格里选中锚定件同一套观感）
		for (int index = 0; index < this.comboRects.size(); index++) {
			int x = this.comboRects.get(index)[0];
			int y = this.comboRects.get(index)[1];
			ArtifactCombo combo = this.comboButtonValues.get(index);
			// null = 「无连击」那一格（选中状态记在 noComboFilter 上，不在 comboFilter 里）
			boolean selected = combo == null ? this.noComboFilter : combo == this.comboFilter;
			boolean hovered = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;
			extractor.fill(x, y, x + CELL - 2, y + CELL - 2, hovered ? 0x50FFFFFF : BOX_COLOR);
			blitIcon(extractor, comboIcon(combo), x + 1, y + 1, 16);

			if (selected) {
				// 白框：一格宽、贴着格子内沿
				extractor.fill(x, y, x + CELL - 2, y + 1, SELECT_COLOR);
				extractor.fill(x, y + CELL - 3, x + CELL - 2, y + CELL - 2, SELECT_COLOR);
				extractor.fill(x, y, x + 1, y + CELL - 2, SELECT_COLOR);
				extractor.fill(x + CELL - 3, y, x + CELL - 2, y + CELL - 2, SELECT_COLOR);
			}
		}

		// 物品网格
		List<Item> items = filteredItems();
		int start = this.page * perPage();

		for (int index = 0; index < perPage(); index++) {
			int slot = start + index;

			if (slot >= items.size()) {
				break;
			}

			int column = index % GRID_COLS;
			int row = index / GRID_COLS;
			int x = left + GRID_X + column * CELL;
			int y = top + GRID_Y + row * CELL;
			Item item = items.get(slot);
			ArtifactRarity rarity = ArtifactLoot.rarityOf(item);
			boolean hovered = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;

			extractor.fill(x, y, x + CELL - 2, y + CELL - 2, hovered ? 0x50FFFFFF : BOX_COLOR);
			drawIcon(extractor, item, x + 1, y + 1, 16);
			drawRarityStrip(extractor, x, y + CELL - 3, CELL - 2, rarity);

			// 锚定页 = 当前锚定的那件；神铸页 = 刚选中的那件。都套白框（用户 2026-10-15 的口径）
			boolean selected = this.mode == Mode.ANCHOR ? item == anchorItem()
					: this.mode == Mode.FORGE && item == this.forgeSelection;

			if (selected) {
				extractor.fill(x, y, x + CELL - 2, y + 1, SELECT_COLOR);
				extractor.fill(x, y + CELL - 3, x + CELL - 2, y + CELL - 2, SELECT_COLOR);
				extractor.fill(x, y, x + 1, y + CELL - 2, SELECT_COLOR);
				extractor.fill(x + CELL - 3, y, x + CELL - 2, y + CELL - 2, SELECT_COLOR);
			}
		}

		// 页码
		Component pageText = Component.literal((this.page + 1) + " / " + pageCount());
		extractor.text(this.font, pageText,
				left + WishPoolMenu.IMAGE_WIDTH - 56 - this.font.width(pageText),
				top + PICKER_BUTTON_Y + 4, DIM_COLOR);

	}

	/** 抽卡动画：一张张亮出来，亮完停一下回卡池页。 */
	private void renderDrawing(GuiGraphicsExtractor extractor) {
		int left = this.leftPos;
		int top = this.topPos;
		int count = this.drawn.size();
		int columns = count <= 1 ? 1 : Math.min(5, count);
		int rows = (count + columns - 1) / columns;
		int cell = count <= 1 ? 56 : 28;
		int gridWidth = columns * cell;
		int gridHeight = rows * cell;
		int originX = left + (this.imageWidth - gridWidth) / 2;
		int originY = top + (WishPoolMenu.IMAGE_HEIGHT - gridHeight) / 2;
		int shown = Math.min(count, this.drawnTick / REVEAL_TICKS + 1);

		extractor.text(this.font, Component.translatable("screen.sephiria_fantasy.wish_pool.drawing"),
				left + 8, top + 8, TEXT_COLOR);

		for (int index = 0; index < count; index++) {
			int column = index % columns;
			int row = index / columns;
			int x = originX + column * cell;
			int y = originY + row * cell;

			if (index >= shown) {
				// 还没亮：一个暗框
				extractor.fill(x, y, x + cell - 4, y + cell - 4, BOX_COLOR);
				continue;
			}

			ItemStack stack = this.drawn.get(index);
			ArtifactRarity rarity = ArtifactLoot.rarityOf(stack.getItem());
			// 品质底色**一直保留**（用户 2026-10-15：只闪一下的话动画结束后看不出品质），
			// 刚亮出来的那一张再亮一档，看着像「闪一下」
			boolean fresh = index == shown - 1;
			int tint = (fresh ? 0xA0 : 0x50) << 24 | (rarity.color() & 0xFFFFFF);
			extractor.fill(x, y, x + cell - 4, y + cell - 4, tint);
			drawIcon(extractor, stack.getItem(), x + 2, y + 2, cell - 8);
			// 下沿再画一条品质色条（羁绊是彩虹，与选择页同一套），这样一眼能读出品质
			drawRarityStrip(extractor, x, y + cell - 5, cell - 4, rarity);
		}

		// 这一批攒了多少能量
		int gained = 0;

		for (ItemStack stack : this.drawn) {
			gained += WishPool.energyFor(stack.getItem());
		}

		Component energy = Component.translatable("screen.sephiria_fantasy.wish_pool.energy_gain",
				Component.literal(String.valueOf(gained)).withColor(ENERGY_COLOR));
		extractor.text(this.font, energy, left + (this.imageWidth - this.font.width(energy)) / 2,
				top + WishPoolMenu.IMAGE_HEIGHT - 40, TEXT_COLOR);

		// 提示一直显示：动画不会再自动返回，要点一下才回卡池页（用户 2026-10-15）
		Component skip = Component.translatable("screen.sephiria_fantasy.wish_pool.drawing_skip");
		extractor.text(this.font, skip, left + (this.imageWidth - this.font.width(skip)) / 2,
				top + WishPoolMenu.IMAGE_HEIGHT - 24, DIM_COLOR);
	}

	/** 左上角不画原版那两行标签（这个界面没有物品栏）。 */
	@Override
	protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
	}

	/** 悬停浮窗：选择页里显示物品名字（神铸页再带上能量价格）。 */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		super.extractTooltip(extractor, mouseX, mouseY);

		if (this.mode != Mode.ANCHOR && this.mode != Mode.FORGE) {
			return;
		}

		// 连击筛选按钮的悬停说明
		for (int index = 0; index < this.comboRects.size(); index++) {
			int[] rect = this.comboRects.get(index);

			if (mouseX >= rect[0] && mouseX < rect[0] + CELL - 2
					&& mouseY >= rect[1] && mouseY < rect[1] + CELL - 2) {
				ArtifactCombo combo = this.comboButtonValues.get(index);
				renderTooltip(extractor, List.of(combo == null
						? Component.translatable("screen.sephiria_fantasy.wish_pool.combo_none")
						: Component.translatable(combo.translationKey())), mouseX, mouseY);
				return;
			}
		}

		Item hovered = itemAt(mouseX, mouseY);

		if (hovered == null) {
			return;
		}

		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(hovered.getDescriptionId()).withColor(ArtifactLoot.rarityOf(hovered).color()));

		if (this.mode == Mode.FORGE) {
			int cost = WishPool.forgeCost(ArtifactLoot.rarityOf(hovered));
			lines.add(Component.translatable("screen.sephiria_fantasy.wish_pool.forge_cost",
					Component.literal(String.valueOf(cost)).withColor(
							this.menu.energy() >= cost ? ENERGY_COLOR : ArtifactRarity.COMMON.color())));
		}

		renderTooltip(extractor, lines, mouseX, mouseY);
	}

	/** 网格里鼠标下的物品。 */
	private Item itemAt(int mouseX, int mouseY) {
		List<Item> items = filteredItems();

		for (int index = 0; index < perPage(); index++) {
			int slot = this.page * perPage() + index;

			if (slot >= items.size()) {
				return null;
			}

			int column = index % GRID_COLS;
			int row = index / GRID_COLS;
			int x = this.leftPos + GRID_X + column * CELL;
			int y = this.topPos + GRID_Y + row * CELL;

			if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) {
				return items.get(slot);
			}
		}

		return null;
	}

	/**
	 * 点击：动画期间点一下先跳到全部显示、再点一下回卡池页；选择页点物品就是锚定 / 神铸。
	 */
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (this.mode == Mode.DRAWING) {
			int allShown = this.drawn.size() * REVEAL_TICKS;

			if (this.drawnTick < allShown) {
				this.drawnTick = allShown;
			} else {
				// 全部亮出来之后再点一下才返回（这样玩家能看清抽到了什么）
				openMode(Mode.POOL);
			}

			return true;
		}

		// 卡池页：点神铸下面那行开关（自己画的控件，见 renderOverflowToggle）
		if (isOverOverflowToggle((int) event.x(), (int) event.y())) {
			ClientPlayNetworking.send(WishPoolClickPayload.toggleOverflow());
			return true;
		}

		if (this.mode == Mode.ANCHOR || this.mode == Mode.FORGE) {
			// 连击筛选格子（自己画的按钮）
			for (int index = 0; index < this.comboRects.size(); index++) {
				int[] rect = this.comboRects.get(index);

				if (event.x() >= rect[0] && event.x() < rect[0] + CELL - 2
						&& event.y() >= rect[1] && event.y() < rect[1] + CELL - 2) {
					ArtifactCombo combo = this.comboButtonValues.get(index);

					if (combo == null) {
						// 「无连击」那一格：与具体连击的筛选互斥
						this.noComboFilter = !this.noComboFilter;
						this.comboFilter = null;
					} else {
						this.comboFilter = this.comboFilter == combo ? null : combo;
						this.noComboFilter = false;
					}

					this.page = 0;
					refreshButtons();
					return true;
				}
			}

			Item clicked = itemAt((int) event.x(), (int) event.y());

			if (clicked != null) {
				if (this.mode == Mode.ANCHOR) {
					ClientPlayNetworking.send(
							WishPoolClickPayload.anchor(this.slatePool, BuiltInRegistries.ITEM.getId(clicked)));
				} else {
					// 神铸：点一下只选中（再点一下取消），真正花能量的是「确认」按钮
					this.forgeSelection = this.forgeSelection == clicked ? null : clicked;
					refreshButtons();
				}

				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	/** 神铸页按了「确认」：把选中的那件发过去（够不够能量由服务端再判一次）。 */
	private void confirmForge() {
		if (this.forgeSelection == null) {
			return;
		}

		ClientPlayNetworking.send(WishPoolClickPayload.forge(BuiltInRegistries.ITEM.getId(this.forgeSelection)));
		this.forgeSelection = null;
		refreshButtons();
	}

	// ------------------------------------------------------------------ 小工具

	/** 锚定件（菜单同步过来的是注册表数字 id，0 = 没锚定）。 */
	private Item anchorItem() {
		int id = this.menu.anchorId(this.slatePool);
		return id <= 0 ? null : BuiltInRegistries.ITEM.byId(id);
	}

	/** 连击筛选的图标：null = 「无连击」那一格，用它自己那张小圆盘。 */
	private static Identifier comboIcon(ArtifactCombo combo) {
		String file = combo == null ? "combo_none" : "combo_" + combo.id();
		return Identifier.fromNamespaceAndPath(com.sephiria.Sephiria.MOD_ID, "textures/gui/" + file + ".png");
	}

	/**
	 * 格子下沿那条品质色条。
	 *
	 * <p>羁绊（彩）这一档不走 {@code rarity.color()}——那是个紫色代表色，用户 2026-10-15 要求
	 * 「羁绊神器下面的条从紫色改为彩色」，所以按像素取彩虹色画成一条彩色渐变。
	 */
	private static void drawRarityStrip(GuiGraphicsExtractor extractor, int x, int y, int width,
			ArtifactRarity rarity) {
		if (rarity != ArtifactRarity.BOND) {
			extractor.fill(x, y, x + width, y + 1, 0xFF000000 | (rarity.color() & 0xFFFFFF));
			return;
		}

		int colours = com.sephiria.util.Colours.rainbowSize();

		for (int i = 0; i < width; i++) {
			extractor.fill(x + i, y, x + i + 1, y + 1,
					com.sephiria.util.Colours.rainbowAt(i * colours / Math.max(1, width)));
		}
	}

	/** 画物品图标：只画本模组物品（贴图一定在 textures/item/<id>.png），不能走原版物品绘制。 */
	private static void drawIcon(GuiGraphicsExtractor extractor, Item item, int x, int y, int size) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		blitIcon(extractor, Identifier.fromNamespaceAndPath(id.getNamespace(),
				"textures/item/" + id.getPath() + ".png"), x, y, size);
	}

	/**
	 * 直接 blit 一张本模组的 16×16 贴图（物品图标 / 连击图标都走它）。
	 *
	 * <p>要放大就<b>改 pose 缩放</b>，不能把 blit 的 width/height 传成大于 16：
	 * 那两个参数是「采样范围」，UV 会算成 width/16 &gt; 1，贴图按 2×2 平铺
	 * ——单抽那张大图因此显示成四个图标（用户 2026-10-15 报的 bug）。
	 */
	private static void blitIcon(GuiGraphicsExtractor extractor, Identifier texture, int x, int y, int size) {
		if (size == 16) {
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
			return;
		}

		org.joml.Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(size / 16.0F, size / 16.0F);
		extractor.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
		pose.popMatrix();
	}

	private void renderTooltip(GuiGraphicsExtractor extractor, List<Component> lines, int mouseX, int mouseY) {
		Font font = this.font;
		int width = 0;

		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}

		int x = mouseX + 8;
		int y = mouseY + 8;
		int height = lines.size() * 10 + 6;
		extractor.fill(x - 3, y - 3, x + width + 3, y + height - 3, 0xF0100010);
		extractor.fill(x - 3, y - 3, x + width + 3, y - 2, 0xFF5050A0);

		int lineY = y;

		for (Component line : lines) {
			extractor.text(font, line, x, lineY, TEXT_COLOR);
			lineY += 10;
		}
	}

}
