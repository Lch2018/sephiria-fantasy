import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Offline renderer for the generated voxel models, three panels side by side:
 * the original sprite, the model seen from the front (must match the sprite) and an
 * isometric view (shows the 3D structure). Lets the look be judged without launching the game.
 */
final class Preview {
	private static final double YAW = Math.toRadians(-38);
	private static final double PITCH = Math.toRadians(22);
	private static final int PANEL = 384;
	private static final int GAP = 20;
	private static final int HEAD = 34;
	private static final int MARGIN = 20;

	private Preview() {
	}

	static void render(int grid, int[][] rgb, double[][] depth, double[][] zOffset, List<int[]> rects,
			Path spritePath, Path out, String caption) throws Exception {
		List<Voxelizer.Box> boxes = Voxelizer.boxes(grid, rgb, depth, zOffset, rects);

		int width = MARGIN + (PANEL + GAP) * 3;
		int height = HEAD + PANEL + MARGIN;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setColor(new Color(198, 198, 198));
		g.fillRect(0, 0, width, height);

		g.setColor(Color.BLACK);
		g.setFont(new Font("Consolas", Font.BOLD, 14));
		String[] titles = { "sprite (source art)", "model, front view", "model, isometric" };
		for (int i = 0; i < 3; i++) {
			g.drawString(titles[i], MARGIN + i * (PANEL + GAP), HEAD - 12);
		}
		g.drawString(caption, MARGIN, 20);

		// panel 1: the sprite at the same scale the front view uses
		BufferedImage sprite = ImageIO.read(spritePath.toFile());
		g.drawImage(sprite, MARGIN, HEAD, PANEL, PANEL, null);

		// panel 2: front view (orthographic, far boxes first)
		drawFront(g, boxes, MARGIN + PANEL + GAP, HEAD);

		// panel 3: isometric
		drawIsometric(g, boxes, MARGIN + (PANEL + GAP) * 2, HEAD);

		g.dispose();
		ImageIO.write(img, "png", out.toFile());
	}

	/** two-panel view (front + isometric) for hand-designed models that have no source sprite */
	static void renderBoxes(List<Voxelizer.Box> boxes, Path out, String caption) throws Exception {
		int width = MARGIN + (PANEL + GAP) * 2;
		int height = HEAD + PANEL + MARGIN;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(Color.BLACK);
		g.setFont(new Font("Consolas", Font.BOLD, 14));
		g.drawString(caption, MARGIN, 20);
		g.drawString("front", MARGIN, HEAD - 12);
		g.drawString("isometric", MARGIN + PANEL + GAP, HEAD - 12);

		drawFront(g, boxes, MARGIN, HEAD);
		drawIsometric(g, boxes, MARGIN + PANEL + GAP, HEAD);

		g.dispose();
		ImageIO.write(img, "png", out.toFile());
	}

	private static void drawFront(Graphics2D g, List<Voxelizer.Box> boxes, int offX, int offY) {
		double scale = PANEL / 16.0;
		// full white-ish backdrop so transparent areas are obvious
		g.setColor(new Color(230, 230, 230));
		g.fillRect(offX, offY, PANEL, PANEL);

		List<Voxelizer.Box> sorted = new ArrayList<>(boxes);
		sorted.sort(Comparator.comparingDouble(b -> (b.z1() + b.z2()) / 2.0));

		for (Voxelizer.Box b : sorted) {
			int x1 = (int) Math.round(offX + b.x1() * scale);
			int x2 = (int) Math.round(offX + b.x2() * scale);
			int y1 = (int) Math.round(offY + (16 - b.y2()) * scale);
			int y2 = (int) Math.round(offY + (16 - b.y1()) * scale);
			g.setColor(new Color(b.rgb()));
			g.fillRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
		}
	}

