package com.sephiria.client.damage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 「赛菲莉娅 Mod 设置 → 伤害显示」的配置，存在 config/sephiria-fantasy-damage.json。
 *
 * <p>只影响装了 ToroHealth 时、由本模组伤害生成的浮动数字；ToroHealth 不在时这些值没有效果。
 */
public final class DamageDisplayConfig {
	public static final double BRIGHTNESS_MIN = 0.25D;
	public static final double BRIGHTNESS_MAX = 1.50D;
	public static final double SIZE_MIN = 0.50D;
	public static final double SIZE_MAX = 2.00D;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("sephiria-fantasy-damage.json");

	private static boolean loaded;
	private static double brightness = 1.0D;
	private static double size = 1.0D;
	private static boolean preTax;

	private DamageDisplayConfig() {
	}

	public static double brightness() {
		load();
		return brightness;
	}

	public static double size() {
		load();
		return size;
	}

	public static boolean preTax() {
		load();
		return preTax;
	}

	public static void setBrightness(double value) {
		load();
		brightness = clamp(value, BRIGHTNESS_MIN, BRIGHTNESS_MAX);
	}

	public static void setSize(double value) {
		load();
		size = clamp(value, SIZE_MIN, SIZE_MAX);
	}

	public static void setPreTax(boolean value) {
		load();
		preTax = value;
	}

	public static void load() {
		if (loaded) {
			return;
		}

		loaded = true;

		if (!Files.exists(PATH)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			brightness = clamp(root.has("brightness") ? root.get("brightness").getAsDouble() : 1.0D,
					BRIGHTNESS_MIN, BRIGHTNESS_MAX);
			size = clamp(root.has("size") ? root.get("size").getAsDouble() : 1.0D,
					SIZE_MIN, SIZE_MAX);
			preTax = root.has("pre_tax") && root.get("pre_tax").getAsBoolean();
		}
		catch (IOException | RuntimeException exception) {
			// 配置坏了就退回默认值，不让伤害数字因为一个文件读不动而崩掉
			brightness = 1.0D;
			size = 1.0D;
			preTax = false;
		}
	}

	public static void save() {
		load();
		JsonObject root = new JsonObject();
		root.addProperty("brightness", brightness);
		root.addProperty("size", size);
		root.addProperty("pre_tax", preTax);

		try {
			Files.createDirectories(PATH.getParent());

			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		}
		catch (IOException exception) {
			// 写不进去只影响下次启动的设置，不影响本次游戏
		}
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
