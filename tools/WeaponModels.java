import java.nio.file.Path;

/**
 * Hand-designed voxel models for the weapons whose shape cannot come from the sprite alone:
 * the sword and the shield of the paired weapon are separate objects, the katana has a
 * scabbard with a drawing animation, and the crossbow follows the vanilla crossbow's parts
 * (stock, prod, string, trigger) so its charge animation can move the string.
 *
 * Coordinates are half-unit voxels over the 32x32x16 grid: x right, y up, z depth (front = larger z).
 */
public final class WeaponModels {
	private WeaponModels() {
	}

	public static void main(String[] args) throws Exception {
		Path models = Path.of(args[0]);
		Path textures = Path.of(args[1]);
		Path previews = Path.of(args[2]);

		sword(models, textures, previews);
		greatsword(models, textures, previews);
		shield(models, textures, previews);
		for (int pull = 0; pull < 3; pull++) {
			crossbow(models, textures, previews, pull);
		}
		crossbowLoaded(models, textures, previews);
		katana(models, textures, previews, 0);
		katana(models, textures, previews, 1);
		katana(models, textures, previews, 2);
	}

	/** main hand of the paired weapon: blade, fuller, guard, grip, pommel */
	private static void sword(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// blade, 2 units wide and 9.5 units long, tip tapered
		b.box(14, 12, 6, 17, 29, 9, ModelBuilder.STEEL_LIGHT);
		b.box(15, 30, 6, 16, 31, 9, ModelBuilder.STEEL_LIGHT);
		b.box(14, 30, 6, 17, 30, 9, ModelBuilder.STEEL_LIGHT);
		// raised white highlight down the middle of the blade
		b.box(15, 13, 9, 16, 29, 9, ModelBuilder.WHITE);
		// darker bevels along both edges give the blade a cross-section
		b.box(14, 12, 6, 14, 29, 6, ModelBuilder.STEEL_MID);
		b.box(17, 12, 6, 17, 29, 6, ModelBuilder.STEEL_MID);

		// crossguard: steel bar with a wooden centre
		b.box(10, 10, 5, 21, 11, 10, ModelBuilder.STEEL_MID);
		b.box(14, 10, 6, 17, 11, 9, ModelBuilder.WOOD);

		// grip with a lighter band
		b.box(14, 4, 7, 17, 9, 8, ModelBuilder.WOOD_DARK);
		b.box(14, 6, 8, 17, 7, 8, ModelBuilder.STEEL_LIGHT);

		// pommel
		b.box(13, 2, 6, 18, 3, 9, ModelBuilder.NAVY);
		b.box(14, 4, 7, 17, 4, 8, ModelBuilder.NAVY);

		write(b, models, textures, previews, "sword_in_hand", "sephiria:item/weapon_3d", "sword");
	}

	/** off hand of the paired weapon: oval shield with steel rim, wooden planks and a boss */
	private static void shield(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// plate: steel rim, then the wooden face painted inside it
		b.disc(16, 16, 9.5, 9.5, 8, 10, ModelBuilder.STEEL_LIGHT);
		b.disc(16, 16, 7.8, 7.8, 8, 10, ModelBuilder.WOOD);
		// plank lines
		b.box(12, 10, 10, 12, 22, 10, ModelBuilder.WOOD_DARK);
		b.box(19, 10, 10, 19, 22, 10, ModelBuilder.WOOD_DARK);
		// centre boss, raised
		b.box(14, 14, 10, 17, 17, 12, ModelBuilder.STEEL_LIGHT);
		b.box(15, 15, 12, 16, 16, 12, ModelBuilder.WHITE);
		// arm straps on the back
		b.box(15, 11, 6, 16, 21, 7, ModelBuilder.WOOD_DARK);

		write(b, models, textures, previews, "shield_in_hand", "sephiria:item/shield_3d", "shield");
	}

