package com.sephiria.client.hud;

import com.sephiria.Sephiria;
import com.sephiria.ability.DashSkill;
import com.sephiria.client.InvulnClientData;
import com.sephiria.client.SkillClientData;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaGreatswordItem;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/**
 * SEPHIRIA 的 HUD：右下角武器面板 + 左下角冲刺面板。
 *
 * <p>武器面板只在主手拿着 SEPHIRIA 武器时显示，布局是「左文字右图标」——名称在上、
 * 该武器的特有属性在下，图标贴最右侧。有存储量的武器（弩）显示弹匣，刀显示拔刀状态，
 * 其余武器暂时没有特有属性行。
 *
 * <p>位置和缩放都从 {@link HudConfig} 读，可以在 {@link HudConfigScreen} 里调。
 * 26.2 的 HUD 走「提取渲染状态」的流程，所以这里用 {@code GuiGraphicsExtractor} 提交绘制。
 */
public final class SephiriaHud implements HudElement {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "hud");

	private static final int ICON = 16;
	private static final int PAD = 4;
	private static final int LINE = 10;
	private static final int PANEL_COLOR = 0x90101010;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int PROPERTY_COLOR = 0xFFB8B8B8;
	/** 无敌条的整宽与高度（像素），以及颜色。 */
	private static final int BAR_WIDTH = 60;
	private static final int BAR_HEIGHT = 3;
	private static final int BAR_COLOR = 0xFFFFFFFF;
	/** 武器面板里的蓄力条宽度（像素）。 */
	private static final int CHARGE_BAR_WIDTH = 60;
	private static final int CHARGE_BAR_HEIGHT = 4;
	private static final int BAR_BACK_COLOR = 0x60000000;

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		// 开界面时不需要额外判断：HUD 层在那种情况下本来就不会被绘制
		renderWeapon(extractor, client);
		renderDash(extractor, client);
		renderInvuln(extractor, client);
	}

	/**
	 * 无敌条：触发无敌时出现，随无敌时间消耗<b>从两端向中间缩短</b>，归零即消失。
	 *
	 * <p>没有外框，就是一条纯色填充；缩短靠"在自身宽度里居中"实现——只要让填充始终以
	 * 元素中心为中心，两端就会同时收缩。
	 */
	private void renderInvuln(GuiGraphicsExtractor extractor, Minecraft client) {
		float fraction = InvulnClientData.fraction(client.player);

		if (fraction <= 0.0F) {
			return;
		}

		int width = Math.max(1, Math.round(BAR_WIDTH * fraction));
		int left = (BAR_WIDTH - width) / 2;
		HudConfig.Element config = HudConfig.element(HudConfig.INVULN);
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate((float) config.x, (float) config.y);
		pose.scale((float) config.scale, (float) config.scale);

		extractor.fill(left, 0, left + width, BAR_HEIGHT, BAR_COLOR);

		pose.popMatrix();
	}

	private void renderWeapon(GuiGraphicsExtractor extractor, Minecraft client) {
		ItemStack stack = client.player.getMainHandItem();

		if (!(stack.getItem() instanceof SephiriaWeapon)) {
			return;
		}

		Font font = client.font;
		Component name = stack.getHoverName();
		Component property = propertyLine(stack);
		float bar = propertyBar(client, stack);

		int textWidth = font.width(name);
		if (property != null) {
			int propertyWidth = font.width(property);

			if (!Float.isNaN(bar)) {
				propertyWidth += CHARGE_BAR_WIDTH + 2;
			}

			textWidth = Math.max(textWidth, propertyWidth);
		}

		int width = textWidth + PAD * 3 + ICON;
		int height = Math.max(ICON, property == null ? LINE : LINE * 2) + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.WEAPON);
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate((float) config.x, (float) config.y);
		pose.scale((float) config.scale, (float) config.scale);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		// 图标贴最右侧
		extractor.item(stack, width - ICON - PAD, (height - ICON) / 2);
		// 名称在上、特有属性在下
		extractor.text(font, name, PAD, PAD, NAME_COLOR, true);

		if (property != null) {
			extractor.text(font, property, PAD, PAD + LINE, PROPERTY_COLOR, true);
		}

		if (!Float.isNaN(bar)) {
			// 进度条型属性：标签后面跟一条随蓄力增长的条
			int barLeft = PAD + font.width(property) + 2;
			int barTop = PAD + LINE + (LINE - CHARGE_BAR_HEIGHT) / 2;
			int filled = Math.round(CHARGE_BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, bar)));

			extractor.fill(barLeft, barTop, barLeft + CHARGE_BAR_WIDTH, barTop + CHARGE_BAR_HEIGHT, BAR_BACK_COLOR);

			if (filled > 0) {
				extractor.fill(barLeft, barTop, barLeft + filled, barTop + CHARGE_BAR_HEIGHT, BAR_COLOR);
			}
		}

		pose.popMatrix();
	}

	private void renderDash(GuiGraphicsExtractor extractor, Minecraft client) {
		Font font = client.font;
		Component text = Component.translatable("ui.sephiria.dash",
				format(SkillClientData.current(DashSkill.STORAGE)),
				format(SkillClientData.max(DashSkill.STORAGE)));

		int width = font.width(text) + PAD * 2;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.DASH);
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();
		pose.translate((float) config.x, (float) config.y);
		pose.scale((float) config.scale, (float) config.scale);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		extractor.text(font, text, PAD, PAD, NAME_COLOR, true);

		pose.popMatrix();
	}

	/**
	 * 进度条型特有属性的当前占比（0~1）；该武器没有进度条时返回 NaN。
	 *
	 * <p>蓄力进度不需要任何同步包：客户端自己就知道有没有在使用、还剩多少使用刻。
	 */
	private static float propertyBar(Minecraft client, ItemStack stack) {
		if (!(stack.getItem() instanceof SephiriaGreatswordItem) || client.player == null) {
			return Float.NaN;
		}

		ItemStack using = client.player.getUseItem();

		if (!client.player.isUsingItem() || using.getItem() != stack.getItem()) {
			return 0.0F;
		}

		int charge = SephiriaGreatswordItem.chargeTicks(using, client.player,
				client.player.getUseItemRemainingTicks());
		return Math.min(1.0F, charge / (float) SephiriaGreatswordItem.CHARGE_TICKS);
	}

	/** 武器的特有属性行；没有特有属性的武器返回 null（先留空）。 */
	private static Component propertyLine(ItemStack stack) {
		if (stack.getItem() instanceof SephiriaCrossbowItem) {
			return Component.translatable("ui.sephiria.magazine",
					format(SkillClientData.current(SephiriaCrossbowItem.STORAGE)),
					format(SkillClientData.max(SephiriaCrossbowItem.STORAGE)));
		}

		if (stack.getItem() instanceof SephiriaGreatswordItem) {
			return Component.translatable("ui.sephiria.whirlwind");
		}

		if (stack.getItem() instanceof SephiriaDaggerItem) {
			return Component.translatable("ui.sephiria.focus",
					format(SkillClientData.current(SephiriaDaggerItem.FOCUS)),
					format(SkillClientData.max(SephiriaDaggerItem.FOCUS)));
		}

		if (stack.getItem() instanceof SephiriaKatanaItem) {
			return Component.translatable(SephiriaKatanaItem.isSheathed(stack)
					? "ui.sephiria.sheathed"
					: "ui.sephiria.unsheathed");
		}

		return null;
	}

	/** 存储量按整数显示（目前都是次数/发数）。 */
	private static String format(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}
}
