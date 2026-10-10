package com.sephiria.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

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

	/** 乌云容量条（左下角，默认在 MP 上方；只在乌云激活时显示）。 */
	public static final String CLOUD = "cloud";

	/** 太阳剑数量条（左下角，默认在乌云上方；只在太阳剑激活时显示）。 */
	public static final String SUN_SWORD = "sun_sword";

	/** 无敌条（准星下方居中）。 */
	public static final String INVULN = "invuln";

	/** 全部可调的 HUD 模块，顺序即设置界面里的顺序；新增模块在这里登记。 */
	public static final List<String> MODULES = List.of(WEAPON, DASH, MP, CLOUD, SUN_SWORD, INVULN);

	/** 一个界面元素的位置与缩放。 */
	public static final class Element {
		public double x;
		public double y;
		public double scale = 1.0D;
		public boolean placed;

		/** 没落过位就按角落放好；center 表示"屏幕水平居中、准星下方一点"。 */
		void placeFrom(String corner, int guiWidth, int guiHeight) {
			if (this.placed) {
				return;
			}

			if ("center".equals(corner)) {
				this.x = guiWidth / 2.0D - 30.0D;
				this.y = guiHeight / 2.0D + 18.0D;
			}
		else {
			this.x = "bottom_right".equals(corner) ? guiWidth - 150.0D : 8.0D;
			// 左下角一列从下往上依次是：冲刺、MP、乌云、太阳剑，各差一行高
			this.y = guiHeight - ("bottom_left_highest".equals(corner) ? 112.0D
					: "bottom_left_higher".equals(corner) ? 88.0D
					: "bottom_left_high".equals(corner) ? 64.0D : 40.0D);
		}

			this.placed = true;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, String> CORNERS = Map.of(
			WEAPON, "bottom_right",
			MP, "bottom_left_high",
			DASH, "bottom_left",
			CLOUD, "bottom_left_higher",
			SUN_SWORD, "bottom_left_highest",
			INVULN, "center");
	private static final Map<String, Element> ELEMENTS = new LinkedHashMap<>();

	private static boolean loaded;

	private HudConfig() {
	}

	public static Element element(String key) {
		load();

		Element element = ELEMENTS.computeIfAbsent(key, k -> new Element());
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
				element.placed = true;
				ELEMENTS.put(entry.getKey(), element);
			}
		}
		catch (IOException | RuntimeException e) {
			// 配置坏了就退回默认布局，不让 HUD 因为一个文件读不动而崩掉
			ELEMENTS.clear();
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();

		for (Map.Entry<String, Element> entry : ELEMENTS.entrySet()) {
			JsonObject object = new JsonObject();
			object.addProperty("x", entry.getValue().x);
			object.addProperty("y", entry.getValue().y);
			object.addProperty("scale", entry.getValue().scale);
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
