package com.sephiria.mixin.client;

import com.sephiria.weapon.SephiriaCrossbowItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 拿着赛菲莉娅弩时，第三人称手部姿势用原版「举着上弦弩」那一套。 */
@Mixin(PlayerRenderer.class)
public class CrossbowArmPoseMixin {
	@Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true, require = 0)
	private static void sephiria$crossbowHold(AbstractClientPlayer player, InteractionHand hand,
			CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
		if (player.swinging) {
			return;
		}

		boolean main = player.getMainHandItem().getItem() instanceof SephiriaCrossbowItem;
		boolean off = player.getOffhandItem().getItem() instanceof SephiriaCrossbowItem;

		if ((hand == InteractionHand.MAIN_HAND && main) || (hand == InteractionHand.OFF_HAND && off)) {
			cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
		}
	}
}
