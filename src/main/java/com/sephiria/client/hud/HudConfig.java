package com.sephiria.client.hud;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** HUD 的持久化设置：显示开关与缩放（1.0 / 1.25 / 1.5）。 */
public final class HudConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir()
			.resolve("sephiria-fantasy-hud.properties");
	private static boolean visible = true;
	private static float scale = 1.0F;

	private HudConfig() {
	}

	public static void load() {
		if (!Files.isRegularFile(FILE)) {
			return;
		}

		Properties properties = new Properties();

		try (InputStream input = Files.newInputStream(FILE)) {
			properties.load(input);
			visible = Boolean.parseBoolean(properties.getProperty("visible", "true"));
			scale = Float.parseFloat(properties.getProperty("scale", "1.0"));
		} catch (IOException | NumberFormatException ignored) {
		}
	}

	public static void save() {
		Properties properties = new Properties();
		properties.setProperty("visible", Boolean.toString(visible));
		properties.setProperty("scale", Float.toString(scale));

		try {
			Files.createDirectories(FILE.getParent());

			try (OutputStream output = Files.newOutputStream(FILE)) {
				properties.store(output, "Sephiria HUD");
			}
		} catch (IOException ignored) {
		}
	}

	public static boolean visible() {
		return visible;
	}

	public static void toggleVisible() {
		visible = !visible;
		save();
	}

	public static float scale() {
		return scale;
	}

	public static void cycleScale() {
		scale = scale >= 1.5F ? 1.0F : scale >= 1.25F ? 1.5F : 1.25F;
		save();
	}
}
