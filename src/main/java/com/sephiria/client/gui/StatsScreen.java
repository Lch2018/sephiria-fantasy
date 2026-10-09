package com.sephiria.client.gui;

import com.sephiria.client.ClientStats;
import com.sephiria.stats.PlayerStats;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * 属性面板：从背包顶部的「属性」标签切进来，顶部的「背包」标签切回去。
 *
 * <p>只用原版部件拼（文字用 StringWidget、切换用 Button），不自己写渲染——这样界面渲染管线的
 * 变动不会影响它。生命值直接读玩家的原版血量，其余读客户端的属性镜像。
 */
public class StatsScreen extends Screen {
	private static final int ROW_HEIGHT = 14;
	/** 叶子（货币）的图标。 */
	private static final net.minecraft.resources.Identifier LEAF_ICON =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(com.sephiria.Sephiria.MOD_ID, "textures/gui/leaf.png");

	/** 「物理强度」那一行的位置：鼠标悬停时在它下面显示算式。 */
	private int physicalRowY;
	private int ampRowY;
	private int attackSpeedRowY;
	/** 「闪避」那一行的位置：悬停时显示闪避率公式。 */
	private int dodgeRowY;
	/** 「谈判力」那一行的位置：悬停时显示换算出的商店折扣（折扣不再单独占一行）。 */
	private int negotiationRowY;
	/** 叶子那一行：图标 + 数值，画在最上面。 */
	private int leafRowY;
	private int rowsLeft;
	/** 右列的 x（属性分两列，免得一列排不下）。 */
	private int rowsRight;

	public StatsScreen() {
		super(Component.translatable("screen.sephiria.stats"));
	}

