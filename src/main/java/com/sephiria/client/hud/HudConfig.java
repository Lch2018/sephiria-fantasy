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
	private static boolean showSkills = true;
	private static boolean showArtifacts = true;
	private static int offsetX;
	private static int offsetY;

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
			showSkills = Boolean.parseBoolean(properties.getProperty("show_skills", "true"));
			showArtifacts = Boolean.parseBoolean(properties.getProperty("show_artifacts", "true"));
			offsetX = Integer.parseInt(properties.getProperty("offset_x", "0"));
			offsetY = Integer.parseInt(properties.getProperty("offset_y", "0"));
		} catch (IOException | NumberFormatException ignored) {
		}
	}

	public static void save() {
		Properties properties = new Properties();
		properties.setProperty("visible", Boolean.toString(visible));
		properties.setProperty("scale", Float.toString(scale));
		properties.setProperty("show_skills", Boolean.toString(showSkills));
		properties.setProperty("show_artifacts", Boolean.toString(showArtifacts));
		properties.setProperty("offset_x", Integer.toString(offsetX));
		properties.setProperty("offset_y", Integer.toString(offsetY));

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

	public static boolean showSkills() {
		return showSkills;
	}

	public static boolean showArtifacts() {
		return showArtifacts;
	}

	public static void toggleSkills() {
		showSkills = !showSkills;
		save();
	}

	public static void toggleArtifacts() {
		showArtifacts = !showArtifacts;
		save();
	}

	public static int offsetX() {
		return offsetX;
	}

	public static int offsetY() {
		return offsetY;
	}

	public static void setOffset(int x, int y) {
		offsetX = x;
		offsetY = y;
	}

	public static void cycleScale() {
		scale = scale >= 1.5F ? 1.0F : scale >= 1.25F ? 1.5F : 1.25F;
		save();
	}
}
