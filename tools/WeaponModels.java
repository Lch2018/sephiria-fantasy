import java.nio.file.Path;

/**
 * Hand-designed voxel models for all six weapons.
 *
 * Grid: 64 x 64 x 32 quarter-unit voxels over the 16 x 16 model box (x right, y up, z depth with
 * the front at larger z). Fine enough for tapered tips, edge bevels, wrap bands, rivets and a
 * curved katana blade, and every weapon is built from its own feature list rather than traced
 * from the sprite.
 */
public final class WeaponModels {
	private WeaponModels() {
	}

	public static void main(String[] args) throws Exception {
		Path models = Path.of(args[0]);
		Path textures = Path.of(args[1]);
		Path previews = Path.of(args[2]);

		sword(models, textures, previews);
		shield(models, textures, previews);
		greatsword(models, textures, previews);
		dagger(models, textures, previews);
		quarterstaff(models, textures, previews);
		for (int pull = 0; pull < 3; pull++) {
			crossbow(models, textures, previews, pull);
		}
		crossbowLoaded(models, textures, previews);
		for (int stage = 0; stage < 3; stage++) {
			katana(models, textures, previews, stage);
		}
	}

	/** main hand of the paired weapon: tapered blade with a raised fuller, guard, wrapped grip */
	private static void sword(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// blade: 2 units wide, 9 units long, with a two step taper towards the point
		b.box(28, 24, 12, 35, 59, 19, ModelBuilder.STEEL_LIGHT);
		b.box(29, 60, 12, 34, 61, 19, ModelBuilder.STEEL_LIGHT);
		b.box(31, 62, 12, 32, 63, 19, ModelBuilder.STEEL_LIGHT);
		// darker cutting edges
		b.box(28, 24, 12, 28, 59, 19, ModelBuilder.STEEL_MID);
		b.box(35, 24, 12, 35, 59, 19, ModelBuilder.STEEL_MID);
		// raised white fuller down the middle, with a ground face line beside it
		b.box(31, 26, 19, 32, 57, 19, ModelBuilder.WHITE);
		b.box(29, 26, 19, 29, 57, 19, ModelBuilder.STEEL_LIGHT);
		b.box(34, 26, 19, 34, 57, 19, ModelBuilder.STEEL_LIGHT);

		// crossguard: long bar, darker tips, wooden centre block
		b.box(20, 20, 10, 43, 23, 21, ModelBuilder.STEEL_MID);
		b.box(20, 20, 10, 22, 23, 21, ModelBuilder.STEEL_DARK);
		b.box(41, 20, 10, 43, 23, 21, ModelBuilder.STEEL_DARK);
		b.box(28, 20, 12, 35, 23, 19, ModelBuilder.WOOD);

		// grip with two wrap bands
		b.box(28, 8, 14, 35, 19, 17, ModelBuilder.WOOD_DARK);
		b.box(28, 11, 17, 35, 12, 17, ModelBuilder.STEEL_MID);
		b.box(28, 15, 17, 35, 16, 17, ModelBuilder.STEEL_MID);

		// pommel with a bright band
		b.box(26, 4, 12, 37, 7, 19, ModelBuilder.NAVY);
		b.box(27, 5, 19, 36, 6, 19, ModelBuilder.STEEL_LIGHT);

		write(b, models, textures, previews, "sword_in_hand", "sephiria:item/weapon_3d", "sword");
	}

	/** off hand of the paired weapon: oval shield with rivetted rim, planks, stepped boss */
	private static void shield(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// steel rim, then the wooden face inside it
		b.disc(32, 32, 19.0, 19.0, 16, 21, ModelBuilder.STEEL_LIGHT);
		b.disc(32, 32, 15.5, 15.5, 16, 21, ModelBuilder.WOOD);
		// rivets around the rim
		for (int[] r : new int[][] { { 18, 18 }, { 45, 18 }, { 18, 45 }, { 45, 45 } }) {
			b.box(r[0], r[1], 21, r[0] + 2, r[1] + 2, 21, ModelBuilder.STEEL_DARK);
		}
		// plank seams
		b.box(24, 20, 21, 25, 44, 21, ModelBuilder.WOOD_DARK);
		b.box(39, 20, 21, 40, 44, 21, ModelBuilder.WOOD_DARK);
		// stepped centre boss
		b.box(28, 28, 21, 35, 35, 24, ModelBuilder.STEEL_LIGHT);
		b.box(30, 30, 24, 33, 33, 25, ModelBuilder.WHITE);
		// arm straps on the back
		b.box(30, 22, 12, 33, 42, 15, ModelBuilder.WOOD_DARK);
		b.box(30, 24, 12, 33, 25, 15, ModelBuilder.WOOD);

		write(b, models, textures, previews, "shield_in_hand", "sephiria:item/shield_3d", "shield");
	}

