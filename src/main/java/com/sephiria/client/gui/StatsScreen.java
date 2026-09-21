package com.sephiria.client.gui;

import com.sephiria.client.ClientStats;
import com.sephiria.stats.PlayerStats;
import com.sephiria.client.tabs.SephiriaTab;
import com.sephiria.client.tabs.SephiriaTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 属性面板：从背包顶部的「属性」标签切进来，顶部的「背包」标签切回去。
 *
 * <p>只用原版部件拼（文字用 StringWidget、切换用 Button），不自己写渲染——这样界面渲染管线的
 * 变动不会影响它。生命值直接读玩家的原版血量，其余读客户端的属性镜像。
 */
public class StatsScreen extends Screen {
	private static final int ROW_HEIGHT = 14;
	/** 树叶（货币）的图标。 */
	private static final net.minecraft.resources.Identifier LEAF_ICON =
			net.minecraft.resources.Identifier.fromNamespaceAndPath(com.sephiria.Sephiria.MOD_ID, "textures/gui/leaf.png");

	/** 「物理强度」那一行的位置：鼠标悬停时在它下面显示算式。 */
	private int physicalRowY;
	private int ampRowY;
	private int attackSpeedRowY;
	/** 树叶那一行：图标 + 数值，画在最上面。 */
	private int leafRowY;
	private int rowsLeft;

	public StatsScreen() {
		super(Component.translatable("screen.sephiria.stats"));
	}

	@Override
	protected void init() {
		int left = this.width / 2 - 100;
		int y = 34;

		// 标签栏：与背包界面里那一排位置对应（原版背包 / 赛菲利亚背包 / 属性）
		SephiriaTabs.add(this, left, 14, SephiriaTab.ATTRIBUTES);

		var player = Minecraft.getInstance().player;
		String health = player == null
				? "-"
				: format(player.getHealth()) + " / " + format(player.getMaxHealth());

		// 树叶（货币）放第一行，并且带图标——一眼就能看到
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
		row(left, y += ROW_HEIGHT, "screen.sephiria.stats.melee_range", format(ClientStats.meleeRange()) + "%");
		this.rowsLeft = left;
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

		// 树叶：图标在文字左边，数值黄色
		extractor.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, LEAF_ICON,
				this.rowsLeft, this.leafRowY, 0.0F, 0.0F, 12, 12, 16, 16);
		extractor.text(this.font, Component.translatable("screen.sephiria.stats.leaves",
						Component.literal(format(ClientStats.leaves())).withColor(0xFFFFD24A)),
				this.rowsLeft + 15, this.leafRowY + 2, 0xFFFFFFFF);

		if (mouseX < this.rowsLeft || mouseX >= this.rowsLeft + 200) {
			return;
		}

		if (mouseY >= this.physicalRowY && mouseY < this.physicalRowY + ROW_HEIGHT) {
			drawBreakdown(extractor, this.physicalRowY, ClientStats.physicalBreakdown());
		} else if (mouseY >= this.attackSpeedRowY && mouseY < this.attackSpeedRowY + ROW_HEIGHT) {
			drawBreakdown(extractor, this.attackSpeedRowY, ClientStats.attackSpeedBreakdown());
		}
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
}
