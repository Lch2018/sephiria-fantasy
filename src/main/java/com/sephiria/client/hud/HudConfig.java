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
 * <p>默认布局与 26.3 对齐：武器在右下，技能 / 神器两排在它上方；左下一列从下往上依次是
 * 冲刺、MP、乌云、太阳剑、冰霜武具；无敌条在准星下方居中。
 *
 * <p>旧版配置只有一个全局的 {@code visible} / {@code scale} / {@code show_skills} /
 * {@code show_artifacts} / {@code offset_x} / {@code offset_y}。读取时如果某个模块没有
 * {@code <模块>.*} 新键，就把旧键翻译成它的初始配置：技能 / 神器两排继承旧开关与缩放，
 * 后加的模块（武器、乌云这些旧版没有的）保持显示、缩放 1.0。写盘统一用新键，
 * 所以旧文件升级后不会被写回旧格式。
 */
public final class HudConfig {
	/** 武器 UI（右下角）：名称 + 特有属性 + 图标。 */
	public static final String WEAPON = "weapon";
	/** 冲刺 UI（左下角）：剩余次数。 */
	public static final String DASH = "dash";
	/** MP 条（左下角，默认在冲刺上方）。 */
	public static final String MP = "mp";
	/** 乌云容量条（左下角，默认在 MP 上方；只在乌云激活时显示）。 */
	public static final String CLOUD = "cloud";
	/** 太阳剑数量条（左下角，默认在乌云上方；只在太阳剑激活时显示）。 */
	public static final String SUN_SWORD = "sun_sword";
	/** 冰霜武具充能行（左下角，默认在太阳剑上方；带上那些神器时才显示）。 */
	public static final String FROST = "frost";
	/** 无敌条（准星下方居中）。 */
	public static final String INVULN = "invuln";
	/** 技能 HUD（右下，武器 / 神器上方；6 个技能栏的图标与冷却）。 */
	public static final String SKILLS = "skills";
	/** 神器 HUD（右下，技能 HUD 与武器 UI 之间；有冷却 / 充能的神器图标）。 */
	public static final String ARTIFACTS = "artifacts";

	/** 全部可调的 HUD 模块，顺序即设置界面里的顺序；新增模块在这里登记。 */
	public static final List<String> MODULES = List.of(WEAPON, DASH, MP, CLOUD, SUN_SWORD, FROST, INVULN,
			SKILLS, ARTIFACTS);

	public static final double SCALE_MIN = 0.5D;
	public static final double SCALE_MAX = 2.0D;
	/** 布局界面里每按一次 - / + 的缩放步长。 */
	public static final double SCALE_STEP = 0.05D;

	/** 默认落位离屏幕左边的距离（和旧 HUD 的 8px 边距一致）。 */
	private static final int MARGIN = 8;
	/** 默认按 9 个图标估算行宽：技能栏最多 6 格、神器最多 9 个，取大的那一边。 */
	private static final int DEFAULT_SLOTS = 9;
	private static final int DEFAULT_ICON_STEP = 18;
	/** 图标行高（含图标下方 4px 的充能条），与 {@link SephiriaHud} 的 ROW_HEIGHT 一致。 */
	private static final int DEFAULT_ROW_HEIGHT = 22;
	/** 文字面板高（图标下方一行 10px 文字 + 上下 4px 留白），与 {@link SephiriaHud} 的测量一致。 */
	private static final int DEFAULT_PANEL_HEIGHT = 18;
	/** 面板默认宽度：只用来首次落位时夹进屏幕，真实宽度渲染时按文字重新算。 */
	private static final int DEFAULT_PANEL_WIDTH = 150;
	private static final int DEFAULT_INVULN_WIDTH = 60;
	/** 无敌条默认高度取预览高度（真实条只有 3px，太细不好拖）。 */
	private static final int DEFAULT_INVULN_HEIGHT = 9;