	/** two handed blade: wider and longer than the sword, with a broad guard */
	private static void greatsword(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// blade: 3 units wide, 9.5 units long
		b.box(26, 22, 12, 37, 59, 19, ModelBuilder.STEEL_LIGHT);
		b.box(27, 60, 12, 36, 61, 19, ModelBuilder.STEEL_LIGHT);
		b.box(30, 62, 12, 33, 63, 19, ModelBuilder.STEEL_LIGHT);
		// edges, fuller and the ground faces beside it
		b.box(26, 22, 12, 26, 59, 19, ModelBuilder.STEEL_MID);
		b.box(37, 22, 12, 37, 59, 19, ModelBuilder.STEEL_MID);
		b.box(31, 24, 19, 32, 57, 19, ModelBuilder.WHITE);
		b.box(29, 24, 19, 29, 57, 19, ModelBuilder.STEEL_LIGHT);
		b.box(34, 24, 19, 34, 57, 19, ModelBuilder.STEEL_LIGHT);

		// broad guard with darker tips and a wooden core
		b.box(16, 18, 8, 47, 21, 23, ModelBuilder.STEEL_MID);
		b.box(16, 18, 8, 18, 21, 23, ModelBuilder.STEEL_DARK);
		b.box(45, 18, 8, 47, 21, 23, ModelBuilder.STEEL_DARK);
		b.box(28, 18, 10, 35, 21, 21, ModelBuilder.WOOD);

		// long grip with two bands, then the pommel
		b.box(28, 4, 14, 35, 17, 17, ModelBuilder.WOOD_DARK);
		b.box(28, 8, 17, 35, 9, 17, ModelBuilder.STEEL_MID);
		b.box(28, 13, 17, 35, 14, 17, ModelBuilder.STEEL_MID);
		b.box(26, 0, 12, 37, 3, 19, ModelBuilder.NAVY);
		b.box(27, 1, 19, 36, 2, 19, ModelBuilder.STEEL_LIGHT);

		write(b, models, textures, previews, "steel_greatsword_in_hand", "sephiria:item/greatsword_3d", "greatsword");
	}

	/** short blade: compact proportions, big highlight, small guard */
	private static void dagger(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		b.box(30, 34, 14, 33, 55, 17, ModelBuilder.STEEL_LIGHT);
		b.box(31, 56, 14, 32, 57, 17, ModelBuilder.STEEL_LIGHT);
		b.box(30, 34, 14, 30, 55, 17, ModelBuilder.STEEL_MID);
		b.box(33, 34, 14, 33, 55, 17, ModelBuilder.STEEL_MID);
		b.box(31, 36, 17, 32, 53, 17, ModelBuilder.WHITE);

		// small guard
		b.box(26, 32, 12, 37, 35, 19, ModelBuilder.STEEL_MID);
		b.box(30, 32, 14, 33, 35, 17, ModelBuilder.WOOD);

		// grip and pommel
		b.box(30, 22, 15, 33, 31, 18, ModelBuilder.WOOD_DARK);
		b.box(30, 26, 18, 33, 27, 18, ModelBuilder.STEEL_MID);
		b.box(28, 20, 14, 35, 21, 19, ModelBuilder.NAVY);

		write(b, models, textures, previews, "dagger_in_hand", "sephiria:item/weapon_3d", "dagger");
	}

