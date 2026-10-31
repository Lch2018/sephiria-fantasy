package com.sephiria.client.gui;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.client.ClientStats;
import com.sephiria.client.tabs.ScreenTabs;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.stats.PlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 属性面板：与 26.3 的属性页同一套排版——上边第一行是叶子（带图标），
 * 下面左右两列铺开全部属性。
 *
 * <p>1.21.1 没有 26.3 的 {@code GuiGraphicsExtractor}，这里直接用
 * {@link GuiGraphics#drawString} 画文字、{@link GuiGraphics#blit} 画图标；
 * 悬停算式与 26.3 一样在行下面补一行。
 *
 * <p>26.3 的右列最后一行在 GUI 高度很小时会顶到屏幕底边，这里补了「装不下才出现」的
 * 滚动（一次一行），正常窗口尺寸下仍是最上面、与 26.3 完全一致。
 */
public class StatsScreen extends Screen {
	private static final int ROW_HEIGHT = 14;
	/** 叶子（货币）的图标。 */
	private static final ResourceLocation LEAF_ICON =
			ResourceLocation.fromNamespaceAndPath(Sephiria.MOD_ID, "textures/gui/leaf.png");

	/** 两列的 x：屏幕中心左右各 210（列宽 200、列间 10）。 */
	private static final int COLUMN_OFFSET = 210;
	private static final int COLUMN_WIDTH = 200;
	/** 第一行（叶子）的 y，与 26.3 的 {@code y = 34} 一致。 */
	private static final int CONTENT_TOP = 34;
	/** 底部留白：最后一行不贴着屏幕底边。 */
	private static final int BOTTOM_MARGIN = 8;
	/** 左列 12 行、右列 14 行，整体按 15 行算高度（右列第一行从第 1 行开始）。 */
	private static final int CONTENT_ROWS = 15;
	private static final int CONTENT_HEIGHT = CONTENT_ROWS * ROW_HEIGHT;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int HOVER_TEXT = 0xFFDDDDDD;
	/** 叶子数值的黄（与 26.3 一致）。 */
	private static final int LEAF_YELLOW = 0xFFFFD24A;
	/** 换算结果的绿（防御减伤 / 幸运加成）。 */
	private static final int BONUS_GREEN = 0xFF55FF55;
	/** 药水来源的红（算式里）。 */
	private static final int POTION_RED = 0xFFFF5555;
	/** 滚动条颜色（与天赋页同一套）。 */
	private static final int SCROLLBAR_WIDTH = 3;
	private static final int SCROLLBAR_TRACK_COLOUR = 0x30FFFFFF;
	private static final int SCROLLBAR_THUMB_COLOUR = 0xFF9A9AA8;

	/** 「物理强度」那一行的位置：鼠标悬停时在它下面显示算式。 */
	private int physicalRowY;
	private int ampRowY;
	private int attackSpeedRowY;
	/** 「闪避」那一行的位置：悬停时显示闪避率公式。 */
	private int dodgeRowY;
	/** 「谈判力」那一行的位置：悬停时显示换算出的商店折扣（折扣不再单独占一行）。 */
	private int negotiationRowY;
	/** 「幸运值」那一行的位置：悬停时显示换算出的「稀有及以上 +y%」。 */
	private int luckRowY;
	/** 「防御力」那一行的位置：悬停时显示换算出的「减伤 x%」。 */
	private int defenseRowY;
	/** 叶子那一行：图标 + 数值，画在最上面。 */
	private int leafRowY;
	private int rowsLeft;
	private int rowsRight;
	/** 滚动偏移（像素）：只在内容装不下时非零。 */
	private int scrollOffset;

	public StatsScreen() {
		super(Component.translatable("screen.sephiria_fantasy.stats"));
	}

	/**
	 * 顶部标签栏：六页里高亮「属性」这一格，其余页点了直接切过去。
	 * 标签放在 y=6~26，内容从 {@code CONTENT_TOP}=34 起，不会压到属性行。
	 */
	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(graphics);
	}

	@Override
	protected void init() {
		super.init();
		ScreenTabs.add(this, SephiriaTab.ATTRIBUTES);
	}

	/**
	 * 按「背包键」（默认 E）关掉属性页、回到游戏——与天赋页、神器技能页一致。
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
			this.onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);

		this.rowsLeft = this.width / 2 - COLUMN_OFFSET;
		this.rowsRight = this.rowRightX();
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());

		drawLeaf(graphics);
		drawLeftColumn(graphics);
		drawRightColumn(graphics);
		drawHover(graphics, mouseX, mouseY);
		drawScrollbar(graphics);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	/** 滚轮：一次滚一行（整行吸附），只改客户端显示。 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0D && maxScroll() > 0) {
			this.scrollOffset = Mth.clamp(
					this.scrollOffset - (int) Math.signum(scrollY) * ROW_HEIGHT, 0, maxScroll());
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void drawLeaf(GuiGraphics graphics) {
		this.leafRowY = rowTop(0);

		if (!visible(this.leafRowY)) {
			return;
		}

		// 叶子：图标在文字左边，数值黄色
		graphics.blit(LEAF_ICON, this.rowsLeft, this.leafRowY, 0.0F, 0.0F, 12, 12, 16, 16);
		graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.stats.leaves",
						Component.literal(format(ClientStats.leaves())).withColor(LEAF_YELLOW)),
				this.rowsLeft + 15, this.leafRowY + 2, WHITE, false);
	}

	private void drawLeftColumn(GuiGraphics graphics) {
		int left = this.rowsLeft;
		var player = Minecraft.getInstance().player;
		String health = player == null
				? "-"
				: format(player.getHealth()) + " / " + format(player.getMaxHealth());
		int row = 1;
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.hp", health);
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.mp",
				format(ClientStats.mp()) + " / " + format(ClientStats.maxMp()));
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.mp_regen", format(ClientStats.mpRegen()));
		this.physicalRowY = rowTop(row);
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.physical", format(ClientStats.physical()));
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.fire", format(ClientStats.fire()));
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.ice", format(ClientStats.ice()));
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.lightning", format(ClientStats.lightning()));
		this.defenseRowY = rowTop(row);
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.defense", format(ClientStats.defense()));
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.mp_absorb", format(ClientStats.mpAbsorb()));
		this.ampRowY = rowTop(row);
		row(graphics, left, row++, "screen.sephiria_fantasy.stats.physical_amp",
				format(ClientStats.physicalAmp()) + "%");
		this.attackSpeedRowY = rowTop(row);
		row(graphics, left, row, "screen.sephiria_fantasy.stats.attack_speed",
				format(ClientStats.attackSpeed()) + "%");
	}

	private void drawRightColumn(GuiGraphics graphics) {
		int right = this.rowsRight;
		int row = 1;
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.melee_range",
				format(ClientStats.meleeRange()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.weapon_damage",
				format(ClientStats.weaponDamage()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.special_attack",
				format(ClientStats.specialAttack()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.normal_attack_damage",
				format(ClientStats.normalAttackDamage()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.crit_chance",
				format(ClientStats.critChance()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.crit_damage",
				format(ClientStats.critDamage()) + "%");
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.ignore_defense",
				format(ClientStats.ignoreDefense()));
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.move_speed",
				format(ClientStats.moveSpeed()) + "%");
		this.dodgeRowY = rowTop(row);
		// 行里显示闪避点数（与词条/其它来源的口径一致），闪避率放在悬停算式里
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.dodge", format(ClientStats.dodge()));
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.lifesteal", format(ClientStats.lifesteal()));
		this.negotiationRowY = rowTop(row);
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.negotiation",
				format(ClientStats.negotiation()));
		this.luckRowY = rowTop(row);
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.luck", format(ClientStats.luck()));
		row(graphics, right, row++, "screen.sephiria_fantasy.stats.leaf_gain",
				format(ClientStats.leafGainPercent()) + "%");
		row(graphics, right, row, "screen.sephiria_fantasy.stats.xp_drop",
				format(ClientStats.xpDropPercent()) + "%");
	}

	/**
	 * 鼠标放在「物理强度」「攻击速度」「防御力」或「闪避」「谈判力」「幸运值」上时，
	 * 在那一行下面补一行换算结果 / 算式。
	 *
	 * <p>行按列区分：先看是不是左列的行，再看是不是右列的行，最后才按鼠标 x
	 * 判断落在哪一列——谈判力在右列，用左列的 x 范围判断会永远判不中。
	 */
	private void drawHover(GuiGraphics graphics, int mouseX, int mouseY) {
		boolean leftRow = inRow(this.physicalRowY, mouseY) || inRow(this.attackSpeedRowY, mouseY)
				|| inRow(this.defenseRowY, mouseY);
		boolean rightRow = inRow(this.dodgeRowY, mouseY) || inRow(this.negotiationRowY, mouseY)
				|| inRow(this.luckRowY, mouseY);

		if (!leftRow && !rightRow) {
			return;
		}

		int columnX = leftRow ? this.rowsLeft : this.rowsRight;

		if (mouseX < columnX || mouseX >= columnX + COLUMN_WIDTH) {
			return;
		}

		if (inRow(this.physicalRowY, mouseY)) {
			drawBreakdown(graphics, this.physicalRowY, ClientStats.physicalBreakdown());
		} else if (inRow(this.attackSpeedRowY, mouseY)) {
			drawBreakdown(graphics, this.attackSpeedRowY, ClientStats.attackSpeedBreakdown());
		} else if (inRow(this.defenseRowY, mouseY)) {
			// 防御力 x 的减伤 = x / (x + 80)（防御力 80 → 50%）
			double reduction = PlayerStats.defenseReduction(ClientStats.defense()) * 100.0D;
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.stats.defense_reduction",
							Component.literal(formatPercent(reduction) + "%").withColor(BONUS_GREEN)),
					this.rowsLeft, this.defenseRowY + ROW_HEIGHT - 4, HOVER_TEXT, false);
		} else if (inRow(this.dodgeRowY, mouseY)) {
			// 闪避率 = 0.8 × (1 − e^(−闪避 / 43.28))：把点数与结果一起写出来
			graphics.drawString(this.font, Component.translatable("screen.sephiria_fantasy.stats.dodge_formula",
							format(ClientStats.dodgeRate()), format(ClientStats.dodge())),
					this.rowsRight, this.dodgeRowY + ROW_HEIGHT - 4, HOVER_TEXT, false);
		} else if (inRow(this.negotiationRowY, mouseY)) {
			// 商店折扣 = 70×(1−e^(−谈判力/27.94))：数值与叶子一样用黄色
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.stats.shop_discount",
							Component.literal(formatDecimal(ClientStats.shopDiscount()) + "%").withColor(LEAF_YELLOW)),
					this.rowsRight, this.negotiationRowY + ROW_HEIGHT - 4, HOVER_TEXT, false);
		} else {
			// 幸运值 = 50×(1−e^(−0.0601x))——「稀有及以上物品的出现概率增加 y%」，y% 是绿的
			graphics.drawString(this.font,
					Component.translatable("screen.sephiria_fantasy.stats.luck_bonus",
							Component.literal(formatDecimal(ArtifactLoot.luckPercent(ClientStats.luck())) + "%")
									.withColor(BONUS_GREEN)),
					this.rowsRight, this.luckRowY + ROW_HEIGHT - 4, HOVER_TEXT, false);
		}
	}

	private void drawBreakdown(GuiGraphics graphics, int rowY, PlayerStats.PhysicalBreakdown breakdown) {
		graphics.drawString(this.font, breakdownLine(breakdown), this.rowsLeft, rowY + ROW_HEIGHT - 4,
				HOVER_TEXT, false);
	}

	/** 算式那一行：数字按来源上色，括号与符号保持灰白。 */
	private static Component breakdownLine(PlayerStats.PhysicalBreakdown breakdown) {
		return Component.empty()
				.append(Component.literal("=("))
				.append(colour(breakdown.base(), WHITE)).append(Component.literal("+"))
				.append(colour(breakdown.artifactFlat(), LEAF_YELLOW)).append(Component.literal("+"))
				.append(colour(breakdown.potionFlat(), POTION_RED)).append(Component.literal(")*("))
				.append(Component.literal("1+"))
				.append(colour(breakdown.artifactPercent(), LEAF_YELLOW)).append(Component.literal("%+"))
				.append(colour(breakdown.potionPercent(), POTION_RED)).append(Component.literal("%)"));
	}

	private static Component colour(double value, int colour) {
		return Component.literal(format(value)).withColor(colour);
	}

	private int rowTop(int row) {
		return CONTENT_TOP + row * ROW_HEIGHT - this.scrollOffset;
	}

	private int rowRightX() {
		return this.rowsLeft + COLUMN_OFFSET;
	}

	/** 这一行是否还在可视区域里（滚动后只画看得见的部分）。 */
	private boolean visible(int rowY) {
		return rowY + ROW_HEIGHT > CONTENT_TOP && rowY < this.height - BOTTOM_MARGIN;
	}

	private void row(GuiGraphics graphics, int x, int row, String key, String value) {
		int y = rowTop(row);

		if (visible(y)) {
			graphics.drawString(this.font, Component.translatable(key, value), x, y, WHITE, false);
		}
	}

	/** 鼠标是否悬停在某一行的行高范围内。 */
	private static boolean inRow(int rowY, int mouseY) {
		return mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
	}

	/** 内容装不下时才有的最大滚动量。 */
	private int maxScroll() {
		return Math.max(0, CONTENT_HEIGHT - (this.height - CONTENT_TOP - BOTTOM_MARGIN));
	}

	/** 右侧滚动进度条：只有一屏装不下时才画。 */
	private void drawScrollbar(GuiGraphics graphics) {
		int maxScroll = maxScroll();

		if (maxScroll <= 0) {
			return;
		}

		int x = Math.min(this.rowsRight + COLUMN_WIDTH + 4, this.width - SCROLLBAR_WIDTH - 2);
		int top = CONTENT_TOP;
		int track = this.height - BOTTOM_MARGIN - top;

		if (track <= 0) {
			return;
		}

		int visibleHeight = this.height - CONTENT_TOP - BOTTOM_MARGIN;
		int thumb = Math.max(SCROLLBAR_WIDTH * 4, track * visibleHeight / CONTENT_HEIGHT);
		int thumbY = top + (track - thumb) * this.scrollOffset / maxScroll;
		graphics.fill(x, top, x + SCROLLBAR_WIDTH, top + track, SCROLLBAR_TRACK_COLOUR);
		graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumb, SCROLLBAR_THUMB_COLOUR);
	}

	/** 属性都按整数显示（当前数值都是整的），需要小数时再改。 */
	private static String format(double value) {
		return String.valueOf(Math.round(value));
	}

	/** 商店折扣是公式算出来的小数，保留 1 位小数显示。 */
	private static String formatDecimal(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	/** 百分比：整数就写整数（50%），带小数时保留一位（33.3%）——防御力折算的减伤用。 */
	private static String formatPercent(double value) {
		return value == Math.rint(value) ? format(value) : formatDecimal(value);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