	private static void drawIsometric(Graphics2D g, List<Voxelizer.Box> boxes, int offX, int offY) {
		g.setColor(new Color(230, 230, 230));
		g.fillRect(offX, offY, PANEL, PANEL);

		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (Voxelizer.Box b : boxes) {
			for (double[] c : corners(b)) {
				double[] p = project(c);
				minX = Math.min(minX, p[0]);
				maxX = Math.max(maxX, p[0]);
				minY = Math.min(minY, p[1]);
				maxY = Math.max(maxY, p[1]);
			}
		}
		double scale = Math.min((PANEL - 30.0) / Math.max(1e-6, maxX - minX), (PANEL - 30.0) / Math.max(1e-6, maxY - minY));
		double ox = offX + 15 - minX * scale + ((PANEL - 30.0) - (maxX - minX) * scale) / 2.0;
		double oy = offY + 15 - minY * scale + ((PANEL - 30.0) - (maxY - minY) * scale) / 2.0;

		List<Voxelizer.Box> sorted = new ArrayList<>(boxes);
		sorted.sort(Comparator.comparingDouble(Preview::depthOf));

		for (Voxelizer.Box b : sorted) {
			double[][] c = corners(b);
			double[][] p = new double[8][];
			for (int i = 0; i < 8; i++) {
				double[] q = project(c[i]);
				p[i] = new double[] { q[0] * scale + ox, q[1] * scale + oy };
			}
			Color base = new Color(b.rgb());
			drawFace(g, p, new int[] { 0, 1, 5, 4 }, shade(base, 1.05));
			drawFace(g, p, new int[] { 3, 2, 6, 7 }, shade(base, 0.82));
			drawFace(g, p, new int[] { 0, 1, 2, 3 }, shade(base, 0.88));
			drawFace(g, p, new int[] { 4, 5, 6, 7 }, shade(base, 1.18));
			drawFace(g, p, new int[] { 0, 3, 7, 4 }, shade(base, 0.95));
			drawFace(g, p, new int[] { 1, 2, 6, 5 }, shade(base, 1.0));
		}
	}

	private static double[][] corners(Voxelizer.Box b) {
		return new double[][] {
				{ b.x1(), b.y1(), b.z1() }, { b.x2(), b.y1(), b.z1() }, { b.x2(), b.y2(), b.z1() }, { b.x1(), b.y2(), b.z1() },
				{ b.x1(), b.y1(), b.z2() }, { b.x2(), b.y1(), b.z2() }, { b.x2(), b.y2(), b.z2() }, { b.x1(), b.y2(), b.z2() }
		};
	}

	private static double[] project(double[] c) {
		double x = c[0] - 8.0;
		double y = c[1] - 8.0;
		double z = c[2] - 8.0;

		double cosY = Math.cos(YAW), sinY = Math.sin(YAW);
		double xr = x * cosY - z * sinY;
		double zr = x * sinY + z * cosY;

		double cosP = Math.cos(PITCH), sinP = Math.sin(PITCH);
		double yr = y * cosP - zr * sinP;
		double depth = y * sinP + zr * cosP;

		return new double[] { xr, -yr, depth };
	}

	private static double depthOf(Voxelizer.Box b) {
		return project(new double[] { (b.x1() + b.x2()) / 2, (b.y1() + b.y2()) / 2, (b.z1() + b.z2()) / 2 })[2];
	}

	private static void drawFace(Graphics2D g, double[][] p, int[] idx, Color colour) {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(p[idx[0]][0], p[idx[0]][1]);
		for (int i = 1; i < idx.length; i++) path.lineTo(p[idx[i]][0], p[idx[i]][1]);
		path.closePath();
		g.setColor(colour);
		g.fill(path);
		g.setColor(new Color(0, 0, 0, 45));
		g.setStroke(new BasicStroke(0.6f));
		g.draw(path);
	}

	private static Color shade(Color c, double factor) {
		return new Color(
				(int) Math.min(255, c.getRed() * factor),
				(int) Math.min(255, c.getGreen() * factor),
				(int) Math.min(255, c.getBlue() * factor));
	}
}
