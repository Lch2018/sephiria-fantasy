package com.sephiria.client.difficulty;

import com.sephiria.difficulty.DifficultyOption;
import com.sephiria.difficulty.RootRetreat;
import com.sephiria.difficulty.RootRetreatMenu;
import com.sephiria.network.RootRetreatClickPayload;
import com.sephiria.util.Numbers;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 根之退界面：<b>不显示物品栏和背包</b>（用户口径：这是一个新的界面）。
 *
 * <pre>
 *   ┌──────────────────────────────────────────────────────┐
 *   │ 根之退                 │ 第 3 阶段                    │  ← 右上：这个方块的方案
 *   │ 凶猛的爪子   3 / 8 - + │ 经验加成 30%                 │
 *   │ 顽强之躯     0 / 9 - + │──────────────────────────────│
 *   │ 防护铠甲     0 / 5 - + │ 你的方案                     │  ← 右下：你自己（生效中）的方案
 *   │                        │ 第 1 阶段                    │
 *   │                        │ 凶猛的爪子 1层               │
 *   │                        │ 顽强之躯 0层                 │
 *   │                        │ 防护铠甲 0层                 │
 *   │        [ 清空 ]    [ 应用 ]                          │
 *   └──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>左列改的是当前方块那份方案</b>（点「应用」才会变成你自己的方案，「清空」把它清成 0），
 * 右下角一直显示你自己那份（生效中的）——所以没点应用之前两边会不一样，这是设计。
 *
 * <p>所有数值都读菜单同步过来的数据（服务端权威）；选项多到一屏装不下时滚轮滚动，
 * 左右两列共用同一个滚动位置（列表区裁剪 + 滚出去的按钮藏起来——原版控件不受裁剪影响）。
 *
 * <p>1.20.1 的容器界面没有虚化，背景用原版那层暗色渐变；其余绘制与 26.x 同构。
 */
public class RootRetreatScreen extends AbstractContainerScreen<RootRetreatMenu> {
	private static final int PANEL_COLOR = 0xC0101010;
	private static final int BORDER_LIGHT = 0xFF4A4A52;
	private static final int BORDER_DARK = 0xFF1A1A1E;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	/** 等级那截字（「3 / 8」）的颜色：比正文暗一点。 */
	private static final int LEVEL_COLOR = 0xFF9A9AA8;
	/** 「第 x 阶段」的颜色：金色（与商店页的余额同一套）。 */
	private static final int STAGE_COLOR = 0xFFFFD24A;
	/** 「经验加成 x%」的颜色：绿色（祝福对玩家是好处）。 */
	private static final int BLESSING_COLOR = 0xFF55FF55;
	/** 未激活的祝福名字：同色系压暗（力量 / 轻盈绿、勇气黄）。 */
	private static final int BLESSING_DIM_GREEN = 0xFF3F7F3F;
	private static final int BLESSING_DIM_YELLOW = 0xFF7F6A2A;
	/** 小标题（「你的方案」）的颜色。 */
	private static final int HEADER_COLOR = 0xFFAAAAAA;
	/** 鼠标下那一行的底色。 */
	private static final int ROW_HOVER_COLOR = 0x28FFFFFF;
	/** 右侧两个信息块的底色。 */
	private static final int BOX_COLOR = 0x30000000;
	private static final int SCROLLBAR_COLOR = 0x30FFFFFF;
	private static final int SCROLLBAR_THUMB = 0xFF9A9AA8;
	private static final int SCROLLBAR_WIDTH = 2;

	/** 滚动位置：第一行可见的是第几项。 */
	private int scrollRow;
	private final List<Button> minusButtons = new ArrayList<>();
	private final List<Button> plusButtons = new ArrayList<>();
	private Button clearButton;

	public RootRetreatScreen(RootRetreatMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = RootRetreatMenu.IMAGE_WIDTH;
		this.imageHeight = RootRetreatMenu.imageHeight();
	}