	/** vanilla-like crossbow: wooden tiller, steel prod, string that travels back with the pull */
	private static void crossbow(Path models, Path textures, Path previews, int pull) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// tiller and butt
		b.box(9, 14, 8, 24, 16, 10, ModelBuilder.WOOD);
		b.box(11, 16, 8, 23, 17, 10, ModelBuilder.WOOD_DARK);
		b.box(5, 13, 7, 8, 17, 11, ModelBuilder.WOOD_DARK);
		// grip and trigger
		b.box(12, 10, 8, 15, 14, 10, ModelBuilder.WOOD_DARK);
		b.box(17, 12, 9, 19, 14, 10, ModelBuilder.STEEL_DARK);
		// prod: two arms sweeping forward, stepped to keep the voxel look
		b.box(24, 16, 9, 26, 17, 10, ModelBuilder.STEEL_DARK);
		b.box(26, 17, 9, 27, 18, 10, ModelBuilder.STEEL_DARK);
		b.box(27, 18, 9, 28, 20, 10, ModelBuilder.STEEL_DARK);
		b.box(24, 15, 9, 26, 16, 10, ModelBuilder.STEEL_DARK);
		b.box(26, 14, 9, 27, 15, 10, ModelBuilder.STEEL_DARK);
		b.box(27, 12, 9, 28, 13, 10, ModelBuilder.STEEL_DARK);
		// limb tips in light steel so the string anchoring reads
		b.box(28, 19, 9, 28, 20, 10, ModelBuilder.STEEL_LIGHT);
		b.box(28, 12, 9, 28, 13, 10, ModelBuilder.STEEL_LIGHT);

		// string: from tip to tip, its middle pulled back along the tiller
		int nut = 24 - pull * 4;
		line(b, 28, 19, nut, 16, 10, ModelBuilder.WHITE);
		line(b, 28, 12, nut, 16, 10, ModelBuilder.WHITE);

		// a nocked bolt once the string is fully back
		if (pull == 2) {
			b.box(nut, 15, 9, 30, 16, 9, ModelBuilder.WOOD_DARK);
			b.box(30, 15, 9, 31, 16, 9, ModelBuilder.STEEL_LIGHT);
		}

