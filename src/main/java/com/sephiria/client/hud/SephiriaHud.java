package com.sephiria.client.hud;

import com.sephiria.ability.DashSkill;
import com.sephiria.client.ClientStats;
import com.sephiria.client.CloudClientData;
import com.sephiria.client.FrostClientData;
import com.sephiria.client.InvulnClientData;
import com.sephiria.client.SkillClientData;
import com.sephiria.client.SunSwordClientData;
import com.sephiria.network.HudSyncPayload;
import com.sephiria.registry.ModItems;
import com.sephiria.weapon.SephiriaCrossbowItem;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaGreatswordItem;
import com.sephiria.weapon.SephiriaKatanaItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.21.1 的 HUD：技能 / 神器两排图标 + 26.3 的武器面板、冲刺、MP、乌云、太阳剑、
 * 冰霜武具与无敌条。每个模块都按 {@link HudConfig} 里的位置、缩放与显示开关独立绘制。
 *
 * <p>图标行沿用 1.21.1 物品图标（{@code renderItem}）而不是 26.3 的贴图 blit，格距仍是
 * 18px；冷却 / 充能改成与 26.3 一致的<b>顺时针扇形扫描</b>（从正上方开始，进度越高压暗的
 * 扇形越小），走完那一下闪一圈白光。文字面板（武器 / 冲刺 / MP / 乌云 / 太阳剑 / 冰霜）
 * 与 26.3 同一套尺寸与配色。
 *
 * <p>拖动布局时每块模块可以单独摆位、单独缩放（{@link HudConfigScreen} →
 * {@link HudModuleScreen}）。没有数据时正式 HUD 不画（乌云没激活、没拿武器等），
 * 但布局界面里始终画一份占位内容，保证永远有东西可以拖。
 */
public final class SephiriaHud implements HudRenderCallback {
	private static final int ICON = 16;
	private static final int STEP = 18;
	/** 行高留一点余量（图标 16px + 上下底衬）；布局夹取按它算，保证放大后也不出屏幕下边。 */
	private static final int ROW_HEIGHT = 22;
	/** 图标相对行内左上角的留白（给 1px 底衬）。 */
	private static final int ICON_OFFSET = 1;
	/** 没有实际条目时预览用的占位格数：技能栏 6 格、神器最多 9 个。 */
	private static final int DEFAULT_SKILL_SLOTS = 6;
	private static final int DEFAULT_ARTIFACT_SLOTS = 9;
	/**
	 * 图标行的右对齐锚点（局部像素）：9 个图标满行时的宽度。
	 *
	 * <p>与 26.3 一致：图标从右往左排，右边缘固定在锚点上——新增条目只往左长，
	 * 老图标的位置不动，也不会和右下角的武器面板错开。
	 */
	private static final int HUD_RIGHT_ANCHOR = 9 * STEP;

	/** 文字面板：上下左右留白、每行字高。 */
	private static final int PAD = 4;
	private static final int LINE = 10;
	/** 文字面板右侧的进度条尺寸与轨道色。 */
	private static final int BAR_WIDTH = 60;
	private static final int BAR_HEIGHT = 8;
	private static final int BAR_TRACK_COLOR = 0xFF202020;
	/** 武器面板里的蓄力条尺寸（剑意在 1.21.1 还没移植，暂时只有巨剑蓄力用得上）。 */
	private static final int CHARGE_BAR_WIDTH = 60;
	private static final int CHARGE_BAR_HEIGHT = 4;
	private static final int CHARGE_BAR_COLOR = 0xFFFFFFFF;
	private static final int CHARGE_BAR_BACK_COLOR = 0x60000000;
	/** 无敌条：真实高度 3px，布局框给 9px 好拖（见 {@link #INVULN_PREVIEW_HEIGHT}）。 */
	private static final int INVULN_WIDTH = 60;
	private static final int INVULN_HEIGHT = 3;
	private static final int INVULN_PREVIEW_HEIGHT = 9;
	private static final int INVULN_COLOR = 0xFFFFFFFF;
	/** 各类进度条填充色（与 26.3 同一套）。 */
	private static final int MP_BAR_COLOR = 0xFF3B6BE8;
	private static final int CLOUD_BAR_COLOR = 0xFF7EFAFF;
	private static final int SUN_SWORD_BAR_COLOR = 0xFFFDF74C;
	/** 冰霜武具里的两种字色：可用了冰蓝，否则灰。 */
	private static final int FROST_READY_COLOR = 0xFF8FDCFF;
	private static final int FROST_COOLING_COLOR = 0xFF808080;
	/** 冰霜武具那一行里两个字之间的间距。 */
	private static final int FROST_GAP = 3;
	/** 冰弓的默认上限（只在占位预览里用）。 */
	private static final int BOW_PREVIEW_AMMO = 7;

