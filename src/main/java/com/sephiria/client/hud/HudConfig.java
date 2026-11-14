package com.sephiria.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD 布局配置：每个界面元素的位置（屏幕绝对坐标）和缩放，存在 config/sephiria-fantasy-hud.json。
 *
 * <p>首次使用时按各自的默认角落初始化——武器在右下、冲刺在左下——因为默认值依赖当前
 * 分辨率，不能在类加载时写死。之后位置就是绝对坐标，配置界面里拖动滑块直接改它。
 */
public final class HudConfig {
	/** 武器 UI（右下角）。 */
	public static final String WEAPON = "weapon";
	/** 冲刺 UI（左下角）。 */
	public static final String DASH = "dash";
	/** MP 条（左下角，默认在冲刺上方）。 */
	public static final String MP = "mp";

	/** 护盾（MP 正下方；护盾量 > 0 时显示）。 */
	public static final String SHIELD = "shield";

	/** 乌云容量条（左下角，默认在 MP 上方；只在乌云激活时显示）。 */
	public static final String CLOUD = "cloud";

	/** 太阳剑数量条（左下角，默认在乌云上方；只在太阳剑激活时显示）。 */
	public static final String SUN_SWORD = "sun_sword";

	/** 冰霜武具充能行（左下角，默认在太阳剑上方；带上那两件神器时才显示）。 */
	public static final String FROST = "frost";

	/** 无敌条（准星下方居中）。 */
	public static final String INVULN = "invuln";
	/** 无敌条的局部宽度：缩放时按它保持中心不动。 */
	public static final int INVULN_WIDTH = 60;

	/** 技能 HUD（武器 UI 上方；6 个神器技能栏的图标与冷却）。 */
	public static final String SKILLS = "skills";

	/** 神器 HUD（技能 HUD 与武器 UI 之间；有冷却 / 充能的神器图标）。 */
	public static final String ARTIFACTS = "artifacts";

	/** 经验加加（EXP++）点数（默认关闭）。内部键仍用 fatigue，兼容旧布局配置。 */
	public static final String FATIGUE = "fatigue";

	/** 全部可调的 HUD 模块，顺序即设置界面里的顺序；新增模块在这里登记。 */
	public static final List<String> MODULES = List.of(WEAPON, DASH, MP, SHIELD, CLOUD, SUN_SWORD, FROST, INVULN,
			SKILLS, ARTIFACTS, FATIGUE);

	/** 一个界面元素的位置与缩放。 */
	public static final class Element {
		public double x;
		public double y;
		public double scale = 1.0D;
		public boolean placed;
		public boolean visible = true;
		/** 是否每帧按屏幕宽度重新水平居中（无敌条默认如此；拖动 X 后关闭）。 */
		public boolean centered;

