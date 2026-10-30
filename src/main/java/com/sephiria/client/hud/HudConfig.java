package com.sephiria.client.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 每个 HUD 模块的显示开关、屏幕位置和缩放，存在 config/sephiria-fantasy-hud.properties。
 *
 * <p>位置是 GUI 缩放坐标下的模块左上角，缩放围绕左上角进行，渲染时再把整块夹在屏幕内
 * （见 {@link SephiriaHud#layout(String)}）。默认位置依赖当前分辨率，所以第一次用到某个
 * 模块时才按屏幕大小落位（{@link Element#placed}）。
 *
 * <p>旧版配置只有一个全局的 {@code visible} / {@code scale} / {@code show_skills} /
 * {@code show_artifacts} / {@code offset_x} / {@code offset_y}。读取时如果某个模块没有
 * {@code skills.*} / {@code artifacts.*} 新键，就把旧键翻译成它的初始配置：显示开关与缩放
 * 直接继承，偏移量叠加到默认位置上。写盘统一用新键，所以旧文件升级后不会被写回旧格式。
 */
public final class HudConfig {
	/** 技能 HUD（6 个技能栏的图标与冷却）。 */
	public static final String SKILLS = "skills";
	/** 神器 HUD（有冷却 / 充能的神器图标）。 */
	public static final String ARTIFACTS = "artifacts";

	/** 全部可调的 HUD 模块，顺序即设置界面里的顺序；新增模块在这里登记。 */
	public static final List<String> MODULES = List.of(SKILLS, ARTIFACTS);

	public static final double SCALE_MIN = 0.5D;
	public static final double SCALE_MAX = 2.0D;
	/** 布局界面里每按一次 - / + 的缩放步长。 */
	public static final double SCALE_STEP = 0.05D;

	/** 默认落位离屏幕左边的距离（和旧 HUD 的 8px 边距一致）。 */
	private static final int MARGIN = 8;
	/** 默认按 9 个图标估算行宽：技能栏最多 6 格、神器最多 9 个，取大的那一边。 */
	private static final int DEFAULT_SLOTS = 9;
	private static final int DEFAULT_ICON_STEP = 18;
	/** 行高含图标下方 4px 的充能条，与 {@link SephiriaHud} 的 ROW_HEIGHT 一致。 */
	private static final int DEFAULT_ROW_HEIGHT = 22;
	/** 技能行离屏幕底边的距离；神器行在它上面 22px（旧 HUD 的两行间距 + 行高）。 */
	private static final int SKILLS_BOTTOM = 64;
	private static final int ARTIFACTS_BOTTOM = 86;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir()
			.resolve("sephiria-fantasy-hud.properties");
	private static final Map<String, Element> ELEMENTS = new LinkedHashMap<>();
	/** 旧格式的偏移量，只在模块还没有绝对坐标时叠加到默认落位上。 */
	private static int legacyOffsetX;
	private static int legacyOffsetY;
	private static boolean loaded;

	private HudConfig() {
	}

	/** 一个 HUD 模块的位置与缩放。 */
	public static final class Element {
		public boolean visible = true;
		public double x;
		public double y;
		public double scale = 1.0D;
		public boolean placed;

		private void placeFrom(String module, int screenWidth, int screenHeight) {
			if (this.placed) {
				return;
			}

			// 缩放围绕左上角：放大多少就把默认 y 往上挪多少，让底边仍贴着原来的位置
			double extra = DEFAULT_ROW_HEIGHT * Math.max(0.0D, this.scale - 1.0D);
			this.x = MARGIN + legacyOffsetX;
			this.y = (switch (module) {
				case SKILLS -> screenHeight - SKILLS_BOTTOM;
				case ARTIFACTS -> screenHeight - ARTIFACTS_BOTTOM;
				default -> screenHeight / 2.0D;
			}) + legacyOffsetY - extra;
			clamp(this, screenWidth, screenHeight, DEFAULT_SLOTS * DEFAULT_ICON_STEP, DEFAULT_ROW_HEIGHT);
			this.placed = true;
		}
	}

	public static Element element(String module) {
		load();

		Element element = ELEMENTS.computeIfAbsent(module, key -> new Element());
		Minecraft client = Minecraft.getInstance();

		if (!element.placed && client.getWindow() != null) {
			element.placeFrom(module, client.getWindow().getGuiScaledWidth(),
					client.getWindow().getGuiScaledHeight());
		}

		return element;
	}

	/** 把模块夹在屏幕内：宽高按缩放后的实际尺寸算。 */
	public static void clamp(Element element, int screenWidth, int screenHeight, int width, int height) {
		double maxX = Math.max(0.0D, screenWidth - width * element.scale);
		double maxY = Math.max(0.0D, screenHeight - height * element.scale);
		element.x = Mth.clamp(element.x, 0.0D, maxX);
		element.y = Mth.clamp(element.y, 0.0D, maxY);
	}

	public static double clampScale(double scale) {
		return Mth.clamp(scale, SCALE_MIN, SCALE_MAX);
	}

	public static void load() {
		if (loaded) {
			return;
		}

		loaded = true;

		if (!Files.isRegularFile(FILE)) {
			return;
		}

		Properties properties = new Properties();

		try (InputStream input = Files.newInputStream(FILE)) {
			properties.load(input);
		}
		catch (IOException | IllegalArgumentException e) {
			// 文件读不动就全用默认布局，不让 HUD 因为一个坏文件起不来
			return;
		}

		boolean globalVisible = parseBool(properties, "visible", true);
		boolean showSkills = parseBool(properties, "show_skills", true);
		boolean showArtifacts = parseBool(properties, "show_artifacts", true);
		double legacyScale = clampScale(parseDouble(properties, "scale", 1.0D));
		legacyOffsetX = parseInt(properties, "offset_x", 0);
		legacyOffsetY = parseInt(properties, "offset_y", 0);

		for (String module : MODULES) {
			Element element = new Element();

			if (hasModuleKeys(properties, module)) {
				element.x = parseDouble(properties, module + ".x", 0.0D);
				element.y = parseDouble(properties, module + ".y", 0.0D);
				element.scale = clampScale(parseDouble(properties, module + ".scale", 1.0D));
				element.visible = parseBool(properties, module + ".visible", true);
				// 只写了开关 / 缩放而没有坐标：位置仍按默认落位算
				element.placed = properties.containsKey(module + ".x") || properties.containsKey(module + ".y");
			}
			else {
				element.visible = globalVisible && (SKILLS.equals(module) ? showSkills : showArtifacts);
				element.scale = legacyScale;
				// 位置要等窗口尺寸出来再算，placeFrom 会把旧偏移量算进去
				element.placed = false;
			}

			ELEMENTS.put(module, element);
		}
	}

	public static void save() {
		Properties properties = new Properties();
		Element skills = element(SKILLS);
		Element artifacts = element(ARTIFACTS);

		for (String module : MODULES) {
			// 位置在拖动 / 缩放时已经按该模块的真实行宽夹过（SephiriaHud.layout），
			// 这里不要再按固定宽度夹一次：技能行只有 6 格，按 9 格夹会把它们往左推
			Element element = element(module);
			properties.setProperty(module + ".visible", Boolean.toString(element.visible));
			properties.setProperty(module + ".x", Integer.toString((int) Math.round(element.x)));
			properties.setProperty(module + ".y", Integer.toString((int) Math.round(element.y)));
			properties.setProperty(module + ".scale",
					Double.toString(Math.round(element.scale * 100.0D) / 100.0D));
		}

		// 同时写一份旧版的全局键：万一降级回旧构建，至少能读回显示开关与缩放。
		// 新版读取时只要看到 skills.* / artifacts.* 就不会再走旧键，所以不会打架。
		// 旧版没有绝对坐标的概念（只有 offset），这里写 0，位置由旧版的默认角落决定。
		properties.setProperty("visible", Boolean.toString(skills.visible || artifacts.visible));
		properties.setProperty("scale", Double.toString(Math.round(skills.scale * 100.0D) / 100.0D));
		properties.setProperty("show_skills", Boolean.toString(skills.visible));
		properties.setProperty("show_artifacts", Boolean.toString(artifacts.visible));
		properties.setProperty("offset_x", "0");
		properties.setProperty("offset_y", "0");

		try {
			Files.createDirectories(FILE.getParent());

			try (OutputStream output = Files.newOutputStream(FILE)) {
				properties.store(output, "Sephiria Fantasy HUD layout");
			}
		}
		catch (IOException ignored) {
			// 写不进去只影响下次启动的布局，不影响本次游戏
		}
	}

	private static boolean hasModuleKeys(Properties properties, String module) {
		String prefix = module + ".";

		return properties.containsKey(prefix + "visible") || properties.containsKey(prefix + "x")
				|| properties.containsKey(prefix + "y") || properties.containsKey(prefix + "scale");
	}

	private static boolean parseBool(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value);
	}

	private static double parseDouble(Properties properties, String key, double fallback) {
		String value = properties.getProperty(key);

		if (value == null) {
			return fallback;
		}

		try {
			double parsed = Double.parseDouble(value);
			return Double.isFinite(parsed) ? parsed : fallback;
		}
		catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static int parseInt(Properties properties, String key, int fallback) {
		String value = properties.getProperty(key);

		if (value == null) {
			return fallback;
		}

		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			return fallback;
		}
	}
}
