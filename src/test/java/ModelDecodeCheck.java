import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.server.Bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Temporary dev-only check: decode the mod's item model definitions with the real vanilla codecs.
 *
 * <p>Item model JSON is only ever parsed by the client at resource load, so a wrong field name would
 * not show up until the game is started - this runs the same codecs in a plain JVM instead.
 *
 * <p>The mod's own components cannot be registered here (outside Fabric, the built-in registries
 * refuse registration before {@code Bootstrap.bootStrap()} and are frozen after it), so the
 * component ids are swapped for vanilla components of the same value type before decoding:
 * {@code minecraft:damage} (an int component) stands in for the eclipse's SIDE and
 * {@code minecraft:unbreakable} (a Unit component) for the two markers. The model SHAPE - which is
 * what can be wrong - is validated for real; the ids themselves are plain registry lookups.
 * Not part of the mod jar (test source set, never shipped).
 */
public final class ModelDecodeCheck {
	private ModelDecodeCheck() {
	}

	/** file -> [mod component id, vanilla stand-in] */
	private static final List<String[]> FILES = List.of(
			new String[] { "eternal_eclipse", "sephiria_fantasy:eternal_eclipse_side", "minecraft:damage" },
			new String[] { "sun_sword", "sephiria_fantasy:sun_sword_ice", "minecraft:unbreakable" },
			new String[] { "ice_hammer", "sephiria_fantasy:flame_tool", "minecraft:unbreakable" },
			new String[] { "ice_spear", "sephiria_fantasy:flame_tool", "minecraft:unbreakable" },
			new String[] { "ice_arrow", "sephiria_fantasy:flame_tool", "minecraft:unbreakable" },
			// 根之退 / 许愿池（方块物品）与四叶草：没有组件，替换串在文件里不存在，等于原样解一遍
			new String[] { "root_retreat", "sephiria_fantasy:root_retreat_side", "minecraft:damage" },
			new String[] { "wish_pool", "sephiria_fantasy:wish_pool_side", "minecraft:damage" },
			new String[] { "root_retreat", "sephiria_fantasy:root_retreat_side2", "minecraft:damage" },
			// 「书」：最普通的一层定义（同样没有组件，原样解一遍，防止以后改贴图路径时写坏）
			new String[] { "book", "sephiria_fantasy:book_marker", "minecraft:damage" });

	public static void main(String[] args) throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		ItemModels.bootstrap();
		SelectItemModelProperties.bootstrap();
		ConditionalItemModelProperties.bootstrap();
		RangeSelectItemModelProperties.bootstrap();

		int failures = 0;

		for (String[] entry : FILES) {
			Path path = Path.of("src/main/resources/assets/sephiria_fantasy/items/" + entry[0] + ".json");
			String text = Files.readString(path).replace(entry[1], entry[2]);
			JsonObject json = JsonParser.parseString(text).getAsJsonObject();
			var result = ItemModels.CODEC.parse(JsonOps.INSTANCE, json.get("model"));

			if (result.error().isPresent()) {
				System.out.println("FAIL " + entry[0] + " -> " + result.error().get().message());
				failures++;
			} else {
				System.out.println("ok   " + entry[0] + " -> " + result.result().get());
			}
		}

		System.out.println(failures == 0 ? "ALL MODEL DEFINITIONS DECODE" : (failures + " FAILURE(S)"));
		System.exit(failures == 0 ? 0 : 1);
	}
}