	private static final int PANEL_COLOR = 0x90101010;
	private static final int NAME_COLOR = 0xFFFFFFFF;
	private static final int PROPERTY_COLOR = 0xFFB8B8B8;
	private static final int COOLDOWN_COLOR = 0xB0000000;
	private static final int FLASH_COLOR = 0x80FFFFFF;
	private static final long FLASH_MILLIS = 260L;
	private static final int FRAME_VISIBLE_COLOR = 0xFFFFFFFF;
	private static final int FRAME_HIDDEN_COLOR = 0xFF808080;
	private static final int PLACEHOLDER_FILL = 0x40000000;
	private static final int PLACEHOLDER_BORDER = 0x60FFFFFF;

	/** 上一次进度（用来判断「刚完成」）与闪光结束时间，键是 {@code s:物品id} / {@code a:物品id}。 */
	private static final Map<String, Float> LAST_PROGRESS = new HashMap<>();
	private static final Map<String, Long> FLASH_UNTIL = new HashMap<>();

	/** 换世界 / 重连时清掉冷却闪光的记忆（HUD 镜像也会一起清）。 */
	public static void resetFlash() {
		LAST_PROGRESS.clear();
		FLASH_UNTIL.clear();
	}

	/**
	 * 一个模块在屏幕上的实际位置与尺寸（已经夹进屏幕）。缩放后的宽高是
	 * {@code width * scale} / {@code height * scale}。
	 */
	public record Layout(double x, double y, int width, int height, double scale) {
		public boolean contains(double mouseX, double mouseY) {
			return mouseX >= this.x && mouseX <= this.x + this.width * this.scale
					&& mouseY >= this.y && mouseY <= this.y + this.height * this.scale;
		}
	}

	/** 一个模块渲染所需的内容尺寸；{@code available == false} 表示当前没有可画的数据。 */
	private record Panel(int width, int height, boolean available) {
	}

	/** 武器面板的一行内容（名称 / 特有属性 / 可选的进度条）。 */
	private record WeaponView(ItemStack stack, Component name, Component property, float bar) {
	}