	/** long staff: full height shaft, metal ferrules, wood grain breaks */
	private static void quarterstaff(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		b.box(30, 0, 16, 33, 63, 19, ModelBuilder.WOOD);
		// grain: darker bands where the wood was cut
		for (int y : new int[] { 12, 28, 44 }) {
			b.box(30, y, 19, 33, y + 3, 19, ModelBuilder.WOOD_DARK);
		}
		// metal ferrules
		b.box(30, 8, 16, 33, 11, 19, ModelBuilder.STEEL_MID);
		b.box(30, 24, 16, 33, 27, 19, ModelBuilder.STEEL_MID);
		b.box(30, 48, 16, 33, 51, 19, ModelBuilder.STEEL_MID);
		// dark end caps
		b.box(30, 0, 16, 33, 3, 19, ModelBuilder.STEEL_DARK);
		b.box(30, 60, 16, 33, 63, 19, ModelBuilder.STEEL_DARK);

		write(b, models, textures, previews, "quarterstaff_in_hand", "sephiria:item/weapon_3d", "quarterstaff");
	}

	/** vanilla-like crossbow: tiller, rail, stepped prod, string that travels with the pull */
	private static void crossbow(Path models, Path textures, Path previews, int pull) throws Exception {
		ModelBuilder b = new ModelBuilder();
		crossbowBody(b);

		int nut = 48 - pull * 8;
		line(b, 59, 41, nut, 31, 21, ModelBuilder.WHITE);
		line(b, 59, 21, nut, 31, 21, ModelBuilder.WHITE);

		if (pull == 2) {
			b.box(nut, 30, 19, 60, 31, 19, ModelBuilder.WOOD_DARK);
			b.box(60, 30, 19, 61, 31, 19, ModelBuilder.STEEL_LIGHT);
		}

		write(b, models, textures, previews, "crossbow_pulling_" + pull, "sephiria:item/crossbow_3d", "crossbow_pull" + pull);
	}

	/** charged crossbow: bolt resting on the rail */
	private static void crossbowLoaded(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();
		crossbowBody(b);

		line(b, 59, 41, 48, 31, 21, ModelBuilder.WHITE);
		line(b, 59, 21, 48, 31, 21, ModelBuilder.WHITE);
		b.box(30, 33, 21, 60, 34, 21, ModelBuilder.WOOD_DARK);
		b.box(60, 33, 21, 61, 34, 21, ModelBuilder.STEEL_LIGHT);

		write(b, models, textures, previews, "crossbow_loaded_in_hand", "sephiria:item/crossbow_3d", "crossbow_loaded");
	}

	/** the parts every crossbow state shares */
	private static void crossbowBody(ModelBuilder b) {
		// tiller, top rail, butt with a steel plate
		b.box(18, 28, 16, 48, 33, 21, ModelBuilder.WOOD);
		b.box(22, 33, 18, 46, 35, 21, ModelBuilder.WOOD_DARK);
		b.box(10, 26, 14, 17, 35, 23, ModelBuilder.WOOD_DARK);
		b.box(10, 26, 14, 11, 35, 23, ModelBuilder.STEEL_DARK);
		// grip and trigger
		b.box(24, 20, 16, 31, 28, 21, ModelBuilder.WOOD_DARK);
		b.box(34, 24, 18, 39, 28, 21, ModelBuilder.STEEL_DARK);
		// prod: stepped arms sweeping forward, mirrored above and below the rail
		b.box(48, 32, 18, 52, 35, 21, ModelBuilder.STEEL_DARK);
		b.box(52, 34, 18, 55, 37, 21, ModelBuilder.STEEL_DARK);
		b.box(55, 36, 18, 57, 39, 21, ModelBuilder.STEEL_DARK);
		b.box(57, 38, 18, 59, 41, 21, ModelBuilder.STEEL_DARK);
		b.box(59, 40, 18, 60, 42, 21, ModelBuilder.STEEL_LIGHT);
		b.box(48, 27, 18, 52, 30, 21, ModelBuilder.STEEL_DARK);
		b.box(52, 25, 18, 55, 28, 21, ModelBuilder.STEEL_DARK);
		b.box(55, 23, 18, 57, 26, 21, ModelBuilder.STEEL_DARK);
		b.box(57, 21, 18, 59, 24, 21, ModelBuilder.STEEL_DARK);
		b.box(59, 20, 18, 60, 22, 21, ModelBuilder.STEEL_LIGHT);
	}

