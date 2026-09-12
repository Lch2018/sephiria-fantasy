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
 * Turns a SEPHIRIA weapon sprite into a voxel-style Minecraft model.
 *
 * The sprite is sampled onto a square voxel grid covering the same 16x16 model footprint the
 * flat sprite used, so the 3D model lines up exactly with the 2D icon. Depth comes from the
 * material: steel blades and highlights stay thin, wood and leather parts are a little
 * thicker, which keeps the features readable from the side without turning into noise.
 *
 * Two cleanups make the result look like a weapon rather than a relief:
 *  - the bold black outline of the art is filled with the nearest material colour, because it
 *    is a drawing convention, not a feature (the silhouette is untouched);
 *  - disconnected parts (the sword versus the shield behind it) are placed on their own depth
 *    layers so overlapping pieces stay readable.
 *
 * Voxels are merged into axis-aligned rectangles (same colour and depth), each becoming one
 * cuboid that spans its full depth. Colours live in a small palette texture so the faces can
 * never bleed into each other.
 *
 * Usage: java Voxelizer <sprite.png> <out_model.json> <out_texture.png> <grid> <modelName> <parentModel> <previewOut.png>
 */
public final class Voxelizer {
	private static final int SOURCE = 128;
	private static final int ALPHA_CUTOFF = 128;
	private static final String INDENT = "\t";

	/** how far a non-main part sits behind the main one, in model units */
	private static final double LAYER_SEPARATION = 1.6;

	public static void main(String[] args) throws Exception {
		if (args.length < 7) {
			System.err.println("usage: Voxelizer <sprite.png> <out_model.json> <out_texture.png> <grid> <modelName> <parentModel> <previewOut.png>");
			return;
		}

		Path spritePath = Path.of(args[0]);
		Path modelPath = Path.of(args[1]);
		Path texturePath = Path.of(args[2]);
		int grid = Integer.parseInt(args[3]);
		String modelName = args[4];
		String parent = args[5];
		int partIndex = args.length > 7 ? Integer.parseInt(args[7]) : -1;

		BufferedImage sprite = ImageIO.read(spritePath.toFile());
		if (sprite == null) throw new IllegalStateException("cannot read " + spritePath);
		if (sprite.getWidth() != SOURCE || sprite.getHeight() != SOURCE) {
			throw new IllegalStateException("expected a " + SOURCE + "x" + SOURCE + " sprite, got " + sprite.getWidth() + "x" + sprite.getHeight());
		}
		if (SOURCE % grid != 0) throw new IllegalArgumentException("grid must divide " + SOURCE);

		int cell = SOURCE / grid;
		int[][] rgb = new int[grid][grid];         // -1 = transparent
		double[][] depth = new double[grid][grid]; // thickness in model units
		double[][] zOffset = new double[grid][grid]; // depth layer offset

		for (int gy = 0; gy < grid; gy++) {
			for (int gx = 0; gx < grid; gx++) {
				Map<Integer, Integer> counts = new LinkedHashMap<>();
				int opaque = 0;
				for (int sy = 0; sy < cell; sy++) {
					for (int sx = 0; sx < cell; sx++) {
						int argb = sprite.getRGB(gx * cell + sx, gy * cell + sy);
						if (((argb >>> 24) & 0xFF) < ALPHA_CUTOFF) continue;
						opaque++;
						counts.merge(argb & 0xFFFFFF, 1, Integer::sum);
					}
				}

				if (opaque * 2 < cell * cell) {
					rgb[gy][gx] = -1;
					continue;
				}

				int bestKey = 0;
				int bestCount = -1;
				for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
					if (e.getValue() > bestCount) {
						bestCount = e.getValue();
						bestKey = e.getKey();
					}
				}
				rgb[gy][gx] = bestKey;
			}
		}

