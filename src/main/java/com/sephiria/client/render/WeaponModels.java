package com.sephiria.client.render;

import com.sephiria.Sephiria;
import com.sephiria.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** 武器手持时改用 `_in_hand` 3D 模型；GUI/掉落仍用平面模型。 */
public final class WeaponModels {
	private static final Map<Item, ResourceLocation> IN_HAND = Map.of(
			ModItems.DEFAULT_SWORD_AND_SHIELD, model("shield_in_hand"),
			ModItems.STEEL_GREATSWORD, model("steel_greatsword_in_hand"),
			ModItems.DAGGER, model("dagger_in_hand"),
			ModItems.COLOSSAL_CROSSBOW, model("colossal_crossbow_in_hand"),
			ModItems.QUARTERSTAFF, model("quarterstaff_in_hand"));

	private WeaponModels() {
	}

	private static ResourceLocation model(String name) {
		return new ResourceLocation(Sephiria.MOD_ID, "item/" + name);
	}

	public static boolean hand(ItemDisplayContext context) {
		return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	public static BakedModel modelFor(ItemStack stack, ItemDisplayContext context) {
		ResourceLocation id = IN_HAND.get(stack.getItem());

		if (id == null || !hand(context)) {
			return null;
		}

		return Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(id, "inventory"));
	}
}