	@Override
	protected void init() {
		super.init();
		this.minusButtons.clear();
		this.plusButtons.clear();
		DifficultyOption[] options = DifficultyOption.values();

		for (int index = 0; index < options.length; index++) {
			// lambda 里要的是「这一项的下标」，循环变量得先拷一份（它本身不是 effectively final）
			int optionIndex = index;
			Button minus = Button.builder(Component.literal("-"),
							pressed -> send(RootRetreatClickPayload.adjust(optionIndex, -1)))
					.bounds(0, 0, RootRetreatMenu.BUTTON_SIZE, RootRetreatMenu.BUTTON_SIZE)
					.build();
			Button plus = Button.builder(Component.literal("+"),
							pressed -> send(RootRetreatClickPayload.adjust(optionIndex, 1)))
					.bounds(0, 0, RootRetreatMenu.BUTTON_SIZE, RootRetreatMenu.BUTTON_SIZE)
					.build();
			this.minusButtons.add(minus);
			this.plusButtons.add(plus);
			addRenderableWidget(minus);
			addRenderableWidget(plus);
		}

		// 底部：清空（把当前方块那份清成 0）与 应用（把它拷到自己身上）
		this.clearButton = Button.builder(Component.translatable("screen.sephiria_fantasy.root_retreat.clear"),
						pressed -> send(RootRetreatClickPayload.clear()))
				.bounds(this.leftPos + RootRetreatMenu.CLEAR_BUTTON_X, this.topPos + RootRetreatMenu.BUTTON_Y,
						RootRetreatMenu.WIDE_BUTTON_WIDTH, RootRetreatMenu.WIDE_BUTTON_HEIGHT)
				.build();
		addRenderableWidget(this.clearButton);
		addRenderableWidget(Button.builder(Component.translatable("screen.sephiria_fantasy.root_retreat.apply"),
						pressed -> send(RootRetreatClickPayload.apply()))
				.bounds(this.leftPos + RootRetreatMenu.APPLY_BUTTON_X, this.topPos + RootRetreatMenu.BUTTON_Y,
						RootRetreatMenu.WIDE_BUTTON_WIDTH, RootRetreatMenu.WIDE_BUTTON_HEIGHT)
				.build());

		layoutButtons();
	}

	/** 每帧按最新等级刷新按钮状态（0 级时减号灰、满级时加号灰；方块那份全是 0 时清空灰）。 */
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		DifficultyOption[] options = DifficultyOption.values();
		boolean anything = false;

		for (int index = 0; index < options.length; index++) {
			int level = this.menu.blockLevelOf(options[index]);
			boolean visible = this.minusButtons.get(index).visible;
			this.minusButtons.get(index).active = visible && level > 0;
			this.plusButtons.get(index).active = visible && level < options[index].maxLevel();
			anything |= level > 0;
		}

		this.clearButton.active = anything;
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	/** 按当前滚动位置摆按钮，并把滚出可视区的按钮藏起来（原版控件不受裁剪影响）。 */
	private void layoutButtons() {
		int maxScroll = maxScrollRow();
		this.scrollRow = Mth.clamp(this.scrollRow, 0, maxScroll);

		for (int index = 0; index < this.minusButtons.size(); index++) {
			int row = index - this.scrollRow;
			boolean visible = row >= 0 && row < RootRetreatMenu.VISIBLE_ROWS;
			int y = this.topPos + RootRetreatMenu.LIST_TOP + row * RootRetreatMenu.ROW_HEIGHT
					+ RootRetreatMenu.BUTTON_Y_OFFSET;
			place(this.minusButtons.get(index), RootRetreatMenu.MINUS_BUTTON_X, y, visible);
			place(this.plusButtons.get(index), RootRetreatMenu.PLUS_BUTTON_X, y, visible);
		}
	}

	private void place(Button button, int x, int y, boolean visible) {
		button.setPosition(this.leftPos + x, y);
		button.visible = visible;
		button.active = visible;
	}

	/** 最多能滚几行（选项比可视行数少时是 0）。 */
	private static int maxScrollRow() {
		return Math.max(0, DifficultyOption.values().length - RootRetreatMenu.VISIBLE_ROWS);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (scrollY != 0.0D && maxScrollRow() > 0 && isOverList(mouseX, mouseY)) {
			this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
			layoutButtons();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollY);
	}

	private boolean isOverList(double mouseX, double mouseY) {
		return mouseX >= this.leftPos && mouseX < this.leftPos + RootRetreatMenu.RIGHT_X
				&& mouseY >= this.topPos + RootRetreatMenu.LIST_TOP
				&& mouseY < this.topPos + RootRetreatMenu.LIST_TOP + RootRetreatMenu.LIST_HEIGHT;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int left = this.leftPos;
		int top = this.topPos;

		graphics.fill(left - 1, top - 1, left + this.imageWidth + 1, top + this.imageHeight + 1, BORDER_DARK);
		graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, PANEL_COLOR);
		graphics.fill(left, top, left + this.imageWidth, top + 1, BORDER_LIGHT);

