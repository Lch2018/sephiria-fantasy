package com.sephiria.mixin.client;

import com.sephiria.client.render.WeaponModels;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 手持本模组武器时把平面模型换成 `_in_hand` 3D 模型。 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererModelMixin {
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
	private BakedModel sephiria$inHand(BakedModel model, ItemStack stack, ItemDisplayContext context) {
		BakedModel replacement = WeaponModels.modelFor(stack, context);
		return replacement != null ? replacement : model;
	}
}
