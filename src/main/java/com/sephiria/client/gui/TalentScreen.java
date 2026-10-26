package com.sephiria.client.gui;

import com.sephiria.Sephiria;
import com.sephiria.client.ClientTalents;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import com.sephiria.network.TalentClickPayload;
import com.sephiria.network.TalentSyncRequestPayload;
import com.sephiria.talent.Talent;
import com.sephiria.talent.Talents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 天赋页：一行一条天赋，最左是「名字：已加点数」与「+x% 属性」，中间三个阶段图标，
 * 最右是 -5 / -1 / +1 / +5，底下是「重置」与「剩余天赋点数」。
 *
 * <p>所有数字读 {@link ClientTalents}（服务端同步下来的），按钮只发包；加减范围、剩余点数、
 * 阶段是否点亮都由服务端判定并立刻重推，所以界面永远显示服务端的真实状态。
 *
 * <p>绘制顺序：先画面板底与文字，再调 {@code super.extractRenderState} 让按钮画在上面
 * （26.3 的界面是「收集绘制指令」的模型，调用顺序就是图层顺序）。
 */
public class TalentScreen extends Screen {
	private static final int TAB_Y = 14;
	private static final int TITLE_Y = 38;
	private static final int SUBTITLE_Y = 52;
	private static final int FIRST_ROW_Y = 68;
	private static final int ROW_HEIGHT = 58;
	private static final int ROW_GAP = 6;

	/** 跟属性页一样宽（420），两列属性的那个面板刚好铺满。 */
	private static final int PANEL_WIDTH = 420;
	private static final int ICON_SIZE = 22;
	private static final int ICON_GAP = 10;
	/** 三个阶段图标在行内的 x 偏移（相对面板左边）。 */
	private static final int ICON_OFFSET = 180;
	private static final int BUTTON_WIDTH = 26;
	private static final int BUTTON_HEIGHT = 18;
	private static final int BUTTON_GAP = 4;
	/** 四个按钮从面板右边往里缩多少。 */
	private static final int BUTTON_RIGHT_MARGIN = 14;
	/** 屏幕底部留给「重置 / 剩余点数」这一行的高度。 */
	private static final int BOTTOM_MARGIN = 30;
	/** 右侧滚动进度条：宽度、面板右外侧的偏移、轨道对齐的行数，以及轨道 / 滑块颜色。 */
	private static final int SCROLLBAR_WIDTH = 3;
	private static final int SCROLLBAR_OFFSET_X = 4;
	private static final int SCROLLBAR_ROWS = 2;
	private static final int SCROLLBAR_TRACK_COLOUR = 0x30FFFFFF;
	private static final int SCROLLBAR_THUMB_COLOUR = 0xFF9A9AA8;

	/** 「天赋」标题色。 */
	private static final int TITLE_COLOUR = 0xFFFFE29B;
	/** 说明文字灰色。 */
	private static final int SUBTITLE_COLOUR = 0xFF9A9A9A;
	/** 阶段描述与「+x%」的绿色（与本模组其它绿色数值一致）。 */
	private static final int GREEN = 0xFF55FF55;
	/** 剩余天赋点数的颜色。 */
	private static final int REMAINING_COLOUR = 0xFFFFDF7E;
	/** 行底板的颜色（天赋图标自带的 #161319 加一点透明度）。 */
	private static final int ROW_BACKGROUND = 0xC0161319;

	private int panelLeft;
	private int panelRight;
	private final Map<Talent, Button[]> adjustButtons = new EnumMap<>(Talent.class);
	private Button resetButton;
	private int resetButtonY;
	/** 天赋页滚动到第几行（0 = 最上面）。 */
	private int scrollRow;
	/** 当前是否在出生点 100 格内（不在时所有按钮置灰，悬停出红字）。 */
	private boolean editingAllowed;

	public TalentScreen() {
		super(Component.translatable("screen.sephiria_fantasy.talent"));
	}

