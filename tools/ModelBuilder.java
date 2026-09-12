import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small voxel model builder for the hand-designed weapons (as opposed to the art-derived
 * ones produced by Voxelizer).
 *
 * A model is a grid of half-unit voxels over the same 16x16x16 model box Minecraft items use.
 * Parts are painted with box() in the game's palette, mirrored for symmetry where that helps,
 * then emitted as merged cuboids with hidden faces dropped, plus a palette texture so faces
 * can never bleed. Preview.render can draw the result without launching the game.
 */
public class ModelBuilder {
	public static final int VOX = 32;          // 16 model units at half-unit resolution
	public static final int DEPTH = 16;        // depth range, centred on z = 8
	public static final double UNIT = 0.5;     // model units per voxel

	// palette lifted from the SEPHIRIA art
	public static final int OUTLINE = 0x000000;
	public static final int STEEL_LIGHT = 0xBDBECF;
	public static final int STEEL_MID = 0x7E87A7;
	public static final int STEEL_DARK = 0x4B4B64;
	public static final int NAVY = 0x37364C;
	public static final int WOOD = 0x705052;
	public static final int WOOD_DARK = 0x493843;
	public static final int WHITE = 0xFFFFFF;
	public static final int NEAR_BLACK = 0x222034;

	private final int[][][] voxels = new int[VOX][VOX][DEPTH];
	private final List<Voxelizer.Box> boxes = new ArrayList<>();

	public ModelBuilder() {
		for (int x = 0; x < VOX; x++) {
			for (int y = 0; y < VOX; y++) {
				java.util.Arrays.fill(voxels[x][y], -1);
			}
		}
	}

	/** paints a box in voxel coordinates, inclusive on both ends */
	public ModelBuilder box(int x1, int y1, int z1, int x2, int y2, int z2, int rgb) {
		for (int x = Math.max(0, x1); x <= Math.min(VOX - 1, x2); x++) {
			for (int y = Math.max(0, y1); y <= Math.min(VOX - 1, y2); y++) {
				for (int z = Math.max(0, z1); z <= Math.min(DEPTH - 1, z2); z++) {
					voxels[x][y][z] = rgb;
				}
			}
		}
		return this;
	}

	/** paints a box and mirrors it across the model centre line, for symmetric parts */
	public ModelBuilder boxMirrored(int x1, int y1, int z1, int x2, int y2, int z2, int rgb) {
		box(x1, y1, z1, x2, y2, z2, rgb);
		int mx1 = VOX - 1 - x2;
		int mx2 = VOX - 1 - x1;
		return box(mx1, y1, z1, mx2, y2, z2, rgb);
	}

	/** an oval plate in the XY plane, used for shields */
	public ModelBuilder disc(double cx, double cy, double rx, double ry, int z1, int z2, int rgb) {
		for (int x = 0; x < VOX; x++) {
			for (int y = 0; y < VOX; y++) {
				double dx = (x + 0.5 - cx) / rx;
				double dy = (y + 0.5 - cy) / ry;
				if (dx * dx + dy * dy <= 1.0) {
					box(x, y, z1, x, y, z2, rgb);
				}
			}
		}
		return this;
	}