	@Override
	public void onHudRender(GuiGraphics graphics, DeltaTracker tickDelta) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.options.hideGui) {
			return;
		}

		renderIconRow(graphics, HudConfig.SKILLS, HudClientData.skills(), "s");
		renderIconRow(graphics, HudConfig.ARTIFACTS, HudClientData.artifacts(), "a");
		renderPanel(graphics, HudConfig.WEAPON, client);
		renderPanel(graphics, HudConfig.DASH, client);
		renderPanel(graphics, HudConfig.MP, client);
		renderPanel(graphics, HudConfig.CLOUD, client);
		renderPanel(graphics, HudConfig.SUN_SWORD, client);
		renderPanel(graphics, HudConfig.FROST, client);
		renderPanel(graphics, HudConfig.INVULN, client);
	}

	// ------------------------------------------------------------------ 技能 / 神器图标行

	/** 正式 HUD 的图标行：模块不可见或没有条目时整排不画。 */
	private static void renderIconRow(GuiGraphics graphics, String module, List<HudSyncPayload.Entry> entries,
			String keyPrefix) {
		HudConfig.Element element = HudConfig.element(module);

		if (!element.visible || entries.isEmpty()) {
			return;
		}

		Layout layout = layout(module, HUD_RIGHT_ANCHOR, ROW_HEIGHT);
		graphics.pose().pushPose();
		graphics.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
		graphics.pose().scale((float) layout.scale(), (float) layout.scale(), 1.0F);
		// 右对齐：图标从锚点往左排，新增条目只往左长（与 26.3 同一套）
		drawIcons(graphics, entries, keyPrefix, HUD_RIGHT_ANCHOR - entries.size() * STEP + ICON_OFFSET);
		graphics.pose().popPose();
	}

	/**
	 * 图标与冷却：冷却是顺时针扇形罩（26.3 的画法），走完那一下闪一圈白光。
	 * 充能条目也走同一套扇形——进度越高，压暗的扇形越小。
	 */
	private static void drawIcons(GuiGraphics graphics, List<HudSyncPayload.Entry> entries, String keyPrefix,
			int startX) {
		long now = System.currentTimeMillis();
		int x = startX;

		for (HudSyncPayload.Entry entry : entries) {
			ItemStack stack = HudClientData.stackOf(entry);

			if (stack.isEmpty()) {
				x += STEP;
				continue;
			}

			graphics.fill(x - 1, 0, x + ICON + 1, ICON + 2, PANEL_COLOR);
			graphics.renderItem(stack, x, ICON_OFFSET);

			float progress = HudClientData.progress(entry);
			String key = keyPrefix + ":" + entry.item();
			Float last = LAST_PROGRESS.put(key, progress);

			if (last != null && last < 0.999F && progress >= 0.999F) {
				FLASH_UNTIL.put(key, now + FLASH_MILLIS);
			}

			drawCooldownWedge(graphics, x, ICON_OFFSET, ICON, progress);

			Long flash = FLASH_UNTIL.get(key);

			if (flash != null) {
				if (flash < now) {
					FLASH_UNTIL.remove(key);
				}
				else {
					drawCircleOverlay(graphics, x, ICON_OFFSET, ICON, FLASH_COLOR);
				}
			}

			x += STEP;
		}
	}

	/**
	 * 顺时针冷却罩：从正上方开始，按 {@code progress} 扫过；扫过的部分不再压暗。
	 * 逐像素判断角度（16×16 一格，开销很小），所以圆形边缘也是圆滑的像素圆。
	 */
	private static void drawCooldownWedge(GuiGraphics graphics, int x, int y, int size, float progress) {
		if (progress >= 0.999F) {
			return;
		}

		double center = size / 2.0D;
		double radius = size / 2.0D;
		double sweep = Mth.clamp(progress, 0.0F, 1.0F) * Math.PI * 2.0D;

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
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, COOLDOWN_COLOR);
				}
			}
		}
	}

	/** 圆形白色闪光（冷却 / 充能完成那一下）。 */
	private static void drawCircleOverlay(GuiGraphics graphics, int x, int y, int size, int colour) {
		double center = size / 2.0D;
		double radius = size / 2.0D;

		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double dx = px + 0.5D - center;
				double dy = py + 0.5D - center;

				if (dx * dx + dy * dy <= radius * radius) {
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, colour);
				}
			}
		}
	}

	// ------------------------------------------------------------------ 文字面板模块

	/** 正式 HUD 的文字面板：不可见或没有数据时整块不画。 */
	private static void renderPanel(GuiGraphics graphics, String module, Minecraft client) {
		HudConfig.Element element = HudConfig.element(module);

		if (!element.visible) {
			return;
		}

		Panel panel = panelOf(module, client, false);

		if (!panel.available()) {
			return;
		}

		Layout layout = layout(module, panel.width(), panel.height());
		graphics.pose().pushPose();
		graphics.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
		graphics.pose().scale((float) layout.scale(), (float) layout.scale(), 1.0F);
		drawPanel(graphics, module, client, panel, false);
		graphics.pose().popPose();
	}

	/** 布局界面用预览：不看显示开关，但用白 / 灰框标出当前是否可见。 */
	public static void renderPreview(GuiGraphics graphics, String module) {
		Minecraft client = Minecraft.getInstance();
		Panel panel = panelOf(module, client, true);
		Layout layout = layout(module, panel.width(), panel.height());

		graphics.pose().pushPose();
		graphics.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
		graphics.pose().scale((float) layout.scale(), (float) layout.scale(), 1.0F);

		if (isIconRow(module)) {
			List<HudSyncPayload.Entry> entries = entriesOf(module);
			int slots = entries.isEmpty() ? defaultSlots(module) : entries.size();
			int startX = HUD_RIGHT_ANCHOR - slots * STEP + ICON_OFFSET;

			if (entries.isEmpty()) {
				drawPlaceholders(graphics, client, slots, startX);
			}
			else {
				drawIcons(graphics, entries, HudConfig.SKILLS.equals(module) ? "s" : "a", startX);
			}
		}
		else {
			drawPanel(graphics, module, client, panel, true);
		}

		drawFrame(graphics, panel.width(), panel.height(), HudConfig.element(module).visible);
		graphics.pose().popPose();
	}

	/** 模块在屏幕上的实际位置：用预览尺寸测量，布局界面与正式渲染共用同一套夹取。 */
	public static Layout layout(String module) {
		Minecraft client = Minecraft.getInstance();
		Panel panel = panelOf(module, client, true);
		return layout(module, panel.width(), panel.height());
	}

	private static Layout layout(String module, int width, int height) {
		HudConfig.Element element = HudConfig.element(module);
		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow() == null ? 320 : client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow() == null ? 240 : client.getWindow().getGuiScaledHeight();
		int boxWidth = Math.max(1, width);
		int boxHeight = Math.max(1, height);
		double maxX = Math.max(0.0D, screenWidth - boxWidth * element.scale);
		double maxY = Math.max(0.0D, screenHeight - boxHeight * element.scale);

		return new Layout(Mth.clamp(element.x, 0.0D, maxX), Mth.clamp(element.y, 0.0D, maxY),
				boxWidth, boxHeight, element.scale);
	}

	private static boolean isIconRow(String module) {
		return HudConfig.SKILLS.equals(module) || HudConfig.ARTIFACTS.equals(module);
	}

	private static List<HudSyncPayload.Entry> entriesOf(String module) {
		return HudConfig.ARTIFACTS.equals(module) ? HudClientData.artifacts() : HudClientData.skills();
	}

	private static int defaultSlots(String module) {
		return HudConfig.ARTIFACTS.equals(module) ? DEFAULT_ARTIFACT_SLOTS : DEFAULT_SKILL_SLOTS;
	}

	/**
	 * 量出模块的绘制尺寸。
	 *
	 * <p>{@code preview == true} 时即使没有数据也给出占位内容的尺寸（布局界面要能拖）；
	 * {@code false} 时没有数据就返回 {@code available=false}，正式 HUD 跳过整块。
	 */
	private static Panel panelOf(String module, Minecraft client, boolean preview) {
		Font font = client.font;

		if (isIconRow(module)) {
			// 图标行的框宽固定为锚点（9 格），条目少的时候图标都贴在框的右边
			return new Panel(HUD_RIGHT_ANCHOR, ROW_HEIGHT, !entriesOf(module).isEmpty());
		}

		return switch (module) {
			case HudConfig.WEAPON -> weaponPanel(client, preview);
			case HudConfig.DASH -> textPanel(font, dashText(preview));
			case HudConfig.MP -> barPanel(font, mpText(), font.width("MP"));
			case HudConfig.CLOUD -> {
				if (!preview && !CloudClientData.active()) {
					yield new Panel(0, 0, false);
				}

				yield barPanel(font, cloudText(preview), 0);
			}
			case HudConfig.SUN_SWORD -> {
				if (!preview && !SunSwordClientData.active()) {
					yield new Panel(0, 0, false);
				}

				yield barPanel(font, sunSwordText(preview), 0);
			}
			case HudConfig.FROST -> {
				if (!preview && !FrostClientData.visible()) {
					yield new Panel(0, 0, false);
				}

				int width = font.width(frostLabel());
				List<Component> parts = frostParts(preview);

				for (Component part : parts) {
					width += FROST_GAP + font.width(part);
				}

				yield new Panel(width + PAD * 2, LINE + PAD * 2, true);
			}
			case HudConfig.INVULN -> {
				if (!preview && InvulnClientData.fraction(client.player) <= 0.0F) {
					yield new Panel(0, 0, false);
				}

				yield new Panel(INVULN_WIDTH, INVULN_PREVIEW_HEIGHT, true);
			}
			default -> new Panel(0, 0, false);
		};
	}

	private static Panel textPanel(Font font, Component text) {
		return new Panel(font.width(text) + PAD * 2, LINE + PAD * 2, true);
	}

	private static Panel barPanel(Font font, Component text, int labelMinWidth) {
		int textWidth = Math.max(font.width(text), labelMinWidth);
		return new Panel(textWidth + BAR_WIDTH + PAD * 4, LINE + PAD * 2, true);
	}

	/** 按模块画内容（坐标已经在模块本地空间，原点在左上角）。 */
	private static void drawPanel(GuiGraphics graphics, String module, Minecraft client, Panel panel,
			boolean preview) {
		switch (module) {
			case HudConfig.WEAPON -> drawWeapon(graphics, client, panel, preview);
			case HudConfig.DASH -> drawTextPanel(graphics, client.font, panel, dashText(preview));
			case HudConfig.MP -> drawBarPanel(graphics, client.font, panel, mpText(), client.font.width("MP"),
					mpFraction(), MP_BAR_COLOR);
			case HudConfig.CLOUD -> drawBarPanel(graphics, client.font, panel, cloudText(preview), 0,
					cloudFraction(preview), CLOUD_BAR_COLOR);
			case HudConfig.SUN_SWORD -> drawBarPanel(graphics, client.font, panel, sunSwordText(preview), 0,
					sunSwordFraction(preview), SUN_SWORD_BAR_COLOR);
			case HudConfig.FROST -> drawFrost(graphics, client.font, panel, preview);
			case HudConfig.INVULN -> drawInvuln(graphics, client, preview);
			default -> {
			}
		}
	}

	private static void drawTextPanel(GuiGraphics graphics, Font font, Panel panel, Component text) {
		graphics.fill(0, 0, panel.width(), panel.height(), PANEL_COLOR);
		graphics.drawString(font, text, PAD, PAD, NAME_COLOR, true);
	}

	/** 「左边文字 + 右边进度条」的通用画法（MP / 乌云 / 太阳剑共用）。 */
	private static void drawBarPanel(GuiGraphics graphics, Font font, Panel panel, Component text,
			int labelMinWidth, float fraction, int colour) {
		graphics.fill(0, 0, panel.width(), panel.height(), PANEL_COLOR);
		graphics.drawString(font, text, PAD, PAD, NAME_COLOR, true);
		int barX = PAD * 2 + Math.max(font.width(text), labelMinWidth);
		int barY = PAD + 1;
		graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BAR_TRACK_COLOR);

		int filled = Math.round(BAR_WIDTH * Mth.clamp(fraction, 0.0F, 1.0F));

		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, colour);
		}
	}

	// ------------------------------------------------------------------ 武器面板

	private static Panel weaponPanel(Minecraft client, boolean preview) {
		WeaponView view = weaponView(client, preview);

		if (view.stack().isEmpty()) {
			return new Panel(0, 0, false);
		}

		Font font = client.font;
		int textWidth = font.width(view.name());

		if (view.property() != null) {
			int propertyWidth = font.width(view.property());

			if (!Float.isNaN(view.bar())) {
				propertyWidth += CHARGE_BAR_WIDTH + 2;
			}

			textWidth = Math.max(textWidth, propertyWidth);
		}

		int width = textWidth + PAD * 3 + ICON;
		int height = Math.max(ICON, view.property() == null ? LINE : LINE * 2) + PAD * 2;
		return new Panel(width, height, true);
	}

	private static void drawWeapon(GuiGraphics graphics, Minecraft client, Panel panel, boolean preview) {
		WeaponView view = weaponView(client, preview);

		if (view.stack().isEmpty()) {
			return;
		}

		Font font = client.font;
		int width = panel.width();
		int height = panel.height();
		graphics.fill(0, 0, width, height, PANEL_COLOR);
		// 图标贴最右侧
		graphics.renderItem(view.stack(), width - ICON - PAD, (height - ICON) / 2);
		// 名称在上、特有属性在下
		graphics.drawString(font, view.name(), PAD, PAD, NAME_COLOR, true);

		if (view.property() != null) {
			graphics.drawString(font, view.property(), PAD, PAD + LINE, PROPERTY_COLOR, true);
		}

		if (!Float.isNaN(view.bar())) {
			// 进度条型属性：标签后面跟一条随蓄力增长的条
			int barLeft = PAD + font.width(view.property()) + 2;
			int barTop = PAD + LINE + (LINE - CHARGE_BAR_HEIGHT) / 2;
			int filled = Math.round(CHARGE_BAR_WIDTH * Mth.clamp(view.bar(), 0.0F, 1.0F));

			graphics.fill(barLeft, barTop, barLeft + CHARGE_BAR_WIDTH, barTop + CHARGE_BAR_HEIGHT,
					CHARGE_BAR_BACK_COLOR);

			if (filled > 0) {
				graphics.fill(barLeft, barTop, barLeft + filled, barTop + CHARGE_BAR_HEIGHT, CHARGE_BAR_COLOR);
			}
		}
	}

	/**
	 * 武器面板的内容：手里拿着 SEPHIRIA 武器就用它；布局预览里没拿武器时用刀当占位，
	 * 保证「武器 UI」这一块永远有东西可拖。
	 */
	private static WeaponView weaponView(Minecraft client, boolean preview) {
		ItemStack stack = ItemStack.EMPTY;

		if (client.player != null) {
			ItemStack held = client.player.getMainHandItem();

			if (held.getItem() instanceof SephiriaWeapon) {
				stack = held;
			}
		}

		if (stack.isEmpty() && preview) {
			stack = new ItemStack(ModItems.BLADE);
		}

		if (stack.isEmpty()) {
			return new WeaponView(ItemStack.EMPTY, Component.empty(), null, Float.NaN);
		}

		return new WeaponView(stack, stack.getHoverName(), propertyLine(stack), propertyBar(client, stack));
	}

	/** 武器的特有属性行；没有特有属性的武器返回 null（盾 / 长棍暂时不显示属性行）。 */
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
			// 1.21.1 的刀还没有剑意系统（26.3 的 SWORD_INTENT 未移植），所以只显示入鞘 / 拔刀
			return Component.translatable("ui.sephiria_fantasy.katana_state",
					Component.translatable(SephiriaKatanaItem.isSheathed(stack)
							? "ui.sephiria_fantasy.sheathed"
							: "ui.sephiria_fantasy.unsheathed"));
		}

		return null;
	}

	/**
	 * 进度条型特有属性的当前占比（0~1）；该武器没有进度条时返回 NaN。
	 *
	 * <p>巨剑的蓄力进度不需要任何同步包：客户端自己就知道有没有在使用、还剩多少使用刻。
	 * 刀的剑意在 1.21.1 还没移植，所以这里不画条。
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

	// ------------------------------------------------------------------ 冲刺 / MP / 乌云 / 太阳剑

	private static Component dashText(boolean preview) {
		double current = SkillClientData.current(DashSkill.STORAGE);
		double max = SkillClientData.max(DashSkill.STORAGE);

		if (preview && max <= 0.0D) {
			// 还没收到同步包时用基础次数占位，布局界面里才有内容可看
			current = DashSkill.MAX_CHARGES;
			max = DashSkill.MAX_CHARGES;
		}

		return Component.translatable("ui.sephiria_fantasy.dash", format(current), format(max));
	}

	private static Component mpText() {
		return Component.literal(format(ClientStats.mp()) + "/" + format(ClientStats.maxMp()));
	}

	private static float mpFraction() {
		double max = ClientStats.maxMp();
		return max <= 0.0D ? 0.0F : (float) Mth.clamp(ClientStats.mp() / max, 0.0D, 1.0D);
	}

	private static Component cloudText(boolean preview) {
		double max = CloudClientData.max();
		double current = Math.min(CloudClientData.capacity(), max);

		if (preview && max <= 0.0D) {
			current = 0.0D;
			max = 0.0D;
		}

		return Component.translatable("ui.sephiria_fantasy.cloud", cloudAmount(current), cloudAmount(max));
	}

	private static float cloudFraction(boolean preview) {
		double max = CloudClientData.max();
		double current = Math.min(CloudClientData.capacity(), max);
		return max <= 0.0D ? 0.0F : (float) Mth.clamp(current / max, 0.0D, 1.0D);
	}

	private static Component sunSwordText(boolean preview) {
		double max = SunSwordClientData.max();
		double current = Math.min(SunSwordClientData.current(), max);

		if (preview && max <= 0.0D) {
			current = 0.0D;
			max = 0.0D;
		}

		return Component.translatable("ui.sephiria_fantasy.sun_sword", cloudAmount(current), cloudAmount(max));
	}

	private static float sunSwordFraction(boolean preview) {
		double max = SunSwordClientData.max();
		double current = Math.min(SunSwordClientData.current(), max);
		return max <= 0.0D ? 0.0F : (float) Mth.clamp(current / max, 0.0D, 1.0D);
	}

	/** 容量按整数显示：当前值向下取整——「还够不够打一发」看的是整数（2 点射要 2 点）。 */
	private static String cloudAmount(double value) {
		int floored = (int) Math.floor(value + 1.0E-6D);
		return String.valueOf(Math.max(0, floored));
	}

	// ------------------------------------------------------------------ 冰霜武具 / 无敌条

	private static Component frostLabel() {
		return Component.translatable("ui.sephiria_fantasy.frost_armament");
	}

	/**
	 * 「冰霜武具：剑 锤 矛 弓x/7 斩」里每个字的文本与颜色。
	 *
	 * <p>正式 HUD 只列带在身上的那几件；布局预览里五件全列（灰色），保证宽度稳定可拖。
	 */
	private static List<Component> frostParts(boolean preview) {
		List<Component> parts = new ArrayList<>();

		if (preview || FrostClientData.hasScabbard()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.sword",
					!preview && FrostClientData.scabbardReady()));
		}

		if (preview || FrostClientData.hasHammer()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.hammer",
					!preview && FrostClientData.hammerReady()));
		}

		if (preview || FrostClientData.hasSpear()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.spear",
					!preview && FrostClientData.spearReady()));
		}

		if (preview || FrostClientData.hasBow()) {
			// 弓那一格是「弓x/7」：有弹药就冰蓝（发动得出来），打空了变灰
			int ammo = preview ? BOW_PREVIEW_AMMO : FrostClientData.bowAmmo();
			int max = preview ? BOW_PREVIEW_AMMO : FrostClientData.bowMax();
			parts.add(frostGlyph(Component.translatable("ui.sephiria_fantasy.frost_armament.bow", ammo, max),
					!preview && FrostClientData.bowAmmo() > 0));
		}

		if (preview || FrostClientData.hasGuillotine()) {
			parts.add(frostGlyph("ui.sephiria_fantasy.frost_armament.guillotine",
					!preview && FrostClientData.guillotineReady()));
		}

		return parts;
	}

	private static void drawFrost(GuiGraphics graphics, Font font, Panel panel, boolean preview) {
		graphics.fill(0, 0, panel.width(), panel.height(), PANEL_COLOR);
		int x = PAD;
		graphics.drawString(font, frostLabel(), x, PAD, NAME_COLOR, true);
		x += font.width(frostLabel());

		for (Component part : frostParts(preview)) {
			x += FROST_GAP;
			graphics.drawString(font, part, x, PAD, NAME_COLOR, true);
			x += font.width(part);
		}
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
	 * 无敌条：触发无敌时出现，随无敌时间消耗<b>从两端向中间缩短</b>，归零即消失。
	 *
	 * <p>没有外框，就是一条纯色填充；缩短靠「在自身宽度里居中」实现。
	 * 布局预览里画半满的一条，并把可拖区域留到 9px 高（真实条只有 3px，太细不好拖）。
	 */
	private static void drawInvuln(GuiGraphics graphics, Minecraft client, boolean preview) {
		float fraction = preview ? 0.5F : InvulnClientData.fraction(client.player);

		if (fraction <= 0.0F) {
			return;
		}

		int width = Math.max(1, Math.round(INVULN_WIDTH * fraction));
		int left = (INVULN_WIDTH - width) / 2;
		graphics.fill(left, 0, left + width, INVULN_HEIGHT, INVULN_COLOR);
	}

	// ------------------------------------------------------------------ 预览辅助

	/** 预览框：可见时白色、隐藏时灰色，1px 粗（随缩放变粗）。 */
	private static void drawFrame(GuiGraphics graphics, int width, int height, boolean visible) {
		int color = visible ? FRAME_VISIBLE_COLOR : FRAME_HIDDEN_COLOR;

		graphics.fill(0, 0, width, 1, color);
		graphics.fill(0, height - 1, width, height, color);
		graphics.fill(0, 0, 1, height, color);
		graphics.fill(width - 1, 0, width, height, color);
	}

	/** 没有技能 / 神器数据时的占位格，保证布局界面里永远有东西可以拖（同样右对齐）。 */
	private static void drawPlaceholders(GuiGraphics graphics, Minecraft client, int slots, int startX) {
		for (int index = 0; index < slots; index++) {
			int x = startX + index * STEP;
			int top = ICON_OFFSET - 1;
			int bottom = ICON_OFFSET + ICON + 1;

			graphics.fill(x - 1, top, x + ICON + 1, bottom, PLACEHOLDER_FILL);
			graphics.fill(x - 1, top, x + ICON + 1, top + 1, PLACEHOLDER_BORDER);
			graphics.fill(x - 1, bottom - 1, x + ICON + 1, bottom, PLACEHOLDER_BORDER);
			graphics.fill(x - 1, top, x, bottom, PLACEHOLDER_BORDER);
			graphics.fill(x + ICON, top, x + ICON + 1, bottom, PLACEHOLDER_BORDER);
			graphics.drawCenteredString(client.font, "?", x + ICON / 2, top + 4, PLACEHOLDER_BORDER);
		}
	}

	/** 存储量按整数显示（目前都是次数/发数）。 */
	private static String format(double value) {
		int rounded = (int) Math.round(value);
		return rounded < 0 ? "0" : String.valueOf(rounded);
	}
}