		boolean[][] wasOutline = new boolean[grid][grid];
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				wasOutline[y][x] = rgb[y][x] >= 0 && isOutline(rgb[y][x]);
			}
		}
		absorbOutline(rgb, grid);
		keepOnlyPart(rgb, grid, partIndex, wasOutline);
		zOffset = assignLayers(rgb, grid);

		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0) depth[y][x] = depthFor(rgb[y][x]);
			}
		}

		Map<Integer, Integer> paletteIndex = new LinkedHashMap<>();
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0) paletteIndex.putIfAbsent(rgb[y][x], paletteIndex.size());
			}
		}
		if (paletteIndex.size() > 16) throw new IllegalStateException("more than 16 colours: " + paletteIndex.size());

		boolean[][] used = new boolean[grid][grid];
		List<int[]> rects = new ArrayList<>();

		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (used[y][x] || rgb[y][x] < 0) continue;
				int colour = rgb[y][x];

				int w = 1;
				while (x + w < grid && !used[y][x + w] && rgb[y][x + w] == colour) w++;

				int h = 1;
				outer:
				while (y + h < grid) {
					for (int i = 0; i < w; i++) {
						if (used[y + h][x + i] || rgb[y + h][x + i] != colour) break outer;
					}
					h++;
				}

				for (int j = 0; j < h; j++) {
					for (int i = 0; i < w; i++) used[y + j][x + i] = true;
				}
				rects.add(new int[] { x, y, w, h });
			}
		}

		String json = buildModel(grid, rgb, depth, zOffset, paletteIndex, rects, modelName, parent);
		Files.createDirectories(modelPath.getParent());
		Files.writeString(modelPath, json, StandardCharsets.UTF_8);
		writePalette(texturePath, paletteIndex);

		String stats = "grid=" + grid + " elements=" + rects.size() + " colours=" + paletteIndex.size();
		Path statsPath = Path.of(args[6]).resolveSibling(Path.of(args[6]).getFileName().toString().replace("_preview.png", ".txt"));
		Files.writeString(statsPath, stats + "\n", StandardCharsets.UTF_8);
		Preview.render(grid, rgb, depth, zOffset, rects, spritePath, Path.of(args[6]), modelName + "  " + stats);

		// 1:1 front render + pixel comparison against the sprite, to catch feature drift
		String compare = frontCompare(grid, rgb, zOffset, rects, sprite);
		Files.writeString(Path.of(args[6]).resolveSibling(Path.of(args[6]).getFileName().toString().replace("_preview.png", ".txt")), compare + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
		System.out.println(modelName + "  " + stats + "  " + compare);
	}

	/**
	 * Renders the model as seen from the front (nearest voxel wins) and reports how many pixels
	 * still carry a colour that matches the sprite. The outline is filled in on purpose, so a
	 * pixel counts as matching when it shares the sprite's colour or lies inside the outline.
	 */
	private static String frontCompare(int grid, int[][] rgb, double[][] zOffset, List<int[]> rects, BufferedImage sprite) throws Exception {
		int[] front = new int[grid * grid];
		java.util.Arrays.fill(front, -1);

		// paint far-to-near so nearer voxels win
		Integer[] order = new Integer[rects.size()];
		for (int i = 0; i < order.length; i++) order[i] = i;
		java.util.Arrays.sort(order, (a, b) -> {
			int[] ra = rects.get(a);
			int[] rb = rects.get(b);
			return Double.compare(zOffset[rb[1]][rb[0]], zOffset[ra[1]][ra[0]]);
		});

		for (int idx : order) {
			int[] r = rects.get(idx);
			int colour = rgb[r[1]][r[0]];
			for (int y = r[1]; y < r[1] + r[3]; y++) {
				for (int x = r[0]; x < r[0] + r[2]; x++) {
					front[y * grid + x] = colour;
				}
			}
		}

		int cell = SOURCE / grid;
		int spritePixels = 0, matched = 0, outlinePixels = 0;
		for (int py = 0; py < SOURCE; py++) {
			for (int px = 0; px < SOURCE; px++) {
				int argb = sprite.getRGB(px, py);
				int modelColour = front[(py / cell) * grid + (px / cell)];
				boolean spriteOpaque = ((argb >>> 24) & 0xFF) >= ALPHA_CUTOFF;

				if (spriteOpaque != (modelColour >= 0)) {
					spritePixels++; // silhouette mismatch
					continue;
				}
				if (!spriteOpaque) continue;

				spritePixels++;
				int spriteColour = argb & 0xFFFFFF;
				if (spriteColour == modelColour) {
					matched++;
				} else if (isOutline(spriteColour)) {
					outlinePixels++;
					matched++; // filled outline, still counts as a match
				}
			}
		}

		BufferedImage img = new BufferedImage(grid, grid, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				int c = front[y * grid + x];
				img.setRGB(x, y, c < 0 ? 0 : (0xFF000000 | c));
			}
		}
		ImageIO.write(img, "png", Path.of(System.getProperty("java.io.tmpdir"), "front_" + grid + ".png").toFile());

		double pct = spritePixels == 0 ? 0 : 100.0 * matched / spritePixels;
		return "front match " + String.format(java.util.Locale.ROOT, "%.2f", pct) + "% (outline-filled " + outlinePixels + " px of " + spritePixels + ")";
	}

	record Box(double x1, double y1, double z1, double x2, double y2, double z2, int rgb) {
	}

	static List<Box> boxes(int grid, int[][] rgbGrid, double[][] depthGrid, double[][] zOffsetGrid, List<int[]> rects) {
		double unit = 16.0 / grid;
		List<Box> out = new ArrayList<>();
		for (int[] r : rects) {
			int x = r[0], y = r[1], w = r[2], h = r[3];
			double centre = 8.0 + zOffsetGrid[y][x];
			double d = depthGrid[y][x];
			out.add(new Box(
					x * unit,
					16.0 - (y + h) * unit,
					centre - d / 2.0,
					(x + w) * unit,
					16.0 - y * unit,
					centre + d / 2.0,
					rgbGrid[y][x]));
		}
		return out;
	}

	/** depth in model units; a shallow relief so colour-region borders do not turn into steps */
	private static double depthFor(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;

		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		boolean grey = (max - min) <= 24;

		if (grey && max >= 220) return 1.4;
		if (grey && max >= 100) return 1.4;
		if (max <= 24) return 1.4;
		if (r >= g && g >= b && (r - b) >= 20) return 2.4; // wood / leather
		return 1.8;                                        // dark blues and everything else
	}

	private static boolean isOutline(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return Math.max(r, Math.max(g, b)) <= 24;
	}

	/**
	 * The art is drawn with a bold pure-black outline. Filling all of it with material colours
	 * would leave the model crisp but the art's crisp edges would be gone; keeping all of it
	 * would turn the weapon into a black slab. So only the interior outline is filled and a
	 * thin black rim stays at the silhouette, which is how the art reads in the first place.
	 */
	private static void absorbOutline(int[][] rgb, int grid) {
		boolean[][] solid = new boolean[grid][grid];
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				solid[y][x] = rgb[y][x] >= 0;
			}
		}

		// erode: a voxel is "interior" when the whole RIM x RIM neighbourhood is solid
		final int rim = 3;
		boolean[][] interior = new boolean[grid][grid];
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (!solid[y][x]) continue;
				boolean allSolid = true;
				for (int dy = -rim; dy <= rim && allSolid; dy++) {
					for (int dx = -rim; dx <= rim; dx++) {
						int nx = x + dx;
						int ny = y + dy;
						if (nx < 0 || ny < 0 || nx >= grid || ny >= grid || !solid[ny][nx]) {
							allSolid = false;
							break;
						}
					}
				}
				interior[y][x] = allSolid;
			}
		}

		int[][] queue = new int[grid * grid][2];
		int head = 0, tail = 0;
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0 && !isOutline(rgb[y][x])) {
					queue[tail][0] = x;
					queue[tail][1] = y;
					tail++;
				}
			}
		}
		if (tail == 0) return;

		int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
		while (head < tail) {
			int x = queue[head][0];
			int y = queue[head][1];
			head++;
			int colour = rgb[y][x];

			for (int[] s : steps) {
				int nx = x + s[0];
				int ny = y + s[1];
				if (nx < 0 || ny < 0 || nx >= grid || ny >= grid) continue;
				if (rgb[ny][nx] < 0 || !isOutline(rgb[ny][nx])) continue;
				if (!interior[ny][nx]) continue; // keep the silhouette rim black
				rgb[ny][nx] = colour;
				queue[tail][0] = nx;
				queue[tail][1] = ny;
				tail++;
			}
		}
	}

	/**
	 * Connected-component pass: the part containing the highest voxel is treated as the weapon
	 * itself and everything else is pushed behind it, so a shield stops merging into its sword.
	 */
	private static double[][] assignLayers(int[][] rgb, int grid) {
		int[][] label = new int[grid][grid];
		for (int[] row : label) java.util.Arrays.fill(row, -1);

		List<int[]> components = new ArrayList<>(); // {topY, size, id}
		int next = 0;
		int[][] queue = new int[grid * grid][2];
		int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] < 0 || label[y][x] >= 0) continue;

				int head = 0, tail = 0;
				queue[tail][0] = x;
				queue[tail][1] = y;
				tail++;
				label[y][x] = next;
				int topY = y;
				int size = 0;

				while (head < tail) {
					int cx = queue[head][0];
					int cy = queue[head][1];
					head++;
					size++;
					if (cy < topY) topY = cy;

					for (int[] s : steps) {
						int nx = cx + s[0];
						int ny = cy + s[1];
						if (nx < 0 || ny < 0 || nx >= grid || ny >= grid) continue;
						if (rgb[ny][nx] < 0 || label[ny][nx] >= 0) continue;
						label[ny][nx] = next;
						queue[tail][0] = nx;
						queue[tail][1] = ny;
						tail++;
					}
				}

				components.add(new int[] { topY, size, next });
				next++;
			}
		}

		double[][] offset = new double[grid][grid];
		if (components.isEmpty()) return offset;

		// main part = the one reaching highest up the sprite (the blade tip / weapon head)
		components.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
		int mainId = components.get(0)[2];

		// measure the main part's centre along z so the neighbours sit clearly behind it
		boolean[][] isBehind = new boolean[grid][grid];
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				isBehind[y][x] = label[y][x] >= 0 && label[y][x] != mainId;
			}
		}

		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] < 0) continue;
				offset[y][x] = isBehind[y][x] ? 0.0 : LAYER_SEPARATION / 2.0;
			}
		}
		// keep the overall model centred on z = 8
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0) offset[y][x] -= LAYER_SEPARATION / 4.0;
			}
		}
		return offset;
	}

	private static String buildModel(int grid, int[][] rgb, double[][] depth, double[][] zOffset, Map<Integer, Integer> palette,
			List<int[]> rects, String modelName, String parent) {
		double unit = 16.0 / grid;
		String paletteTex = "sephiria:item/" + modelName.replace("_in_hand", "_3d");
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append(INDENT).append("\"parent\": \"").append(parent).append("\",\n");
		sb.append(INDENT).append("\"textures\": {\n");
		sb.append(INDENT).append(INDENT).append("\"0\": \"").append(paletteTex).append("\",\n");
		sb.append(INDENT).append(INDENT).append("\"particle\": \"").append(paletteTex).append("\"\n");
		sb.append(INDENT).append("},\n");
		sb.append(INDENT).append("\"elements\": [\n");

		for (int i = 0; i < rects.size(); i++) {
			int[] r = rects.get(i);
			int x = r[0], y = r[1], w = r[2], h = r[3];
			int colour = rgb[y][x];
			int slot = palette.get(colour);
			double d = depth[y][x];
			double centre = 8.0 + zOffset[y][x];
			double z1 = centre - d / 2.0;
			double z2 = centre + d / 2.0;

			double x1 = x * unit;
			double x2 = (x + w) * unit;
			double y1 = 16.0 - (y + h) * unit;
			double y2 = 16.0 - y * unit;

			double[] uv = paletteUv(slot);

			sb.append(INDENT).append(INDENT).append("{ \"from\": [").append(f(x1)).append(", ").append(f(y1)).append(", ").append(f(z1))
					.append("], \"to\": [").append(f(x2)).append(", ").append(f(y2)).append(", ").append(f(z2)).append("], \"faces\": {");

			List<String> faces = new ArrayList<>();
			if (!sameColour(rgb, x, y - 1, colour)) faces.add(face("north", uv));
			if (!sameColour(rgb, x, y + h, colour)) faces.add(face("south", uv));
			if (!sameColour(rgb, x - 1, y, colour)) faces.add(face("west", uv));
			if (!sameColour(rgb, x + w, y, colour)) faces.add(face("east", uv));
			faces.add(face("up", uv));
			faces.add(face("down", uv));

			sb.append(String.join(", ", faces)).append("} }");
			sb.append(i + 1 < rects.size() ? ",\n" : "\n");
		}

		sb.append(INDENT).append("]\n");
		sb.append("}\n");
		return sb.toString();
	}

	private static boolean sameColour(int[][] rgb, int x, int y, int colour) {
		int n = rgb.length;
		if (x < 0 || y < 0 || x >= n || y >= n) return false;
		return rgb[y][x] == colour;
	}

	private static String face(String name, double[] uv) {
		return "\"" + name + "\": { \"texture\": \"#0\", \"uv\": [" + f(uv[0]) + ", " + f(uv[1]) + ", " + f(uv[2]) + ", " + f(uv[3]) + "] }";
	}

	private static double[] paletteUv(int slot) {
		int cx = slot % 4;
		int cy = slot / 4;
		double u1 = (cx * 16 + 4) / 4.0;
		double v1 = (cy * 16 + 4) / 4.0;
		double u2 = (cx * 16 + 12) / 4.0;
		double v2 = (cy * 16 + 12) / 4.0;
		return new double[] { u1, v1, u2, v2 };
	}

	private static void writePalette(Path path, Map<Integer, Integer> palette) throws Exception {
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (Map.Entry<Integer, Integer> e : palette.entrySet()) {
			int rgb = e.getKey();
			int slot = e.getValue();
			int cx = (slot % 4) * 16;
			int cy = (slot / 4) * 16;
			int argb = 0xFF000000 | rgb;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					img.setRGB(cx + x, cy + y, argb);
				}
			}
		}
		Files.createDirectories(path.getParent());
		ImageIO.write(img, "png", path.toFile());
	}

	/**
	 * Keeps only the i-th largest part (0 = largest); -1 keeps everything.
	 *
	 * Parts are the connected regions of real material. The black outline is deliberately not
	 * allowed to bridge them, otherwise the sword and the shield of a single sprite would count
	 * as one piece because their contours touch. Outline pixels are then attached to whichever
	 * material region is nearest, so every part keeps its own share of the contour.
	 */
	private static void keepOnlyPart(int[][] rgb, int grid, int partIndex, boolean[][] wasOutline) {
		if (partIndex < 0) return;

		int[][] label = new int[grid][grid];
		for (int[] row : label) java.util.Arrays.fill(row, -1);
		List<int[]> sizes = new ArrayList<>();
		int next = 0;
		int[][] queue = new int[grid * grid][2];
		int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		// 1) connected material regions
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] < 0 || wasOutline[y][x] || label[y][x] >= 0) continue;
				int head = 0, tail = 0;
				queue[tail][0] = x;
				queue[tail][1] = y;
				tail++;
				label[y][x] = next;
				int size = 0;
				while (head < tail) {
					int cx = queue[head][0];
					int cy = queue[head][1];
					head++;
					size++;
					for (int[] s : steps) {
						int nx = cx + s[0];
						int ny = cy + s[1];
						if (nx < 0 || ny < 0 || nx >= grid || ny >= grid) continue;
						if (rgb[ny][nx] < 0 || wasOutline[ny][nx] || label[ny][nx] >= 0) continue;
						label[ny][nx] = next;
						queue[tail][0] = nx;
						queue[tail][1] = ny;
						tail++;
					}
				}
				sizes.add(new int[] { size, next });
				next++;
			}
		}

		if (sizes.isEmpty()) return;
		sizes.sort((a, b) -> Integer.compare(b[0], a[0]));

		// 2) grow the regions into the outline, nearest region wins
		int head = 0, tail = 0;
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0 && label[y][x] >= 0) {
					queue[tail][0] = x;
					queue[tail][1] = y;
					tail++;
				}
			}
		}
		while (head < tail) {
			int cx = queue[head][0];
			int cy = queue[head][1];
			head++;
			int id = label[cy][cx];
			for (int[] s : steps) {
				int nx = cx + s[0];
				int ny = cy + s[1];
				if (nx < 0 || ny < 0 || nx >= grid || ny >= grid) continue;
				if (rgb[ny][nx] < 0 || label[ny][nx] >= 0) continue;
				label[ny][nx] = id;
				queue[tail][0] = nx;
				queue[tail][1] = ny;
				tail++;
			}
		}

		if (partIndex >= sizes.size()) {
			throw new IllegalStateException("requested part " + partIndex + " but the sprite has only " + sizes.size() + " parts");
		}
		int keep = sizes.get(partIndex)[1];

		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				if (rgb[y][x] >= 0 && label[y][x] != keep) rgb[y][x] = -1;
			}
		}
	}

	private static String f(double v) {
		if (Math.abs(v - Math.rint(v)) < 1e-6) return String.valueOf((int) Math.rint(v));
		return String.format(java.util.Locale.ROOT, "%.4f", v);
	}
}
