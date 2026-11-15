package com.sephiria.client.render;

import com.sephiria.Sephiria;
import com.sephiria.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 的「物品栏 / 掉落用平面图标、手持才用 3D 模型」桥。
 *
 * <p>26.x 的 {@code items/*.json} 用 {@code display_context} 分模型；1.20.1 没有这套，所以
 * 物品模型本身改成平面图标，手持时由 {@code WeaponHandRendererMixin} 递归调用
 * {@code ItemRenderer} 并传入这里挑出的 3D 模型。3D 模型不再被任何物品模型引用，需要
 * {@code ModelLoadingPlugin} 主动注册烘焙（见 {@code SephiriaClient}）。
 */
public final class WeaponHandModels {
	/** 需要按上下文换 3D 模型的物品：只有这几把基础武器（刀走 GeckoLib，不在这里）。 */
	private static final Item[] ITEMS = {
			ModItems.STEEL_GREATSWORD, ModItems.DAGGER, ModItems.DEFAULT_SWORD_AND_SHIELD,
			ModItems.COLOSSAL_CROSSBOW, ModItems.QUARTERSTAFF
	};

	/** 需要预先烘焙的手持模型名（不含 .json）。 */
	public static final String[] MODEL_NAMES = {
			"steel_greatsword_in_hand", "dagger_in_hand", "shield_in_hand", "sword_in_hand",
			"quarterstaff_in_hand", "colossal_crossbow_in_hand",
			"colossal_crossbow_reload_0", "colossal_crossbow_reload_1", "colossal_crossbow_reload_2",
			"colossal_crossbow_reload_3", "colossal_crossbow_reload_4", "colossal_crossbow_reload_5",
			"colossal_crossbow_reload_6", "colossal_crossbow_reload_7", "colossal_crossbow_reload_8",
			"colossal_crossbow_reload_9", "colossal_crossbow_reload_10"
	};

	private WeaponHandModels() {
	}

	public static boolean isHandItem(Item item) {
		for (Item candidate : ITEMS) {
			if (candidate == item) {
				return true;
			}
		}

		return false;
	}

	public static ModelResourceLocation location(String name) {
		return new ModelResourceLocation(Sephiria.MOD_ID, name, "inventory");
	}

	/**
	 * 这一只手该用哪个 3D 模型。
	 *
	 * <p>标准剑盾：右手是剑（{@code sword_in_hand}）、左手是盾（{@code shield_in_hand}），
	 * 与 26.x 的 {@code display_context} 分流一致；其余武器左右手共用同一个模型。
	 * 重型弩换弹期间按冷却进度切 {@code colossal_crossbow_reload_0..10}（26.x 的
	 * {@code minecraft:cooldown} 分流）。
	 */
	public static String modelFor(ItemStack stack, ItemDisplayContext context) {
		boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

		if (stack.is(ModItems.DEFAULT_SWORD_AND_SHIELD)) {
			return leftHand ? "shield_in_hand" : "sword_in_hand";
		}

		if (stack.is(ModItems.COLOSSAL_CROSSBOW)) {
			LocalPlayer player = Minecraft.getInstance().player;
			float cooldown = player == null ? 0.0F
					: player.getCooldowns().getCooldownPercent(ModItems.COLOSSAL_CROSSBOW, 0.0F);

			if (cooldown > 0.001F) {
				int model = Mth.clamp(10 - (int) Math.ceil(cooldown * 10.0F), 0, 10);
				return "colossal_crossbow_reload_" + model;
			}

			return "colossal_crossbow_in_hand";
		}

		String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return path + "_in_hand";
	}
}