	/**
	 * 按「背包键」（默认 E）关掉天赋页、回到游戏——与属性页、神器技能页一致。
	 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (Minecraft.getInstance().options.keyInventory.matches(event)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	protected void init() {
		// 打开界面时向服务端要一次最新状态：个人出生点（床 / 重生锚 / 指令）只在服务端
		ClientPlayNetworking.send(TalentSyncRequestPayload.INSTANCE);
		// 标签栏按整排宽度居中（六个标签后固定偏移会让它偏左）
		SephiriaTabs.add(this, (this.width - SephiriaTabs.width()) / 2, TAB_Y, SephiriaTab.TALENT);

		this.panelLeft = this.width / 2 - PANEL_WIDTH / 2;
		this.panelRight = this.panelLeft + PANEL_WIDTH;
		this.adjustButtons.clear();
		this.scrollRow = Mth.clamp(this.scrollRow, 0, maxScrollRow());

		Talent[] talents = Talent.values();

		for (int row = 0; row < talents.length; row++) {
			Talent talent = talents[row];
			Button[] buttons = new Button[4];

			// 顺序固定：-5 / -1 / +1 / +5（界面上从左到右）；位置由 layoutRows() 按滚动统一摆
			int[] steps = { -5, -1, 1, 5 };

			for (int index = 0; index < steps.length; index++) {
				int delta = steps[index];
				buttons[index] = Button.builder(Component.literal((delta > 0 ? "+" : "") + delta),
						button -> adjust(talent, delta))
						.bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT)
						.build();
				addRenderableWidget(buttons[index]);
			}

			this.adjustButtons.put(talent, buttons);
		}

		// 「重置」固定在屏幕底部（不随天赋页滚动）
		this.resetButtonY = listBottom() + 4;
		this.resetButton = Button.builder(Component.translatable("screen.sephiria_fantasy.talent.reset"),
				button -> {
					if (this.editingAllowed) {
						ClientPlayNetworking.send(TalentClickPayload.resetAll());
					}
				})
				.bounds(this.panelLeft + 14, this.resetButtonY, 48, 18)
				.build();
		addRenderableWidget(this.resetButton);

		layoutRows();
	}

	private static int rowTop(int row) {
		return FIRST_ROW_Y + row * (ROW_HEIGHT + ROW_GAP);
	}

	/** 天赋页列表的下边界（给底部「重置 / 剩余点数」留 BOTTOM_MARGIN）。 */
	private int listBottom() {
		return this.height - BOTTOM_MARGIN;
	}

	/** 一屏能完整放几行（至少 1 行）。 */
	private int visibleRows() {
		int available = Math.max(0, listBottom() - FIRST_ROW_Y);
		int fit = (available + ROW_GAP) / (ROW_HEIGHT + ROW_GAP);
		return Math.max(1, Math.min(Talent.values().length, fit));
	}

	private int maxScrollRow() {
		return Math.max(0, Talent.values().length - visibleRows());
	}

	/**
	 * 按当前滚动位置摆放按钮：只显示可视范围内的行。
	 *
	 * <p>整行滚动（不滚半行），所以图标/文字与按钮不会互相错位；隐藏行的按钮
	 * {@code visible = false}，既不会画出来也收不到点击。
	 */
	private void layoutRows() {
		Talent[] talents = Talent.values();
		int visible = visibleRows();
		int buttonsX = this.panelRight - BUTTON_RIGHT_MARGIN - (4 * BUTTON_WIDTH + 3 * BUTTON_GAP);
		this.scrollRow = Mth.clamp(this.scrollRow, 0, maxScrollRow());

		for (int row = 0; row < talents.length; row++) {
			Button[] buttons = this.adjustButtons.get(talents[row]);

			if (buttons == null) {
				continue;
			}

			boolean shown = row >= this.scrollRow && row < this.scrollRow + visible;
			int top = rowTop(row - this.scrollRow);
			int buttonY = top + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;

			for (int index = 0; index < buttons.length; index++) {
				buttons[index].visible = shown;
				buttons[index].setX(buttonsX + index * (BUTTON_WIDTH + BUTTON_GAP));
				buttons[index].setY(buttonY);
			}
		}
	}

	/** 鼠标在天赋页列表区域（不是顶部标题、也不是底部按钮行）。 */
	private boolean isOverRows(double mouseX, double mouseY) {
		return mouseX >= this.panelLeft && mouseX < this.panelRight
				&& mouseY >= FIRST_ROW_Y && mouseY < listBottom();
	}