	/**
	 * 按「背包键」（默认 E）关掉属性页、回到游戏。
	 *
	 * <p>原版背包、箱子这类界面是 {@code AbstractContainerScreen} 自己处理这个键的，属性页是个普通
	 * {@code Screen}，不补这一手的话按 E 没反应——而另外三页（原版背包 / 赛菲利亚背包 / 商店）都能
	 * 按 E 退出，只有这一页不行会显得很别扭。
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
		int left = this.width / 2 - 210;
		int rightX = left + 210;
		int y = 34;

		// 标签栏：与背包界面里那一排位置对应（原版背包 / 赛菲利亚背包 / 属性）
		// 标签栏仍按「单列面板」居中，属性分两列后它不跟着左移
		SephiriaTabs.add(this, this.width / 2 - 100, 14, SephiriaTab.ATTRIBUTES);

		var player = Minecraft.getInstance().player;
		String health = player == null
				? "-"
				: format(player.getHealth()) + " / " + format(player.getMaxHealth());

		// 叶子（货币）放第一行，并且带图标——一眼就能看到
		this.leafRowY = y;
		y += ROW_HEIGHT;

		row(left, y, "screen.sephiria.stats.hp", health);
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.mp", format(ClientStats.mp()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.mp_regen", format(ClientStats.mpRegen()));
		this.physicalRowY = y += ROW_HEIGHT;
		row(left, this.physicalRowY, "screen.sephiria.stats.physical", format(ClientStats.physical()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.fire", format(ClientStats.fire()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.ice", format(ClientStats.ice()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.lightning", format(ClientStats.lightning()));
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.defense", format(ClientStats.defense()));
		this.ampRowY = y += ROW_HEIGHT;
		row(left, this.ampRowY, "screen.sephiria.stats.physical_amp", format(ClientStats.physicalAmp()) + "%");
		this.attackSpeedRowY = y += ROW_HEIGHT;
		row(left, this.attackSpeedRowY, "screen.sephiria.stats.attack_speed",
				format(ClientStats.attackSpeed()) + "%");
		y = 34;
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.melee_range", format(ClientStats.meleeRange()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.weapon_damage", format(ClientStats.weaponDamage()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.special_attack", format(ClientStats.specialAttack()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.normal_attack_damage", format(ClientStats.normalAttackDamage()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.crit_chance", format(ClientStats.critChance()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.crit_damage", format(ClientStats.critDamage()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.ignore_defense", format(ClientStats.ignoreDefense()));
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.move_speed", format(ClientStats.moveSpeed()) + "%");
		// 闪避直接显示折算后的闪避率（带 %），点数在悬停的算式里给
		this.dodgeRowY = y += ROW_HEIGHT;
		// 行里显示闪避点数（与词条/其它来源的口径一致），闪避率放在悬停算式里
		row(rightX, this.dodgeRowY, "screen.sephiria.stats.dodge", format(ClientStats.dodge()));
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.lifesteal", format(ClientStats.lifesteal()));
		// 商店折扣是谈判力换算出来的，单独占一行太浪费——藏进谈判力的悬停里
		this.negotiationRowY = y += ROW_HEIGHT;
		row(rightX, this.negotiationRowY, "screen.sephiria.stats.negotiation", format(ClientStats.negotiation()));
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.leaf_gain",
				format(ClientStats.leafGainPercent()) + "%");
		row(rightX, y += ROW_HEIGHT, "screen.sephiria.stats.xp_drop",
				format(ClientStats.xpDropPercent()) + "%");
		this.rowsLeft = left;
		this.rowsRight = rightX;
	}

	/**
	 * 鼠标放在「物理强度」或「攻击速度」上时，在那一行下面补一行算式：
	 * {@code 30 =（20+10+0）*（1+0%+0%）}。
	 *
	 * <p>按来源分色：基础值白、神器黄、药水红（树根祝福以后加，银色）。
	 */
	@Override
	public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor extractor, int mouseX, int mouseY,
			float partialTick) {
		super.extractRenderState(extractor, mouseX, mouseY, partialTick);

		// 叶子：图标在文字左边，数值黄色
		extractor.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, LEAF_ICON,
				this.rowsLeft, this.leafRowY, 0.0F, 0.0F, 12, 12, 16, 16);
		extractor.text(this.font, Component.translatable("screen.sephiria.stats.leaves",
						Component.literal(format(ClientStats.leaves())).withColor(0xFFFFD24A)),
				this.rowsLeft + 15, this.leafRowY + 2, 0xFFFFFFFF);

		// 悬停提示跟着行所在的那一列：物理强度与攻速在左列，闪避与谈判力在右列
		// （先按行定列再查 x——谈判力在右列，用左列的 x 范围判断会永远判不中）
		boolean leftRow = inRow(this.physicalRowY, mouseY) || inRow(this.attackSpeedRowY, mouseY);
		boolean rightRow = inRow(this.dodgeRowY, mouseY) || inRow(this.negotiationRowY, mouseY);

		if (!leftRow && !rightRow) {
			return;
		}

		int columnX = leftRow ? this.rowsLeft : this.rowsRight;

		if (mouseX < columnX || mouseX >= columnX + 200) {
			return;
		}

		if (inRow(this.physicalRowY, mouseY)) {
			drawBreakdown(extractor, this.physicalRowY, ClientStats.physicalBreakdown());
		} else if (inRow(this.attackSpeedRowY, mouseY)) {
			drawBreakdown(extractor, this.attackSpeedRowY, ClientStats.attackSpeedBreakdown());
		} else if (inRow(this.dodgeRowY, mouseY)) {
			// 闪避率 = 0.8 × (1 − e^(−闪避 / 43.28))：把点数与结果一起写出来
			extractor.text(this.font, Component.translatable("screen.sephiria.stats.dodge_formula",
					format(ClientStats.dodgeRate()), format(ClientStats.dodge())), this.rowsRight, this.dodgeRowY + 10,
					0xFFDDDDDD);
		} else {
			// 商店折扣 = 70×(1−e^(−谈判力/27.94))：数值与叶子一样用黄色，方便和其它悬停算式区分
			extractor.text(this.font,
					Component.translatable("screen.sephiria.stats.shop_discount",
							Component.literal(formatDecimal(ClientStats.shopDiscount()) + "%").withColor(0xFFFFD24A)),
					this.rowsRight, this.negotiationRowY + 10, 0xFFDDDDDD);
		}
	}

	/** 鼠标是否悬停在某一行的行高范围内。 */
	private static boolean inRow(int rowY, int mouseY) {
		return mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
	}

	private void drawBreakdown(net.minecraft.client.gui.GuiGraphicsExtractor extractor, int rowY,
			PlayerStats.PhysicalBreakdown breakdown) {
		extractor.text(this.font, breakdownLine(breakdown), this.rowsLeft, rowY + 10, 0xFFDDDDDD);
	}

	/** 算式那一行：数字按来源上色，括号与符号保持灰白。 */
	private static Component breakdownLine(PlayerStats.PhysicalBreakdown breakdown) {
		return Component.empty()
				.append(Component.literal("=("))
				.append(colour(breakdown.base(), 0xFFFFFFFF)).append(Component.literal("+"))
				.append(colour(breakdown.artifactFlat(), 0xFFFFD24A)).append(Component.literal("+"))
				.append(colour(breakdown.potionFlat(), 0xFFFF5555)).append(Component.literal(")*("))
				.append(Component.literal("1+"))
				.append(colour(breakdown.artifactPercent(), 0xFFFFD24A)).append(Component.literal("%+"))
				.append(colour(breakdown.potionPercent(), 0xFFFF5555)).append(Component.literal("%)"));
	}

	private static Component colour(double value, int colour) {
		return Component.literal(format(value)).withColor(colour);
	}

	private void row(int left, int y, String key, String value) {
		addRenderableWidget(new StringWidget(left, y, 200, ROW_HEIGHT,
				Component.translatable(key, value), this.font));
	}

	/** 属性都按整数显示（当前数值都是整的），需要小数时再改。 */
	private static String format(double value) {
		return String.valueOf(Math.round(value));
	}

	/** 商店折扣是公式算出来的小数（y = 70×(1−e^(−谈判力/27.94))），保留 1 位小数显示，不四舍五入到整数。 */
	private static String formatDecimal(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
