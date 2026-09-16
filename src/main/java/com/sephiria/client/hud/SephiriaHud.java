package com.sephiria.client.hud;

import com.sephiria.Sephiria;
import com.sephiria.ability.DashSkill;
import com.sephiria.client.SkillClientData;
import com.sephiria.weapon.SephiriaCrossbowItem;
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		// 开界面时不需要额外判断：HUD 层在那种情况下本来就不会被绘制
		renderWeapon(extractor, client);
		renderDash(extractor, client);
	}

	private void renderWeapon(GuiGraphicsExtractor extractor, Minecraft client) {
		ItemStack stack = client.player.getMainHandItem();

		if (!(stack.getItem() instanceof SephiriaWeapon)) {
			return;
		}

		Font font = client.font;
		Component name = stack.getHoverName();
		Component property = propertyLine(stack);

		int textWidth = font.width(name);
		if (property != null) {
			textWidth = Math.max(textWidth, font.width(property));
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

	/** 武器的特有属性行；没有特有属性的武器返回 null（先留空）。 */
	private static Component propertyLine(ItemStack stack) {
		if (stack.getItem() instanceof SephiriaCrossbowItem) {
			return Component.translatable("ui.sephiria.magazine",
					format(SkillClientData.current(SephiriaCrossbowItem.STORAGE)),
					format(SkillClientData.max(SephiriaCrossbowItem.STORAGE)));
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
