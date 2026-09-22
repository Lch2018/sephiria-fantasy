package com.sephiria.mixin.client;

import com.sephiria.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 主手拿着标准剑盾时，副手的渲染由剑盾"接管"：
 * <ul>
 *   <li>副手有真实物品时：它被替换成剑盾，于是不再渲染（物品没有被移动，只是不画，放下剑盾后照旧）；</li>
 *   <li>副手为空时：同样被替换成剑盾本身，于是副手会用 {@code *_lefthand}
 *       分支渲染出盾牌模型（姿势与位置完全走原版左手渲染逻辑）。</li>
 * </ul>
 * <p>26.3 重构后 {@code ItemInHandRenderer} 改名 {@link FirstPersonHandsAndItemsRenderer}，
 * 旧的 {@code renderItem} 也删了；物品全部从 {@code FirstPersonHandsAndItemsRenderState}
 * 的字段流进 {@code submitArmWithItem}，所以在参数上做一次替换就同时覆盖上面两种情况
 * （26.2 里拦真实物品的 @Inject 半边因此不再需要）。
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class OffHandHideMixin {
	@ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack sephiria$virtualOffHandShield(ItemStack stack, PlayerRenderState player,
			FirstPersonHandsAndItemsRenderState hands, float swing, float equip, InteractionHand hand,
			float partial, ItemStack original, float pitch, PoseStack poseStack, SubmitNodeCollector collector,
			int light) {
		// 主手是剑盾时，副手一律渲染剑盾本身（走 *_lefthand 分支 = 盾牌）。
		// 副手原有的真实物品因此不再被渲染——配合 OffHandGuard 的功能禁用，
		// 放下剑盾后（本方法不再替换）它就原样回来。
		if (hand == InteractionHand.OFF_HAND
				&& hands.mainHandItem.is(ModItems.DEFAULT_SWORD_AND_SHIELD)) {
			return hands.mainHandItem;
		}
		return stack;
	}
}