		/** 没落过位就按角落放好；center 表示"屏幕水平居中、准星下方一点"。 */
		void placeFrom(String corner, int guiWidth, int guiHeight) {
			if (this.placed) {
				return;
			}

			if ("center".equals(corner)) {
				// GUI 宽度为奇数时 x 会落在 .5；部分渲染路径会把它截断，看起来就偏左 1px。
				// 标记为 centered，渲染时再按实际屏幕宽度 / 缩放重算一次。
				this.centered = true;
				this.x = Math.ceil(guiWidth / 2.0D - 30.0D);
				this.y = guiHeight / 2.0D + 18.0D;
			}
			else if ("below_mp".equals(corner)) {
				// 护盾默认贴着 MP 条正下方（按 MP 的实际位置算，而不是写死屏幕底部）
				Element mp = element(MP);
				this.x = mp.x;
				this.y = mp.y + 24.0D;
			}
			else if ("right_of_dash".equals(corner)) {
				placeRightOfDash(this);
			}
			else {
				// 右下两行（技能 / 神器）最多 9 个图标，比武器面板宽：默认再往左挪一点，免得贴出屏幕
				this.x = switch (corner) {
					case "bottom_right_high", "bottom_right_higher" -> guiWidth - 174.0D;
					default -> corner.startsWith("bottom_right") ? guiWidth - 150.0D : 8.0D;
				};
				// 两列都从下往上排：武器 / 神器 / 技能各差一行高；左下角依次是冲刺、MP、乌云、
				// 太阳剑、冰霜武具
				this.y = guiHeight - (switch (corner) {
					case "bottom_left_highest3" -> 160.0D;
					case "bottom_left_highest2" -> 136.0D;
					case "bottom_left_highest" -> 112.0D;
					case "bottom_right_higher", "bottom_left_higher" -> 88.0D;
					case "bottom_right_high", "bottom_left_high" -> 64.0D;
					default -> 40.0D;
				});
			}

			this.placed = true;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** 冲刺 HUD 左右内边距合计（与 {@code SephiriaHud.PAD * 2} 对应）。 */
	private static final double PAD_FOR_DASH = 8.0D;
	private static final Map<String, String> CORNERS = Map.ofEntries(
			Map.entry(WEAPON, "bottom_right"),
			Map.entry(MP, "bottom_left_high"),
			Map.entry(SHIELD, "right_of_dash"),
			Map.entry(DASH, "bottom_left"),
			Map.entry(CLOUD, "bottom_left_higher"),
			Map.entry(SUN_SWORD, "bottom_left_highest"),
			Map.entry(FROST, "bottom_left_highest2"),
			Map.entry(SKILLS, "bottom_right_higher"),
			Map.entry(ARTIFACTS, "bottom_right_high"),
			Map.entry(INVULN, "center"),
			Map.entry(FATIGUE, "bottom_left_highest3"));
	private static final Map<String, Element> ELEMENTS = new LinkedHashMap<>();

	private static boolean loaded;

	private HudConfig() {
	}

	/** 把元素放到冲刺 HUD 右侧：按冲刺面板的实际字体宽度与缩放算，两边留 4px 间距。 */
	private static void placeRightOfDash(Element element) {
		Element dash = element(DASH);
		Minecraft client = Minecraft.getInstance();
		double dashWidth = 50.0D;

		if (client.font != null) {
			dashWidth = client.font.width(Component.translatable("ui.sephiria_fantasy.dash", "0", "0"))
					+ PAD_FOR_DASH;
		}

		element.x = dash.x + dashWidth * dash.scale + 4.0D;
		element.y = dash.y;
	}

	public static Element element(String key) {
		load();

		Element element = ELEMENTS.computeIfAbsent(key, k -> {
			Element created = new Element();

			if (FATIGUE.equals(k)) {
				created.visible = false;
			}

			return created;
		});
		Minecraft client = Minecraft.getInstance();

		if (client.getWindow() != null) {
			element.placeFrom(CORNERS.getOrDefault(key, "bottom_left"),
					client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
		}

		return element;
	}

	public static void load() {
		if (loaded) {
			return;
		}
		loaded = true;

		Path path = configPath();

		if (!Files.exists(path)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
				JsonObject object = entry.getValue().getAsJsonObject();
				Element element = new Element();
				element.x = object.get("x").getAsDouble();
				element.y = object.get("y").getAsDouble();
				element.scale = object.get("scale").getAsDouble();
				element.visible = !object.has("visible") || object.get("visible").getAsBoolean();
				// 旧配置没有 centered：无敌条默认居中（用户 2026-11-14 要求），其它模块保持原位
				element.centered = object.has("centered")
						? object.get("centered").getAsBoolean()
						: INVULN.equals(entry.getKey());
				element.placed = true;
				ELEMENTS.put(entry.getKey(), element);
			}

			// 旧版护盾 HUD 默认在 MP 正下方；用户 2026-11-14 改成冲刺右侧，启动时把
			// 「还停在旧默认位置」的那一份迁过去（手动拖过的位置不动）。
			Element shield = ELEMENTS.get(SHIELD);
			Element mp = ELEMENTS.get(MP);

			if (shield != null && mp != null
					&& Math.abs(shield.x - mp.x) < 0.5D
					&& Math.abs(shield.y - (mp.y + 24.0D)) < 0.5D) {
				placeRightOfDash(shield);
			}
		}
		catch (IOException | RuntimeException e) {
			// 配置坏了就退回默认布局，不让 HUD 因为一个文件读不动而崩掉
			ELEMENTS.clear();
		}
	}

	/**
	 * 统一调整：把所有 HUD 模块的缩放设成同一个值。
	 *
	 * <p>只改缩放、不动位置；设置界面里「统一调整」按钮用。
	 */
	public static void applyScaleToAll(double scale) {
		double clamped = Math.max(0.5D, Math.min(2.0D, scale));
		Element invuln = ELEMENTS.get(INVULN);
		double invulnCenter = invuln == null ? Double.NaN
				: invuln.x + INVULN_WIDTH * invuln.scale / 2.0D;

		for (String module : MODULES) {
			element(module).scale = clamped;
		}

		// 无敌条缩放时保持中心不动（centered 时渲染还会再按屏幕中心校一次）
		Element current = ELEMENTS.get(INVULN);

		if (current != null && !Double.isNaN(invulnCenter)) {
			current.x = invulnCenter - INVULN_WIDTH * clamped / 2.0D;
		}
	}

	/** 所有 HUD 缩放的算术平均，作为「统一调整」滑块的初始位置。 */
	public static double averageScale() {
		double total = 0.0D;

		for (String module : MODULES) {
			total += element(module).scale;
		}

		return total / MODULES.size();
	}

	/**
	 * 重置所有 HUD：回到默认位置、缩放 1.0、默认显示状态。
	 *
	 * <p>无敌条仍然保持居中（`centered` 由默认落位重新打开）。
	 */
	public static void resetAll() {
		ELEMENTS.clear();

		for (String module : MODULES) {
			element(module);
		}

		save();
	}

	public static void save() {
		JsonObject root = new JsonObject();

		for (Map.Entry<String, Element> entry : ELEMENTS.entrySet()) {
			JsonObject object = new JsonObject();
			object.addProperty("x", entry.getValue().x);
			object.addProperty("y", entry.getValue().y);
			object.addProperty("scale", entry.getValue().scale);
			object.addProperty("visible", entry.getValue().visible);
			object.addProperty("centered", entry.getValue().centered);
			root.add(entry.getKey(), object);
		}

		Path path = configPath();

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		}
		catch (IOException e) {
			// 写不进去只影响下次启动的布局，不影响本次游戏
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("sephiria-fantasy-hud.json");
	}
}