	/**
	 * Katana with scabbard. stage 0 = sheathed, 1 = half drawn, 2 = fully drawn.
	 * The blade curves gently; the sword rises as a unit while the scabbard stays put, and the
	 * part still inside the scabbard is hidden by it.
	 */
	private static void katana(Path models, Path textures, Path previews, int stage) throws Exception {
		ModelBuilder b = new ModelBuilder();

		if (stage == 2) {
			katanaBlade(b, 28, 63);
			tsuba(b, 24, 27);
			handle(b, 8, 23);
		} else if (stage == 1) {
			scabbard(b, 24, 44);
			katanaBlade(b, 36, 63);
			tsuba(b, 32, 35);
			handle(b, 16, 31);
		} else {
			scabbard(b, 24, 52);
			tsuba(b, 20, 23);
			handle(b, 4, 19);
		}

		write(b, models, textures, previews, "katana_" + (stage == 0 ? "sheathed" : stage == 1 ? "drawing" : "in_hand"),
				"sephiria:item/katana_3d", "katana_" + stage);
	}

	/** curved single edged blade with a bright edge; drawn row by row so the curve is smooth */
	private static void katanaBlade(ModelBuilder b, int y1, int y2) {
		for (int y = y1; y <= y2; y++) {
			int shift = (y - y1) / 16;       // the tip leans away from the handle
			int x1 = 30 + shift;
			b.box(x1, y, 16, x1 + 3, y, 19, ModelBuilder.STEEL_LIGHT);
			b.box(x1 + 3, y, 19, x1 + 3, y, 19, ModelBuilder.WHITE);
			b.box(x1, y, 16, x1, y, 16, ModelBuilder.STEEL_MID);
		}
	}

	/** katana scabbard: fatter than the blade so it hides whatever is still inside */
	private static void scabbard(ModelBuilder b, int y1, int y2) {
		b.box(28, y1, 14, 35, y2, 21, ModelBuilder.NAVY);
		b.box(28, y1, 14, 35, y1 + 3, 21, ModelBuilder.STEEL_LIGHT);
		b.box(28, y1 + 8, 14, 35, y1 + 9, 21, ModelBuilder.STEEL_MID);
	}

	/** tsuba: dark plate with a steel border */
	private static void tsuba(ModelBuilder b, int y1, int y2) {
		b.box(24, y1, 12, 39, y2, 23, ModelBuilder.STEEL_DARK);
		b.box(25, y1, 13, 38, y2, 22, ModelBuilder.NAVY);
	}

	/** wrapped grip with alternating bands and an end cap */
	private static void handle(ModelBuilder b, int y1, int y2) {
		b.box(28, y1, 16, 35, y2, 19, ModelBuilder.WOOD_DARK);
		for (int y = y1 + 3; y + 1 < y2; y += 4) {
			b.box(28, y, 19, 35, y + 1, 19, ModelBuilder.STEEL_MID);
		}
		b.box(28, y1 - 2, 16, 35, y1 - 1, 19, ModelBuilder.NAVY);
	}

	/** straight voxel line, used for crossbow strings */
	private static void line(ModelBuilder b, int x1, int y1, int x2, int y2, int z, int colour) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
		for (int i = 0; i <= steps; i++) {
			double t = steps == 0 ? 0 : i / (double) steps;
			int x = (int) Math.round(x1 + (x2 - x1) * t);
			int y = (int) Math.round(y1 + (y2 - y1) * t);
			b.box(x, y, z, x, y, z, colour);
		}
	}

	private static void write(ModelBuilder b, Path models, Path textures, Path previews, String name, String parent, String previewName) throws Exception {
		Path modelPath = models.resolve(name + ".json");
		Path texturePath = textures.resolve(name.replace("_in_hand", "_3d") + ".png");
		Path previewPath = previews.resolve(previewName + "_design_preview.png");
		String paletteTexture = "sephiria:item/" + name.replace("_in_hand", "_3d");
		b.write(modelPath, texturePath, parent, paletteTexture, previewPath);
		System.out.println("wrote " + name + " (parent " + parent + ")");
	}
}