	/** 左下一列从下往上的默认高度；顺序与 26.3 一致：冲刺、MP、乌云、太阳剑、冰霜武具。 */
	private static final int DASH_BOTTOM = 40;
	private static final int MP_BOTTOM = 64;
	private static final int CLOUD_BOTTOM = 88;
	private static final int SUN_SWORD_BOTTOM = 112;
	private static final int FROST_BOTTOM = 136;
	/** 右下三行从下往上的默认高度：武器、神器、技能。 */
	private static final int WEAPON_BOTTOM = 40;
	private static final int ARTIFACTS_BOTTOM = 64;
	private static final int SKILLS_BOTTOM = 88;
	/** 右下三行的默认左边缘：图标行的锚点宽 162px（9 格），加右侧 8px 边距。 */
	private static final int RIGHT_STACK_X = 170;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir()
			.resolve("sephiria-fantasy-hud.properties");
	private static final Map<String, Element> ELEMENTS = new LinkedHashMap<>();
	/** 旧格式的偏移量，只在左下模块还没有绝对坐标时叠加到默认落位上。 */
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
			double extra = defaultHeight(module) * Math.max(0.0D, this.scale - 1.0D);

			switch (module) {
				case WEAPON -> {
					this.x = screenWidth - DEFAULT_PANEL_WIDTH - MARGIN;
					this.y = screenHeight - WEAPON_BOTTOM - extra;
				}
				case ARTIFACTS -> {
					this.x = screenWidth - RIGHT_STACK_X;
					this.y = screenHeight - ARTIFACTS_BOTTOM - extra;
				}
				case SKILLS -> {
					this.x = screenWidth - RIGHT_STACK_X;
					this.y = screenHeight - SKILLS_BOTTOM - extra;
				}
				case INVULN -> {
					this.x = screenWidth / 2.0D - DEFAULT_INVULN_WIDTH / 2.0D;
					this.y = screenHeight / 2.0D + 18.0D;
				}
				default -> {
					this.x = MARGIN + legacyOffsetX;
					this.y = screenHeight - bottomOf(module) + legacyOffsetY - extra;
				}
			}

			clamp(this, screenWidth, screenHeight, defaultWidth(module), defaultHeight(module));
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
			else if (SKILLS.equals(module)) {
				element.visible = globalVisible && showSkills;
				element.scale = legacyScale;
				// 位置要等窗口尺寸出来再算，placeFrom 会把旧偏移量算进去
				element.placed = false;
			}
			else if (ARTIFACTS.equals(module)) {
				element.visible = globalVisible && showArtifacts;
				element.scale = legacyScale;
				element.placed = false;
			}
			else {
				// 旧版没有这些模块：默认按 1.0 缩放、可见落位，不继承旧的全局缩放
				element.visible = globalVisible;
				element.scale = 1.0D;
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
			// 位置在拖动 / 缩放时已经按该模块的真实宽高夹过（SephiriaHud.layout），
			// 这里不要再按固定宽度夹一次：技能行只有 6 格，按 9 格夹会把它们往左推
			Element element = element(module);
			properties.setProperty(module + ".visible", Boolean.toString(element.visible));
			properties.setProperty(module + ".x", Integer.toString((int) Math.round(element.x)));
			properties.setProperty(module + ".y", Integer.toString((int) Math.round(element.y)));
			properties.setProperty(module + ".scale",
					Double.toString(Math.round(element.scale * 100.0D) / 100.0D));
		}

		// 同时写一份旧版的全局键：万一降级回旧构建，至少能读回显示开关与缩放。
		// 新版读取时只要看到 <模块>.* 就不会再走旧键，所以不会打架。
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

	/** 左下模块默认贴底高度。 */
	private static int bottomOf(String module) {
		return switch (module) {
			case DASH -> DASH_BOTTOM;
			case MP -> MP_BOTTOM;
			case CLOUD -> CLOUD_BOTTOM;
			case SUN_SWORD -> SUN_SWORD_BOTTOM;
			case FROST -> FROST_BOTTOM;
			default -> DASH_BOTTOM;
		};
	}

	/** 首次落位时用来夹进屏幕的估算宽度（真实宽度渲染时按文字重新算）。 */
	private static int defaultWidth(String module) {
		return switch (module) {
			case SKILLS, ARTIFACTS -> DEFAULT_SLOTS * DEFAULT_ICON_STEP;
			case INVULN -> DEFAULT_INVULN_WIDTH;
			default -> DEFAULT_PANEL_WIDTH;
		};
	}

	/** 首次落位时用来夹进屏幕的估算高度。 */
	private static int defaultHeight(String module) {
		return switch (module) {
			case SKILLS, ARTIFACTS, WEAPON -> DEFAULT_ROW_HEIGHT;
			case INVULN -> DEFAULT_INVULN_HEIGHT;
			default -> DEFAULT_PANEL_HEIGHT;
		};
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
