package com.sephiria.mixin.client;

import com.sephiria.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

/**
 * 主手拿着标准剑盾时，副手的渲染由剑盾"接管"：
 * <ul>
 *   <li>副手有真实物品时：跳过它的渲染（那个物品没有被移动，只是不画，放下剑盾后照旧）；</li>
 *   <li>副手为空时：把空的副手物品替换成剑盾本身，于是副手会用 {@code *_lefthand}
 *       分支渲染出盾牌模型（姿势与位置完全走原版左手渲染逻辑）。</li>
 * </ul>
 */
@Mixin(ItemInHandRenderer.class)
public class OffHandHideMixin {
	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void sephiria$hideOffHandWhileHoldingSwordAndShield(LivingEntity entity, ItemStack stack,
			ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light,
			CallbackInfo callback) {
		// 副手真实物品在剑盾持握期间不渲染；副手为空时下面会把剑盾自己填进去，
		// 但那是同一个物品，需要放行（否则连虚拟盾牌也被吞掉）。
		if (entity instanceof Player player
				&& player.getMainHandItem().is(ModItems.DEFAULT_SWORD_AND_SHIELD)
				&& player.getOffhandItem() == stack
				&& !stack.isEmpty()) {
			callback.cancel();
		}
	}

	@ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack sephiria$virtualOffHandShield(ItemStack stack, AbstractClientPlayer player,
			float swing, float equip, InteractionHand hand, float partial, ItemStack original, float pitch,
			PoseStack poseStack, SubmitNodeCollector collector, int light) {
		// 主手是剑盾时，副手一律渲染剑盾本身（走 *_lefthand 分支 = 盾牌）。
		// 副手原有的真实物品因此不再被渲染——配合 OffHandGuard 的功能禁用，
		// 放下剑盾后（本方法不再替换）它就原样回来。
		if (hand == InteractionHand.OFF_HAND
				&& player.getMainHandItem().is(ModItems.DEFAULT_SWORD_AND_SHIELD)) {
			return player.getMainHandItem();
		}
		return stack;
	}
}