	public int at(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= VOX || y >= VOX || z >= DEPTH) return -1;
		return voxels[x][y][z];
	}

	/**
	 * Emits the model: rectangles merged per colour and depth layer, hidden faces dropped,
	 * colours written to a palette texture. The preview path is optional (null = skip).
	 */
	public void write(Path modelPath, Path texturePath, String parent, String paletteTexture, Path previewPath) throws Exception {
		Map<Integer, Integer> palette = new LinkedHashMap<>();
		for (int y = 0; y < VOX; y++) {
			for (int x = 0; x < VOX; x++) {
				for (int z = 0; z < DEPTH; z++) {
					if (voxels[x][y][z] >= 0) palette.putIfAbsent(voxels[x][y][z], palette.size());
				}
			}
		}
		if (palette.size() > 16) throw new IllegalStateException("more than 16 colours: " + palette.size());

		int[][] top = new int[VOX][VOX]; // topmost painted colour per column, for the merge pass
		for (int y = 0; y < VOX; y++) {
			for (int x = 0; x < VOX; x++) {
				top[y][x] = -1;
				for (int z = DEPTH - 1; z >= 0; z--) {
					if (voxels[x][y][z] >= 0) {
						top[y][x] = voxels[x][y][z];
						break;
					}
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\t\"parent\": \"").append(parent).append("\",\n");
		sb.append("\t\"textures\": {\n\t\t\"0\": \"").append(paletteTexture).append("\",\n\t\t\"particle\": \"").append(paletteTexture).append("\"\n\t},\n");
		sb.append("\t\"elements\": [\n");

		List<String> elements = new ArrayList<>();
		boolean[][] used = new boolean[VOX][VOX];
		for (int y = VOX - 1; y >= 0; y--) {
			for (int x = 0; x < VOX; x++) {
				if (used[y][x] || top[y][x] < 0) continue;
				int colour = top[y][x];

				int w = 1;
				while (x + w < VOX && !used[y][x + w] && top[y][x + w] == colour) w++;

				int h = 1;
				outer:
				while (y - h >= 0) {
					for (int i = 0; i < w; i++) {
						if (used[y - h][x + i] || top[y - h][x + i] != colour) break outer;
					}
					h++;
				}
				for (int j = 0; j < h; j++) {
					for (int i = 0; i < w; i++) used[y - j][x + i] = true;
				}
				elements.add(element(x, y - h + 1, w, h, colour, palette.get(colour)));
			}
		}

		sb.append(String.join(",\n", elements)).append("\n\t]\n}\n");
		Files.createDirectories(modelPath.getParent());
		Files.writeString(modelPath, sb.toString(), StandardCharsets.UTF_8);

		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (Map.Entry<Integer, Integer> e : palette.entrySet()) {
			int cx = (e.getValue() % 4) * 16;
			int cy = (e.getValue() / 4) * 16;
			int argb = 0xFF000000 | e.getKey();
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) img.setRGB(cx + x, cy + y, argb);
			}
		}
		Files.createDirectories(texturePath.getParent());
		ImageIO.write(img, "png", texturePath.toFile());

		if (previewPath != null) {
			Preview.renderBoxes(boxes, previewPath, modelPath.getFileName().toString() + "  elements=" + elements.size());
		}
	}

	/**
	 * One cuboid: the column's painted voxels from the bottom-most to the top-most, so a part
	 * reads as a solid slab instead of a paper-thin plate.
	 */
	private String element(int x, int y, int w, int h, int colour, int slot) {
		int z1 = DEPTH, z2 = -1;
		for (int xx = x; xx < x + w; xx++) {
			for (int yy = y; yy < y + h; yy++) {
				for (int z = 0; z < DEPTH; z++) {
					if (voxels[xx][yy][z] >= 0) {
						z1 = Math.min(z1, z);
						z2 = Math.max(z2, z);
					}
				}
			}
		}
		if (z2 < 0) return "";

		double fx1 = x * UNIT, fx2 = (x + w) * UNIT;
		double fy1 = y * UNIT, fy2 = (y + h) * UNIT;
		double fz1 = 8 + (z1 - DEPTH / 2.0) * UNIT;
		double fz2 = 8 + (z2 + 1 - DEPTH / 2.0) * UNIT;
		boxes.add(new Voxelizer.Box(fx1, fy1, fz1, fx2, fy2, fz2, colour));

		double[] uv = paletteUv(slot);
		String face = "\"%s\": { \"texture\": \"#0\", \"uv\": [" + f(uv[0]) + ", " + f(uv[1]) + ", " + f(uv[2]) + ", " + f(uv[3]) + "] }";
		List<String> faces = new ArrayList<>();
		for (String side : new String[] { "north", "south", "east", "west", "up", "down" }) {
			faces.add(String.format(face, side));
		}

		return "\t\t{ \"from\": [" + f(fx1) + ", " + f(fy1) + ", " + f(fz1) + "], \"to\": [" + f(fx2) + ", " + f(fy2) + ", " + f(fz2)
				+ "], \"faces\": { " + String.join(", ", faces) + " } }";
	}

	private static double[] paletteUv(int slot) {
		int cx = slot % 4;
		int cy = slot / 4;
		return new double[] { (cx * 16 + 4) / 4.0, (cy * 16 + 4) / 4.0, (cx * 16 + 12) / 4.0, (cy * 16 + 12) / 4.0 };
	}

	private static String f(double v) {
		if (Math.abs(v - Math.rint(v)) < 1e-6) return String.valueOf((int) Math.rint(v));
		return String.format(java.util.Locale.ROOT, "%.4f", v);
	}
}
