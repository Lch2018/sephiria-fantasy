package com.sephiria.mixin.client;

import com.google.common.collect.Multimap;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 刀的攻击力/攻速随出鞘 / 入鞘状态切换（1.20.1 物品属性是按 ItemStack 查的）。 */
@Mixin(ItemStack.class)
public class KatanaAttributesMixin {
	@Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
	private void sephiria$katanaState(EquipmentSlot slot,
			CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
		ItemStack self = (ItemStack) (Object) this;

		if (slot == EquipmentSlot.MAINHAND && self.getItem() instanceof SephiriaKatanaItem) {
			cir.setReturnValue(SephiriaKatanaItem.stateAttributes(SephiriaKatanaItem.isSheathed(self)));
		}
	}
}
