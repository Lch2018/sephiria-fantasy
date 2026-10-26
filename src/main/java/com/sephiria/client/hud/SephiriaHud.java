package com.sephiria.client.hud;

import com.sephiria.Sephiria;
import com.sephiria.client.ClientStats;
import com.sephiria.client.CloudClientData;
import com.sephiria.client.FrostClientData;
import com.sephiria.client.SunSwordClientData;
import com.sephiria.ability.DashSkill;
import com.sephiria.client.InvulnClientData;
import com.sephiria.client.SkillClientData;
import com.sephiria.network.HudSyncPayload;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

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
	/** 刀的剑意条配色：平时浅青，满层金色（和物品栏图标上的条同一套颜色）。 */
	private static final int INTENT_COLOR = 0xFF6FD8FF;
	private static final int INTENT_FULL_COLOR = 0xFFFFD24A;
	/** 乌云容量条的填充色：电蓝 #7EFAFF（与触电 / 乌云雷击的电火花粒子同色）。 */
	private static final int CLOUD_BAR_COLOR = 0xFF7EFAFF;
	/** 太阳剑数量条的填充色：#FDF74C（用户定的太阳色）。 */
	private static final int SUN_SWORD_BAR_COLOR = 0xFFFDF74C;
	/** 冰霜武具充能的两种字色：攒满了冰蓝 #8FDCFF（与冰冻染色同色）、冷却中灰。 */
	private static final int FROST_READY_COLOR = 0xFF8FDCFF;
	private static final int FROST_COOLING_COLOR = 0xFF808080;
	/** 冰霜武具那一行里两个字号之间的间距（像素）。 */
	private static final int FROST_GAP = 3;
	/** 技能 / 神器 HUD 的图标边长、间距与底衬。 */
	private static final int HUD_ICON = 16;
	private static final int HUD_GAP = 2;
	private static final int HUD_PAD = 2;
	/**
	 * 两行的右对齐锚点（局部像素）：9 个图标满行时的宽度。
	 *
	 * <p>图标从右往左排，右边缘固定在这里——新增条目只往左长，老图标的位置不动。
	 * 默认 HUD x 是 guiWidth - 174，锚点 166 正好让右边缘落在屏幕右侧 8 像素处。
	 */
	private static final int HUD_RIGHT_ANCHOR = 9 * HUD_ICON + 8 * HUD_GAP + HUD_PAD * 2;
	private static final int HUD_PANEL_COLOR = 0x90101010;
	/** 冷却未扫过的部分压暗（半透明黑）。 */
	private static final int HUD_DARK_COLOR = 0xB0000000;
	/** 冷却 / 充能完成那一下的白光。 */
	private static final int HUD_FLASH_COLOR = 0x80FFFFFF;
	private static final long HUD_FLASH_MILLIS = 260L;
	/** 上一次进度（用来判断「刚完成」）与闪光结束时间。 */
	private static final Map<String, Float> HUD_LAST_PROGRESS = new HashMap<>();
	private static final Map<String, Long> HUD_FLASH_UNTIL = new HashMap<>();

	/** 换世界 / 重连时清掉冷却闪光的记忆（HUD 镜像也会一起清）。 */
	public static void resetFlash() {
		HUD_LAST_PROGRESS.clear();
		HUD_FLASH_UNTIL.clear();
	}

	/** HUD 模块贴哪条屏幕边：左侧模块保证不从左边出去，右侧模块保证不从右边出去。 */
	private enum HudAnchor {
		LEFT, RIGHT, CENTER
	}

	/**
	 * 所有普通 HUD 模块共用的变换入口：套上 {@code config.scale}，再把整块面板夹在屏幕内。
	 *
	 * <p>缩放围绕模块的左上角进行（与原行为一致）；夹取按缩放后的实际宽高算，
	 * 所以放大到 2 倍也不会让左 / 右 / 下边缘跑出屏幕。技能 / 神器两行有固定右锚点，
	 * 走自己的 {@code renderIconRow}。
	 */
	private static Matrix3x2fStack beginHud(GuiGraphicsExtractor extractor, Minecraft client,
			HudConfig.Element config, int width, int height, HudAnchor anchor) {
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();

		float scale = (float) config.scale;
		float screenWidth = client.getWindow().getGuiScaledWidth();
		float screenHeight = client.getWindow().getGuiScaledHeight();
		float boxWidth = width * scale;
		float boxHeight = height * scale;
		float margin = 8.0F;
		float maxX = Math.max(margin, screenWidth - margin - boxWidth);
		float maxY = Math.max(margin, screenHeight - margin - boxHeight);
		float x = (float) config.x;
		float y = Math.clamp((float) config.y, margin, maxY);

		x = switch (anchor) {
			case LEFT -> Math.clamp(x, margin, maxX);
			// 右侧模块：x 仍是左边缘，但整块面板的右边缘不能超过屏幕
			case RIGHT -> Math.min(Math.max(margin, x), maxX);
			// 居中模块：两边都夹
			case CENTER -> Math.clamp(x, margin, maxX);
		};

		pose.translate(x, y);
		pose.scale(scale, scale);
		return pose;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker delta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		// 开界面时不需要额外判断：HUD 层在那种情况下本来就不会被绘制
		renderSkillHud(extractor, client);
		renderArtifactHud(extractor, client);
		renderWeapon(extractor, client);
		renderDash(extractor, client);
		renderMp(extractor, client);
		renderCloud(extractor, client);
		renderSunSword(extractor, client);
		renderFrost(extractor, client);
		renderInvuln(extractor, client);
	}

	/** 技能 HUD：6 个技能栏的图标；冷却按顺时针从暗到亮扫过，完成时闪一下。 */
	private void renderSkillHud(GuiGraphicsExtractor extractor, Minecraft client) {
		renderIconRow(extractor, client, HudConfig.SKILLS, HudClientData.skills(), "s");
	}

	/** 神器 HUD：有冷却 / 充能的神器图标；画法与技能 HUD 完全一致。 */
	private void renderArtifactHud(GuiGraphicsExtractor extractor, Minecraft client) {
		renderIconRow(extractor, client, HudConfig.ARTIFACTS, HudClientData.artifacts(), "a");
	}

	private void renderIconRow(GuiGraphicsExtractor extractor, Minecraft client, String module,
			List<HudSyncPayload.Entry> entries, String keyPrefix) {
		if (entries.isEmpty()) {
			return;
		}

		int width = entries.size() * HUD_ICON + (entries.size() - 1) * HUD_GAP + HUD_PAD * 2;
		int height = HUD_ICON + HUD_PAD * 2;
		HudConfig.Element config = HudConfig.element(module);
		Matrix3x2fStack pose = extractor.pose();
		pose.pushMatrix();

		// 右边缘固定：默认锚点落在屏幕右侧 8 像素；缩放后如果会超出右边界，就把整行往左推，
		// 保证图标不会从右边跑出屏幕（老图标仍然只会在右侧，新图标向左长）。
		float scale = (float) config.scale;
		float originX = (float) config.x;
		float maxRight = client.getWindow().getGuiScaledWidth() - 8.0F;
		float anchoredRight = originX + HUD_RIGHT_ANCHOR * scale;

		if (anchoredRight > maxRight) {
			originX -= anchoredRight - maxRight;
		}

		pose.translate(originX, (float) config.y);
		pose.scale(scale, scale);

		int panelLeft = HUD_RIGHT_ANCHOR - width;
		extractor.fill(panelLeft, 0, HUD_RIGHT_ANCHOR, height, HUD_PANEL_COLOR);

		long now = System.currentTimeMillis();
		int right = HUD_RIGHT_ANCHOR - HUD_PAD;

		// 从右往左排：列表第一项钉在最右边，之后每加一项就放在前一项左边；
		// 这样新增图标不会挤动已有图标，也不会占用上一个图标的位置。
		for (HudSyncPayload.Entry entry : entries) {
			Item item = HudClientData.itemOf(entry);
			int iconX = right - HUD_ICON;

			if (item != null) {
				float progress = HudClientData.progress(entry);
				String key = keyPrefix + ":" + entry.item();
				Float last = HUD_LAST_PROGRESS.put(key, progress);

				if (last != null && last < 0.999F && progress >= 0.999F) {
					HUD_FLASH_UNTIL.put(key, now + HUD_FLASH_MILLIS);
				}

				extractor.blit(RenderPipelines.GUI_TEXTURED, HudClientData.iconOf(item),
						iconX, HUD_PAD, 0.0F, 0.0F, HUD_ICON, HUD_ICON, 16, 16);
				drawCooldownWedge(extractor, iconX, HUD_PAD, HUD_ICON, progress);

				Long flash = HUD_FLASH_UNTIL.get(key);

				if (flash != null) {
					if (flash < now) {
						HUD_FLASH_UNTIL.remove(key);
					} else {
						drawCircleOverlay(extractor, iconX, HUD_PAD, HUD_ICON, HUD_FLASH_COLOR);
					}
				}
			}

			right = iconX - HUD_GAP;
		}

		pose.popMatrix();
	}

	/**
	 * 顺时针冷却罩：从正上方开始，按 {@code progress} 扫过；扫过的部分不再压暗。
	 * 逐像素判断角度（16×16 一格，开销很小），所以圆形边缘也是圆滑的像素圆。
	 */
	private static void drawCooldownWedge(GuiGraphicsExtractor extractor, int x, int y, int size,
			float progress) {
		if (progress >= 0.999F) {
			return;
		}

		double center = size / 2.0D;
		double radius = size / 2.0D;
		double sweep = Math.clamp(progress, 0.0F, 1.0F) * Math.PI * 2.0D;

		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double dx = px + 0.5D - center;
				double dy = py + 0.5D - center;

				if (dx * dx + dy * dy > radius * radius) {
					continue;
				}

				double angle = Math.atan2(dx, -dy);

				if (angle < 0.0D) {
					angle += Math.PI * 2.0D;
				}

				if (angle >= sweep) {
					extractor.fill(x + px, y + py, x + px + 1, y + py + 1, HUD_DARK_COLOR);
				}
			}
		}
	}

	/** 圆形白色闪光（完成那一下）。 */
	private static void drawCircleOverlay(GuiGraphicsExtractor extractor, int x, int y, int size, int colour) {
		double center = size / 2.0D;
		double radius = size / 2.0D;

		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double dx = px + 0.5D - center;
				double dy = py + 0.5D - center;

				if (dx * dx + dy * dy <= radius * radius) {
					extractor.fill(x + px, y + py, x + px + 1, y + py + 1, colour);
				}
			}
		}
	}

	/**
	 * 冰霜武具的状态行：{@code 冰霜武具：剑 锤 矛 弓x/7 斩}——「剑」= 驱邪剑鞘、「锤」= 暴风雪之锤、
	 * 「矛」= 沃尔斯帕、「弓」= 冰弓（后者带上剩几发 / 上限）、「斩」= 酷寒断头台。可用了用冰蓝
	 * {@link #FROST_READY_COLOR}（前三个是攒满、弓是有弹药、斩是冷却转好）、否则用灰
	 * {@link #FROST_COOLING_COLOR}（各自独立；只带其中几件就只画那几个字，一件都没带整行不画）。
	 *
	 * <p>状态只在服务端（充能表 / 弹药 / 斩冷却在 {@code frost.FrostArmament} 与
	 * {@code CryoGuillotineItem}），变化时服务端推
	 * {@code FrostSyncPayload}，这里读 {@link FrostClientData} 的镜像——与乌云 / 太阳剑同一套。
	 */
	private void renderFrost(GuiGraphicsExtractor extractor, Minecraft client) {
		if (!FrostClientData.visible()) {
			return;
		}

		Font font = client.font;
		Component label = Component.translatable("ui.sephiria_fantasy.frost_armament");
		List<Component> parts = new ArrayList<>();

		if (FrostClientData.hasScabbard()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.sword", FrostClientData.scabbardReady()));
		}

		if (FrostClientData.hasHammer()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.hammer", FrostClientData.hammerReady()));
		}

		if (FrostClientData.hasSpear()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.spear", FrostClientData.spearReady()));
		}

		if (FrostClientData.hasBow()) {
			// 弓那一格是「弓x/7」：有弹药就冰蓝（发动得出来），打空了变灰
			parts.add(frostGlyph(Component.translatable("ui.sephiria_fantasy.frost_armament.bow",
					FrostClientData.bowAmmo(), FrostClientData.bowMax()), FrostClientData.bowAmmo() > 0));
		}

		if (FrostClientData.hasGuillotine()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.guillotine",
					FrostClientData.guillotineReady()));
		}

		int textWidth = font.width(label);

		for (Component part : parts) {
			textWidth += FROST_GAP + font.width(part);
		}

		int width = textWidth + PAD * 2;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.FROST);
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.LEFT);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		int x = PAD;
		extractor.text(font, label, x, PAD, NAME_COLOR, true);
		x += font.width(label);

		for (Component part : parts) {
			x += FROST_GAP;
			extractor.text(font, part, x, PAD, NAME_COLOR, true);
			x += font.width(part);
		}

		pose.popMatrix();
	}

	/** 冰霜武具那一行里的一个字：攒满 / 有弹药 = 冰蓝，否则灰。 */
	private static Component frostGlyph(String key, boolean ready) {
		return frostGlyph(Component.translatable(key), ready);
	}

	/** 同上，但整段文字自己带参数（冰弓那格是「弓x/7」）。 */
	private static Component frostGlyph(Component text, boolean ready) {
		return text.copy().withColor(ready ? FROST_READY_COLOR : FROST_COOLING_COLOR);
	}

	/**
	 * 太阳剑数量条：{@code 太阳剑：当前/上限 [条]}，条用 #FDF74C。
	 *
	 * <p>显示开关与乌云 UI 同一套：服务端收剑时推一份上限 0，UI 跟着消失。
	 */
	private void renderSunSword(GuiGraphicsExtractor extractor, Minecraft client) {
		if (!SunSwordClientData.active()) {
			return;
		}

		Font font = client.font;
		double max = SunSwordClientData.max();
		double current = Math.min(SunSwordClientData.current(), max);
		float fraction = (float) Math.clamp(current / max, 0.0D, 1.0D);
		Component text = Component.translatable("ui.sephiria_fantasy.sun_sword", cloudAmount(current), cloudAmount(max));
		int textWidth = font.width(text);
		int barWidth = 60;
		int width = textWidth + barWidth + PAD * 4;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.SUN_SWORD);
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.LEFT);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		extractor.text(font, text, PAD, PAD, NAME_COLOR, true);
		int barX = PAD * 2 + textWidth;
		int barY = PAD + 2;
		extractor.fill(barX, barY, barX + barWidth, barY + 8, 0xFF202020);
		extractor.fill(barX, barY, barX + (int) (barWidth * fraction), barY + 8, SUN_SWORD_BAR_COLOR);
		pose.popMatrix();
	}

	/**
	 * 乌云容量条：{@code 乌云：当前/上限 [条]}，条用 #7EFAFF。
	 *
	 * <p>只在乌云激活（连击 ≥ 2 档）时显示——服务端收云时会推一份上限 0，UI 就跟着消失，
	 * 所以这里不看连击等级，只看 {@link CloudClientData#active()}。
	 */
	private void renderCloud(GuiGraphicsExtractor extractor, Minecraft client) {
		if (!CloudClientData.active()) {
			return;
		}

		Font font = client.font;
		double max = CloudClientData.max();
		double current = Math.min(CloudClientData.capacity(), max);
		float fraction = (float) Math.clamp(current / max, 0.0D, 1.0D);
		Component text = Component.translatable("ui.sephiria_fantasy.cloud", cloudAmount(current), cloudAmount(max));
		int textWidth = font.width(text);
		int barWidth = 60;
		int width = textWidth + barWidth + PAD * 4;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.CLOUD);
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.LEFT);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		extractor.text(font, text, PAD, PAD, NAME_COLOR, true);
		int barX = PAD * 2 + textWidth;
		int barY = PAD + 2;
		extractor.fill(barX, barY, barX + barWidth, barY + 8, 0xFF202020);
		extractor.fill(barX, barY, barX + (int) (barWidth * fraction), barY + 8, CLOUD_BAR_COLOR);
		pose.popMatrix();
	}

	/** 容量按整数显示：当前值向下取整——「还够不够打一发」看的是整数（2 点射要 2 点）。 */
	private static String cloudAmount(double value) {
		int floored = (int) Math.floor(value + 1.0E-6D);
		return String.valueOf(Math.max(0, floored));
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
		Matrix3x2fStack pose = beginHud(extractor, client, config, BAR_WIDTH, BAR_HEIGHT, HudAnchor.CENTER);

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
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.RIGHT);

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
				extractor.fill(barLeft, barTop, barLeft + filled, barTop + CHARGE_BAR_HEIGHT,
						propertyBarColor(stack, bar));
			}
		}

		pose.popMatrix();
	}

	/** MP 条：{@code MP：[蓝条] 当前/上限}，蓝色填充与巨剑蓄力条同一套画法。 */
	private void renderMp(GuiGraphicsExtractor extractor, Minecraft client) {
		Font font = client.font;
		double current = ClientStats.mp();
		double max = ClientStats.maxMp();
		float fraction = (float) Math.clamp(max <= 0.0D ? 0.0D : current / max, 0.0D, 1.0D);
		String numbers = format(current) + "/" + format(max);
		int textWidth = Math.max(font.width(numbers), font.width("MP"));
		int barWidth = 60;
		int width = textWidth + barWidth + PAD * 4;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.MP);
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.LEFT);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		extractor.text(font, numbers, PAD, PAD, NAME_COLOR, true);
		int barX = PAD * 2 + textWidth;
		int barY = PAD + 2;
		extractor.fill(barX, barY, barX + barWidth, barY + 8, 0xFF202020);
		extractor.fill(barX, barY, barX + (int) (barWidth * fraction), barY + 8, 0xFF3B6BE8);
		pose.popMatrix();
	}

	private void renderDash(GuiGraphicsExtractor extractor, Minecraft client) {
		Font font = client.font;
		Component text = Component.translatable("ui.sephiria_fantasy.dash",
				format(SkillClientData.current(DashSkill.STORAGE)),
				format(SkillClientData.max(DashSkill.STORAGE)));

		int width = font.width(text) + PAD * 2;
		int height = LINE + PAD * 2;
		HudConfig.Element config = HudConfig.element(HudConfig.DASH);
		Matrix3x2fStack pose = beginHud(extractor, client, config, width, height, HudAnchor.LEFT);

		extractor.fill(0, 0, width, height, PANEL_COLOR);
		extractor.text(font, text, PAD, PAD, NAME_COLOR, true);

		pose.popMatrix();
	}

	/**
	 * 进度条的颜色：默认纯白，刀按剑意分色（满层金色，和物品栏里的剑意条一致）。
	 */
	private static int propertyBarColor(ItemStack stack, float fraction) {
		if (stack.getItem() instanceof SephiriaKatanaItem) {
			return fraction >= 1.0F ? INTENT_FULL_COLOR : INTENT_COLOR;
		}

		return BAR_COLOR;
	}

	/**
	 * 进度条型特有属性的当前占比（0~1）；该武器没有进度条时返回 NaN。
	 *
	 * <p>蓄力进度不需要任何同步包：客户端自己就知道有没有在使用、还剩多少使用刻。
	 * 刀则是读剑意的客户端镜像——层数每次变化服务端都会推一份过来。
	 */
	private static float propertyBar(Minecraft client, ItemStack stack) {
		if (stack.getItem() instanceof SephiriaKatanaItem) {
			double max = SkillClientData.max(SephiriaKatanaItem.SWORD_INTENT);
			return max <= 0.0D
					? 0.0F
					: (float) Math.min(1.0D, SkillClientData.current(SephiriaKatanaItem.SWORD_INTENT) / max);
		}

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
			return Component.translatable("ui.sephiria_fantasy.magazine",
					format(SkillClientData.current(SephiriaCrossbowItem.STORAGE)),
					format(SkillClientData.max(SephiriaCrossbowItem.STORAGE)));
		}

		if (stack.getItem() instanceof SephiriaGreatswordItem) {
			return Component.translatable("ui.sephiria_fantasy.whirlwind");
		}

		if (stack.getItem() instanceof SephiriaDaggerItem) {
			return Component.translatable("ui.sephiria_fantasy.focus",
					format(SkillClientData.current(SephiriaDaggerItem.FOCUS)),
					format(SkillClientData.max(SephiriaDaggerItem.FOCUS)));
		}

		if (stack.getItem() instanceof SephiriaKatanaItem) {
			// 状态 + 剑意：层数用进度条表示（和旋风同一种画法），满层条会转金色
			return Component.translatable("ui.sephiria_fantasy.katana",
					Component.translatable(SephiriaKatanaItem.isSheathed(stack)
							? "ui.sephiria_fantasy.sheathed"
							: "ui.sephiria_fantasy.unsheathed"));
		}

		return null;
	}

	/** 存储量按整数显示（目前都是次数/发数）。 */
	private static String format(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}
}