	/** 滚轮：一次滚一行（整行吸附），只改客户端显示，不涉及服务端点数。 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0D && maxScrollRow() > 0 && isOverRows(mouseX, mouseY)) {
			this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
			layoutRows();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void adjust(Talent talent, int delta) {
		if (!this.editingAllowed) {
			return;
		}

		ClientPlayNetworking.send(TalentClickPayload.adjust(talent.index(), delta));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
		// 修改门槛：出生点 100 格内（用户 2026-10-21）。每帧现算，走开了立刻置灰。
		this.editingAllowed = canEditHere();
		// 先画底与文字，按钮在 super 里画（会盖在底板上，不会被底板的半透明色压暗）
		drawHeader(extractor);
		Talent[] talents = Talent.values();
		int visible = visibleRows();

		for (int row = 0; row < talents.length; row++) {
			// 只画可视范围内的整行（滚动是整行吸附；行外的按钮在 layoutRows 里隐藏）
			if (row < this.scrollRow || row >= this.scrollRow + visible) {
				continue;
			}

			drawRow(extractor, talents[row], row - this.scrollRow, mouseX, mouseY);
		}

		drawFooter(extractor);
		drawScrollbar(extractor);

		super.extractRenderState(extractor, mouseX, mouseY, partialTick);
	}

	/**
	 * 玩家是否在出生点 100 格内（与服务端 {@link Talents#nearSpawn} 同一口径）。
	 *
	 * <p>客户端拿不到玩家自己设置的床 / 重生锚，直接用服务端在同步包里算好的
	 * {@link ClientTalents#nearSpawn()}；打开界面时会主动请求一次，所以在哪都准。
	 */
	private boolean canEditHere() {
		return ClientTalents.nearSpawn();
	}

	private void drawHeader(GuiGraphicsExtractor extractor) {
		Component title = Component.translatable("screen.sephiria_fantasy.talent");
		extractor.text(this.font, title, (this.width - this.font.width(title)) / 2, TITLE_Y, TITLE_COLOUR);

		Component subtitle = Component.translatable("screen.sephiria_fantasy.talent.subtitle");
		extractor.text(this.font, subtitle, (this.width - this.font.width(subtitle)) / 2, SUBTITLE_Y,
				SUBTITLE_COLOUR);
	}

	private void drawRow(GuiGraphicsExtractor extractor, Talent talent, int row, int mouseX, int mouseY) {
		int top = rowTop(row);
		int colour = 0xFF000000 | talent.colour();

		// 行底板 + 左边一条天赋色的竖条，一眼看出这一行属于哪一种天赋
		extractor.fill(this.panelLeft, top, this.panelRight, top + ROW_HEIGHT, ROW_BACKGROUND);
		extractor.fill(this.panelLeft, top, this.panelLeft + 2, top + ROW_HEIGHT, colour);
		extractor.fill(this.panelLeft, top, this.panelRight, top + 1, 0xFF2B2B33);
		extractor.fill(this.panelLeft, top + ROW_HEIGHT - 1, this.panelRight, top + ROW_HEIGHT, 0xFF2B2B33);

		int points = ClientTalents.points(talent);

		// 「愤怒：x」——名字保持白字，只有 x 用天赋色（用户指定的口径）
		Component name = Component.translatable("screen.sephiria_fantasy.talent." + talent.id());
		Component pointsText = Component.literal(String.valueOf(points)).withColor(talent.colour());
		extractor.text(this.font,
				Component.translatable("screen.sephiria_fantasy.talent.points", name, pointsText),
				this.panelLeft + 14, top + 8, 0xFFFFFFFF);

		// 「+x% 暴击几率」/「+x% 攻击速度」/「+x 幸运值」/「+0.3x 最大HP、+0.5x 闪避」
		// ——只有数值部分是绿的，属性名保持白字；急智有两行
		List<Component> statLines = statLines(talent, points);

		for (int line = 0; line < statLines.size(); line++) {
			extractor.text(this.font, statLines.get(line), this.panelLeft + 14, top + 26 + line * 10, 0xFFFFFFFF);
		}

		// 中间三个阶段图标：达到门槛就换点亮版（天赋色），否则用灰版
		for (int stage = 0; stage < Talents.STAGE_POINTS.length; stage++) {
			int iconX = this.panelLeft + ICON_OFFSET + stage * (ICON_SIZE + ICON_GAP);
			int iconY = top + (ROW_HEIGHT - ICON_SIZE) / 2;
			boolean lit = points >= Talents.STAGE_POINTS[stage];
			Identifier texture = icon(talent, stage, lit);
			extractor.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
					ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

			if (inside(iconX, iconY, mouseX, mouseY)) {
				extractor.setComponentTooltipForNextFrame(this.font, stageTooltip(talent, stage, points), mouseX, mouseY);
			}
		}

		updateButtons(talent, points);
	}

