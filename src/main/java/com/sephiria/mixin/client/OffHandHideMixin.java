package com.sephiria.mixin.client;

import com.sephiria.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 主手拿着标准剑盾时，副手的渲染由剑盾接管（1.20.1 版）。 */
@Mixin(ItemInHandRenderer.class)
public class OffHandHideMixin {
	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void sephiria$hideOffHandWhileHoldingSwordAndShield(LivingEntity entity, ItemStack stack,
			ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
			int light, CallbackInfo callback) {
		if (entity instanceof Player player
				&& player.getMainHandItem().is(ModItems.DEFAULT_SWORD_AND_SHIELD)
				&& player.getOffhandItem() == stack
				&& !stack.isEmpty()) {
			callback.cancel();
		}
	}

	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack sephiria$virtualOffHandShield(ItemStack stack, AbstractClientPlayer player,
			float swing, float equip, InteractionHand hand, float partial, ItemStack original, float pitch,
			PoseStack poseStack, MultiBufferSource buffer, int light) {
		if (hand == InteractionHand.OFF_HAND
				&& player.getMainHandItem().is(ModItems.DEFAULT_SWORD_AND_SHIELD)) {
			return player.getMainHandItem();
		}
		return stack;
	}
}
