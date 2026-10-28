package com.sephiria.client.gui;

import com.sephiria.client.ClientStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 简易属性页：显示服务端同步过来的关键数值。 */
public class StatsScreen extends Screen {
	private static final int PANEL = 0xF0101014;
	private static final int BORDER = 0xFF4A4A52;

	public StatsScreen() {
		super(Component.translatable("screen.sephiria_fantasy.stats"));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);

		int left = this.width / 2 - 110;
		int top = this.height / 2 - 100;
		graphics.fill(left, top, left + 220, top + 200, PANEL);
		graphics.fill(left, top, left + 220, top + 1, BORDER);
		graphics.fill(left, top + 199, left + 220, top + 200, BORDER);

		super.render(graphics, mouseX, mouseY, partialTick);

		int x = left + 12;
		int y = top + 12;
		graphics.drawString(this.font, this.title, x, y, 0xFFFFFFFF, false);
		y += 18;

		y = line(graphics, "screen.sephiria_fantasy.stats.mp",
				ClientStats.mp() + " / " + ClientStats.maxMp(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.mp_regen", ClientStats.mpRegen(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.physical", ClientStats.physical(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.fire", ClientStats.fire(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.ice", ClientStats.ice(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.lightning", ClientStats.lightning(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.defense", ClientStats.defense(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.attack_speed", ClientStats.attackSpeed(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.melee_range", ClientStats.meleeRange(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.physical_amp", ClientStats.physicalAmp(), x, y);
		y = line(graphics, "screen.sephiria_fantasy.stats.weapon_damage", ClientStats.weaponDamage(), x, y);
		line(graphics, "screen.sephiria_fantasy.stats.leaves", ClientStats.leaves(), x, y);
	}

	private int line(GuiGraphics graphics, String key, Object value, int x, int y) {
		graphics.drawString(this.font,
				Component.translatable(key, String.valueOf(value)), x, y, 0xFFD0D0D0, false);
		return y + 12;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