	private void drawFooter(GuiGraphicsExtractor extractor) {
		// 剩余点数放在最右边，数字用 #FFDF7E
		Component remaining = Component.translatable("screen.sephiria_fantasy.talent.remaining",
				Component.literal(String.valueOf(ClientTalents.remaining())).withColor(REMAINING_COLOUR));
		extractor.text(this.font, remaining, this.panelRight - this.font.width(remaining), this.resetButtonY + 5,
				0xFFFFFFFF);

		// 不在出生点附近：按钮已置灰，这句红字固定显示在界面下方中间（用户 2026-10-21）
		if (!this.editingAllowed) {
			Component message = Component.translatable("screen.sephiria_fantasy.talent.not_near_spawn")
					.withColor(0xFFFF5555);
			extractor.text(this.font, message,
					this.panelLeft + (PANEL_WIDTH - this.font.width(message)) / 2,
					this.resetButtonY + 5, 0xFFFF5555);
		}
	}

	/**
	 * 右侧滚动进度条：只有一屏装不下所有行时才画；滑块长度按「可视行数 / 总行数」算，
	 * 位置按 {@link #scrollRow} 在 {@code 0..maxScrollRow} 里的比例走。
	 *
	 * <p>轨道长度固定对齐**两行**天赋页（用户 2026-10-21：「太长、对两页、别盖住天赋页」），
	 * 放在面板右外侧 {@value #SCROLLBAR_OFFSET_X}px，不再压在行底板上。
	 */
	private void drawScrollbar(GuiGraphicsExtractor extractor) {
		int maxScroll = maxScrollRow();

		if (maxScroll <= 0) {
			return;
		}

		int x = this.panelRight + SCROLLBAR_OFFSET_X;
		int top = FIRST_ROW_Y;
		int rowSpan = SCROLLBAR_ROWS * ROW_HEIGHT + (SCROLLBAR_ROWS - 1) * ROW_GAP;
		int track = Math.min(rowSpan, listBottom() - top);

		if (track <= 0) {
			return;
		}

		int thumb = Math.max(SCROLLBAR_WIDTH * 4, track * visibleRows() / Talent.values().length);
		int thumbY = top + (track - thumb) * this.scrollRow / maxScroll;
		extractor.fill(x, top, x + SCROLLBAR_WIDTH, top + track, SCROLLBAR_TRACK_COLOUR);
		extractor.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, SCROLLBAR_THUMB_COLOUR);
	}

	/**
	 * 按钮的可用状态跟着同步下来的点数走：没点可退时减号置灰、没点可加或已到上限时加号置灰、
	 * 两点都没花时「重置」置灰。每帧改一次，点完立刻同步回来就生效。
	 */
	private void updateButtons(Talent talent, int points) {
		Button[] buttons = this.adjustButtons.get(talent);

		if (buttons == null) {
			return;
		}

		int remaining = ClientTalents.remaining();
		boolean canRemove = this.editingAllowed && points > 0;
		boolean canAdd = this.editingAllowed && remaining > 0 && points < Talents.MAX_POINTS;
		buttons[0].active = canRemove;
		buttons[1].active = canRemove;
		buttons[2].active = canAdd;
		buttons[3].active = canAdd;

		if (this.resetButton != null) {
			this.resetButton.active = this.editingAllowed && ClientTalents.spent() > 0;
		}
	}

	private static boolean inside(int x, int y, int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
	}

	/** 阶段图标：未点亮用灰版，点亮后换成染了天赋色的版本（切图脚本生成两套）。 */
	private static Identifier icon(Talent talent, int stage, boolean lit) {
		String name = "textures/gui/talent/" + talent.id() + "_stage" + (stage + 1) + (lit ? "_lit" : "") + ".png";
		return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name);
	}

	/**
	 * 阶段图标的悬停描述：效果文字一律绿色；还没到门槛时再补一行灰色的「需要 x 点」。
	 */
	private static List<Component> stageTooltip(Talent talent, int stage, int points) {
		List<Component> lines = new ArrayList<>();

		switch (talent) {
			case RAGE -> {
				switch (stage) {
					case 0 -> lines.add(greenLine("screen.sephiria_fantasy.talent.rage.stage1"));
					case 1 -> lines.add(greenLine("screen.sephiria_fantasy.talent.rage.stage2.boss"));
					default -> lines.add(greenLine("screen.sephiria_fantasy.talent.rage.stage3"));
				}
			}
			case SWIFT -> {
				switch (stage) {
					case 0 -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage1.dodge"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage1.dash"));
					}
					case 1 -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage2.regen"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage2.weapon"));
					}
					default -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage3.invuln"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.swift.stage3.weapon"));
					}
				}
			}
			case WILL -> {
				switch (stage) {
					case 0 -> lines.add(greenLine("screen.sephiria_fantasy.talent.will.stage1"));
					case 1 -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.will.stage2.hp"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.will.stage2.mp"));
					}
					default -> lines.add(greenLine("screen.sephiria_fantasy.talent.will.stage3"));
				}
			}
			case QUICK_WIT -> {
				switch (stage) {
					case 0 -> lines.add(greenLine("screen.sephiria_fantasy.talent.quick_wit.stage1"));
					case 1 -> lines.add(greenLine("screen.sephiria_fantasy.talent.quick_wit.stage2"));
					default -> lines.add(greenLine("screen.sephiria_fantasy.talent.quick_wit.stage3"));
				}
			}
			case SURVIVAL -> {
				switch (stage) {
					case 0 -> lines.add(greenLine("screen.sephiria_fantasy.talent.survival.stage1"));
					case 1 -> lines.add(greenLine("screen.sephiria_fantasy.talent.survival.stage2"));
					default -> lines.add(greenLine("screen.sephiria_fantasy.talent.survival.stage3.imprint"));
				}
			}
			case PATIENCE -> {
				switch (stage) {
					case 0 -> lines.add(greenLine("screen.sephiria_fantasy.talent.patience.stage1"));
					case 1 -> lines.add(greenLine("screen.sephiria_fantasy.talent.patience.stage2"));
					default -> lines.add(greenLine("screen.sephiria_fantasy.talent.patience.stage3"));
				}
			}
			case WISDOM -> {
				switch (stage) {
					case 0 -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage1.absorb"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage1.mp"));
					}
					case 1 -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage2.merge"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage2.dice"));
					}
					default -> {
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage3.reroll"));
						lines.add(greenLine("screen.sephiria_fantasy.talent.wisdom.stage3.xp"));
					}
				}
			}
		}

		int threshold = Talents.STAGE_POINTS[stage];

		if (points < threshold) {
			lines.add(Component.translatable("screen.sephiria_fantasy.talent.requires", threshold)
					.withColor(0xFF777777));
		}

		return lines;
	}

	private static Component greenLine(String key) {
		return Component.translatable(key).withColor(GREEN);
	}

	/** 左侧属性行的数值部分统一绿色，属性名保持白字。 */
	private static Component percentLine(double value, String statKey) {
		return Component.empty()
				.append(Component.literal("+" + formatPercent(value) + "%").withColor(GREEN))
				.append(Component.literal(" "))
				.append(Component.translatable("screen.sephiria_fantasy.talent.stat." + statKey));
	}

	private static Component valueLine(String value, String statKey) {
		return Component.empty()
				.append(Component.literal("+" + value).withColor(GREEN))
				.append(Component.literal(" "))
				.append(Component.translatable("screen.sephiria_fantasy.talent.stat." + statKey));
	}

	/** 每条天赋页左边的属性行（急智有最大 HP 与闪避两行）。 */
	private static List<Component> statLines(Talent talent, int points) {
		List<Component> lines = new ArrayList<>();

		switch (talent) {
			case RAGE -> lines.add(percentLine(points * Talents.RAGE_CRIT_CHANCE_PER_POINT, "crit_chance"));
			case SWIFT -> lines.add(percentLine(points * Talents.SWIFT_ATTACK_SPEED_PER_POINT, "attack_speed"));
			case WILL -> lines.add(valueLine(String.valueOf(points), "luck"));
			case QUICK_WIT -> {
				lines.add(valueLine(formatOne(points * Talents.QUICK_WIT_MAX_HP_PER_POINT), "max_hp"));
				lines.add(valueLine(formatOne(points * Talents.QUICK_WIT_DODGE_PER_POINT), "dodge"));
			}
			case SURVIVAL -> lines.add(valueLine(formatOne(points * Talents.SURVIVAL_MAX_HP_PER_POINT), "max_hp"));
			case PATIENCE -> lines.add(valueLine(String.valueOf(points), "defense"));
			case WISDOM -> lines.add(valueLine(String.valueOf(points), "mp_regen"));
		}

		return lines;
	}

	/** 属性行按整数显示（暴击/攻速每点都是 1%），以后改成小数再调。 */
	private static String formatPercent(double value) {
		return String.valueOf(Math.round(value));
	}

	/** 最大 HP / 闪避这种可能带小数的数值保留 1 位（急智每点 0.3 / 0.5）。 */
	private static String formatOne(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