		write(b, models, textures, previews, "crossbow_pulling_" + pull, "sephiria:item/crossbow_3d", "crossbow_pull" + pull);
	}

	/** charged crossbow: loaded bolt on the rail */
	private static void crossbowLoaded(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		b.box(9, 14, 8, 24, 16, 10, ModelBuilder.WOOD);
		b.box(11, 16, 8, 23, 17, 10, ModelBuilder.WOOD_DARK);
		b.box(5, 13, 7, 8, 17, 11, ModelBuilder.WOOD_DARK);
		b.box(12, 10, 8, 15, 14, 10, ModelBuilder.WOOD_DARK);
		b.box(17, 12, 9, 19, 14, 10, ModelBuilder.STEEL_DARK);
		b.box(24, 16, 9, 26, 17, 10, ModelBuilder.STEEL_DARK);
		b.box(26, 17, 9, 27, 18, 10, ModelBuilder.STEEL_DARK);
		b.box(27, 18, 9, 28, 20, 10, ModelBuilder.STEEL_DARK);
		b.box(24, 15, 9, 26, 16, 10, ModelBuilder.STEEL_DARK);
		b.box(26, 14, 9, 27, 15, 10, ModelBuilder.STEEL_DARK);
		b.box(27, 12, 9, 28, 13, 10, ModelBuilder.STEEL_DARK);
		b.box(28, 19, 9, 28, 20, 10, ModelBuilder.STEEL_LIGHT);
		b.box(28, 12, 9, 28, 13, 10, ModelBuilder.STEEL_LIGHT);

		// string back at the nut with a bolt lying on the rail
		line(b, 28, 19, 24, 16, 10, ModelBuilder.WHITE);
		line(b, 28, 12, 24, 16, 10, ModelBuilder.WHITE);
		b.box(15, 16, 10, 30, 17, 10, ModelBuilder.WOOD_DARK);
		b.box(30, 16, 10, 31, 17, 10, ModelBuilder.STEEL_LIGHT);

		write(b, models, textures, previews, "crossbow_loaded_in_hand", "sephiria:item/crossbow_3d", "crossbow_loaded");
	}

	/**
	 * Katana with its scabbard. stage 0 = sheathed, 1 = half drawn, 2 = fully drawn.
	 *
	 * The sword (blade + tsuba + handle) rises as a unit while the scabbard stays put; the part
	 * of the blade still inside the scabbard is hidden by it, because the scabbard is a fatter
	 * box around the same space. The blade is drawn long enough to fill the whole model once
	 * it is out, which is what makes the drawn katana read as a long blade.
	 */
	private static void katana(Path models, Path textures, Path previews, int stage) throws Exception {
		ModelBuilder b = new ModelBuilder();

		if (stage == 2) {
			// fully drawn: the scabbard is stowed, the blade spans most of the model
			tsuba(b, 12, 13);
			handle(b, 4, 11);
			blade(b, 14, 31);
		} else if (stage == 1) {
			// half drawn: scabbard shortened, sword lifted so the blade shows above the mouth
			scabbard(b, 12, 22);
			tsuba(b, 16, 17);
			handle(b, 8, 15);
			b.box(15, 18, 8, 16, 31, 9, ModelBuilder.STEEL_LIGHT);
			b.box(16, 18, 9, 16, 31, 9, ModelBuilder.WHITE);
			b.box(15, 18, 8, 15, 31, 8, ModelBuilder.STEEL_MID);
		} else {
			// sheathed: only scabbard, tsuba and handle are visible
			scabbard(b, 12, 26);
			tsuba(b, 10, 11);
			handle(b, 2, 9);
		}

		write(b, models, textures, previews, "katana_" + (stage == 0 ? "sheathed" : stage == 1 ? "drawing" : "in_hand"),
				"sephiria:item/katana_3d", "katana_" + stage);
	}

	/** long two-handed blade: the greatsword gets its own geometry so it can be genuinely long */
	private static void greatsword(Path models, Path textures, Path previews) throws Exception {
		ModelBuilder b = new ModelBuilder();

		// blade, 3 units wide, 10.5 units long, tip tapered
		b.box(13, 11, 6, 18, 29, 9, ModelBuilder.STEEL_LIGHT);
		b.box(14, 30, 6, 17, 31, 9, ModelBuilder.STEEL_LIGHT);
		b.box(13, 30, 6, 18, 30, 9, ModelBuilder.STEEL_LIGHT);
		// raised white fuller down the middle, darker bevels on both edges
		b.box(15, 12, 9, 16, 29, 9, ModelBuilder.WHITE);
		b.box(13, 11, 6, 13, 29, 6, ModelBuilder.STEEL_MID);
		b.box(18, 11, 6, 18, 29, 6, ModelBuilder.STEEL_MID);

		// wide crossguard with a wooden centre
		b.box(8, 9, 4, 23, 10, 11, ModelBuilder.STEEL_MID);
		b.box(14, 9, 5, 17, 10, 10, ModelBuilder.WOOD);

		// long grip with a lighter band, then the pommel
		b.box(14, 2, 7, 17, 8, 8, ModelBuilder.WOOD_DARK);
		b.box(14, 4, 9, 17, 5, 9, ModelBuilder.STEEL_LIGHT);
		b.box(13, 0, 6, 18, 1, 9, ModelBuilder.NAVY);

		write(b, models, textures, previews, "steel_greatsword_in_hand", "sephiria:item/greatsword_3d", "greatsword");
	}

	/** katana blade: 1 unit wide, white edge on the cutting side */
	private static void blade(ModelBuilder b, int y1, int y2) {
		b.box(15, y1, 8, 16, y2, 9, ModelBuilder.STEEL_LIGHT);
		b.box(16, y1, 9, 16, y2, 9, ModelBuilder.WHITE);
		b.box(15, y1, 8, 15, y2, 8, ModelBuilder.STEEL_MID);
	}

	/** katana scabbard: fatter than the blade so it can hide the part still inside */
	private static void scabbard(ModelBuilder b, int y1, int y2) {
		b.box(14, y1, 7, 17, y2, 10, ModelBuilder.NAVY);
		b.box(14, y1, 7, 17, y1 + 1, 10, ModelBuilder.STEEL_LIGHT);
	}

	/** katana guard */
	private static void tsuba(ModelBuilder b, int y1, int y2) {
		b.box(12, y1, 6, 19, y2, 11, ModelBuilder.NAVY);
	}

	/** katana grip with a wrap band and an end cap */
	private static void handle(ModelBuilder b, int y1, int y2) {
		b.box(14, y1, 8, 17, y2, 9, ModelBuilder.WOOD_DARK);
		b.box(14, y1 + 3, 9, 17, y1 + 4, 9, ModelBuilder.STEEL_MID);
		b.box(14, y1 - 2, 8, 17, y1 - 1, 9, ModelBuilder.STEEL_DARK);
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