		renderOptions(graphics, mouseX, mouseY);
		renderRightColumn(graphics);
	}

	/** 左列：当前方块那份方案（名字 + 等级）。滚出去的行由裁剪挡掉。 */
	private void renderOptions(GuiGraphics graphics, int mouseX, int mouseY) {
		Font font = this.font;
		DifficultyOption[] options = DifficultyOption.values();
		int left = this.leftPos;
		int listTop = this.topPos + RootRetreatMenu.LIST_TOP;

		graphics.enableScissor(left, listTop, left + RootRetreatMenu.RIGHT_X,
				listTop + RootRetreatMenu.LIST_HEIGHT);

		DifficultyOption hovered = hoveredOption(mouseX, mouseY);

		for (int index = 0; index < options.length; index++) {
			int row = index - this.scrollRow;

			if (row < 0 || row >= RootRetreatMenu.VISIBLE_ROWS) {
				continue;
			}

			int rowY = listTop + row * RootRetreatMenu.ROW_HEIGHT;

			if (options[index] == hovered) {
				graphics.fill(left + 2, rowY, left + RootRetreatMenu.RIGHT_X - 4,
						rowY + RootRetreatMenu.ROW_HEIGHT - 1, ROW_HOVER_COLOR);
			}

			int level = this.menu.blockLevelOf(options[index]);
			// 未激活的那一项整行暗一点，一眼能看出「还没开」
			graphics.drawString(font, options[index].label(), left + RootRetreatMenu.ROW_TEXT_X, rowY + 7,
					level > 0 ? TEXT_COLOR : LEVEL_COLOR, false);

			Component levelText = Component.literal(level + " / " + options[index].maxLevel());
			graphics.drawString(font, levelText,
					left + RootRetreatMenu.ROW_LEVEL_RIGHT - font.width(levelText), rowY + 7, LEVEL_COLOR, false);
		}

		graphics.disableScissor();

		if (maxScrollRow() > 0) {
			renderScrollbar(graphics, left, listTop);
		}
	}

	/** 列表右缘的滚动条：装不下时才画。 */
	private void renderScrollbar(GuiGraphics graphics, int left, int listTop) {
		int maxScroll = maxScrollRow();
		int track = RootRetreatMenu.LIST_HEIGHT;
		int thumb = Math.max(SCROLLBAR_WIDTH * 4,
				track * RootRetreatMenu.VISIBLE_ROWS / DifficultyOption.values().length);
		int thumbY = listTop + (track - thumb) * this.scrollRow / maxScroll;
		int x = left + RootRetreatMenu.RIGHT_X - SCROLLBAR_WIDTH - 2;

		graphics.fill(x, listTop, x + SCROLLBAR_WIDTH, listTop + track, SCROLLBAR_COLOR);
		graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, SCROLLBAR_THUMB);
	}

	/**
	 * 右列两块：上面是这个方块的阶段与祝福，下面是你自己（生效中）的方案。
	 *
	 * <p>两块共用左列的滚动位置：选项多的时候两边一起滚，看到的永远是同一段选项。
	 */
	private void renderRightColumn(GuiGraphics graphics) {
		Font font = this.font;
		int left = this.leftPos + RootRetreatMenu.RIGHT_X;
		int top = this.topPos;
		int right = left + RootRetreatMenu.RIGHT_WIDTH;
		int listBottom = top + RootRetreatMenu.LIST_TOP + RootRetreatMenu.LIST_HEIGHT;

		// 右上：这个方块那份方案的阶段 + 它带来的祝福（每层阶段 +10% 经验 + 三条新祝福）
		graphics.fill(left, top + RootRetreatMenu.LIST_TOP, right,
				top + RootRetreatMenu.BLESSING_ROW_Y + 12, BOX_COLOR);
		Component blockStage = Component.translatable("screen.sephiria_fantasy.root_retreat.stage",
				this.menu.blockStage());
		graphics.drawString(font, blockStage,
				left + (RootRetreatMenu.RIGHT_WIDTH - font.width(blockStage)) / 2,
				top + RootRetreatMenu.BLOCK_STAGE_Y, STAGE_COLOR, false);
		Component blessing = Component.translatable("screen.sephiria_fantasy.root_retreat.blessing",
				Component.literal(Numbers.format(this.menu.blockStage() * RootRetreat.XP_PER_STAGE_PERCENT))
						.withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(BLESSING_COLOR))));
		graphics.drawString(font, blessing,
				left + (RootRetreatMenu.RIGHT_WIDTH - font.width(blessing)) / 2,
				top + RootRetreatMenu.BLESSING_Y, TEXT_COLOR, false);
		renderBlessings(graphics, left, top);

		// 右下：你自己的方案——第 1 行「第 x 阶段」，之后每项一行「名字 z层」
		graphics.fill(left, top + RootRetreatMenu.YOUR_PLAN_TITLE_Y - 6, right, listBottom, BOX_COLOR);
		graphics.drawString(font, Component.translatable("screen.sephiria_fantasy.root_retreat.your_plan"),
				left + 4, top + RootRetreatMenu.YOUR_PLAN_TITLE_Y, HEADER_COLOR, false);
		Component yourStage = Component.translatable("screen.sephiria_fantasy.root_retreat.stage",
				this.menu.playerStage());
		graphics.drawString(font, yourStage, left + 4, top + RootRetreatMenu.YOUR_STAGE_Y, STAGE_COLOR, false);

		graphics.enableScissor(left, top + RootRetreatMenu.YOUR_ROWS_TOP - 2, right, listBottom);
		DifficultyOption[] options = DifficultyOption.values();

		for (int index = 0; index < options.length; index++) {
			int row = index - this.scrollRow;

			if (row < 0 || row >= RootRetreatMenu.VISIBLE_ROWS) {
				continue;
			}

			int rowY = top + RootRetreatMenu.YOUR_ROWS_TOP + row * RootRetreatMenu.YOUR_ROW_HEIGHT;
			Component line = Component.translatable("screen.sephiria_fantasy.root_retreat.plan_line",
					options[index].label(), options[index].stageLine(this.menu.playerLevelOf(options[index])));
			graphics.drawString(font, line, left + 4, rowY, TEXT_COLOR, false);
		}

		graphics.disableScissor();
	}

	/** 右上框里的「祝福 力量 轻盈 勇气」：名字按激活阶段上色（未激活压暗）。 */
	private void renderBlessings(GuiGraphics graphics, int left, int top) {
		Font font = this.font;
		int y = top + RootRetreatMenu.BLESSING_ROW_Y;
		Component label = Component.translatable("screen.sephiria_fantasy.root_retreat.blessings");
		graphics.drawString(font, label, left + 4, y, HEADER_COLOR, false);
		int x = left + 4 + font.width(label) + 4;
		int stage = this.menu.blockStage();

		for (int index = 0; index < 3; index++) {
			Component name = blessingName(index);
			int color = stage >= blessingStage(index) ? blessingColor(index, stage) : blessingDimColor(index);
			graphics.drawString(font, name, x, y, color, false);
			x += font.width(name) + 4;
		}
	}

	private static Component blessingName(int index) {
		return Component.translatable(switch (index) {
			case 0 -> "screen.sephiria_fantasy.root_retreat.blessing.lightness";
			case 1 -> "screen.sephiria_fantasy.root_retreat.blessing.strength";
			default -> "screen.sephiria_fantasy.root_retreat.blessing.courage";
		});
	}

	private static int blessingStage(int index) {
		return switch (index) {
			case 0 -> RootRetreat.LIGHTNESS_STAGE;
			case 1 -> RootRetreat.STRENGTH_STAGE;
			default -> RootRetreat.COURAGE_STAGE;
		};
	}

	/** 勇气在 40 阶段显示「你的勇气值得赞赏」，50 阶段换成「你强的可怕！」。 */
	private static String blessingDescKey(int index, int stage) {
		return switch (index) {
			case 0 -> "screen.sephiria_fantasy.root_retreat.blessing.lightness.desc";
			case 1 -> "screen.sephiria_fantasy.root_retreat.blessing.strength.desc";
			default -> stage >= RootRetreat.COURAGE_RED_STAGE
					? "screen.sephiria_fantasy.root_retreat.blessing.courage.desc50"
					: "screen.sephiria_fantasy.root_retreat.blessing.courage.desc";
		};
	}

	private static int blessingColor(int index, int stage) {
		if (index != 2) {
			return BLESSING_COLOR;
		}

		return stage >= RootRetreat.COURAGE_RED_STAGE
				? RootRetreat.COURAGE_RED_COLOR
				: RootRetreat.COURAGE_GOLD_COLOR;
	}

	private static int blessingDimColor(int index) {
		return index == 2 ? BLESSING_DIM_YELLOW : BLESSING_DIM_GREEN;
	}

	private int blessingRowX() {
		return this.leftPos + RootRetreatMenu.RIGHT_X + 4;
	}

	private int blessingRowY() {
		return this.topPos + RootRetreatMenu.BLESSING_ROW_Y;
	}

	private int blessingNameX(int index) {
		int x = blessingRowX()
				+ this.font.width(Component.translatable("screen.sephiria_fantasy.root_retreat.blessings")) + 4;

		for (int i = 0; i < index; i++) {
			x += this.font.width(blessingName(i)) + 4;
		}

		return x;
	}

	/** 鼠标下的祝福（0 轻盈 / 1 力量 / 2 勇气），不在那一行返回 -1。 */
	private int hoveredBlessing(int mouseX, int mouseY) {
		int y = blessingRowY();

		if (mouseY < y || mouseY >= y + 10) {
			return -1;
		}

		for (int index = 0; index < 3; index++) {
			int x = blessingNameX(index);

			if (mouseX >= x && mouseX < x + this.font.width(blessingName(index))) {
				return index;
			}
		}

		return -1;
	}

	/** 鼠标下的那一项；不在左列（或那一行被滚出去）时返回 null。 */
	private DifficultyOption hoveredOption(int mouseX, int mouseY) {
		if (mouseX < this.leftPos + RootRetreatMenu.ROW_TEXT_X
				|| mouseX >= this.leftPos + RootRetreatMenu.MINUS_BUTTON_X - 2) {
			return null;
		}

		int y = mouseY - (this.topPos + RootRetreatMenu.LIST_TOP);

		if (y < 0 || y >= RootRetreatMenu.LIST_HEIGHT) {
			return null;
		}

		int index = this.scrollRow + y / RootRetreatMenu.ROW_HEIGHT;
		DifficultyOption[] options = DifficultyOption.values();
		return index >= 0 && index < options.length ? options[index] : null;
	}

	/**
	 * 左上角画方块名字。
	 *
	 * <p>1.20.1 与 26.x 一样：{@code renderLabels} 的坐标是「面板内坐标」（pose 已被平移），
	 * 所以这里不加 leftPos/topPos。原版标题是水平居中的，用户要求放到左上角，
	 * 所以自己画、不调 super（顺便也不画「物品栏」标签——这个界面没有物品栏）。
	 */
	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, RootRetreatMenu.PAD, 8, TEXT_COLOR, false);
	}

	/** 悬停浮窗：难度选项那一行，或者右上框里的祝福名字。 */
	@Override
	public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);
		int blessing = hoveredBlessing(mouseX, mouseY);

		if (blessing >= 0) {
			List<Component> lines = new ArrayList<>();
			lines.add(Component.translatable("screen.sephiria_fantasy.root_retreat.blessing.activate",
					blessingStage(blessing)).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(STAGE_COLOR))));
			String desc = blessingDescKey(blessing, this.menu.blockStage());

			if (desc != null) {
				lines.add(Component.translatable(desc).withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(HEADER_COLOR))));
			}

			// 祝福名字在面板最右侧，浮窗放右边会顶出屏幕——改到鼠标左侧
			drawTooltip(graphics, mouseX, mouseY, lines, true);
			return;
		}

		DifficultyOption hovered = hoveredOption(mouseX, mouseY);

		if (hovered != null) {
			drawTooltip(graphics, mouseX, mouseY, hovered.tooltip(this.menu.blockLevelOf(hovered)), false);
		}
	}

	/** 统一的浮窗画法（难度选项在右侧、祝福在左侧，四周夹进屏幕内）。 */
	private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, List<Component> lines,
			boolean toLeft) {
		int width = 0;

		for (Component line : lines) {
			width = Math.max(width, this.font.width(line));
		}

		int height = lines.size() * 10 + 6;
		int x = toLeft ? mouseX - width - 8 : mouseX + 8;
		x = Mth.clamp(x, 4, Math.max(4, this.width - width - 4));
		int y = Mth.clamp(mouseY + 8, 4, Math.max(4, this.height - height - 4));
		graphics.fill(x - 3, y - 3, x + width + 3, y + height - 3, 0xF0100010);
		graphics.fill(x - 3, y - 3, x + width + 3, y - 2, 0xFF5050A0);

		int lineY = y;

		for (Component line : lines) {
			graphics.drawString(this.font, line, x, lineY, TEXT_COLOR, false);
			lineY += 10;
		}
	}

	private static void send(RootRetreatClickPayload payload) {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		RootRetreatClickPayload.write(buffer, payload);
		ClientPlayNetworking.send(RootRetreatClickPayload.CHANNEL, buffer);
	}
}
