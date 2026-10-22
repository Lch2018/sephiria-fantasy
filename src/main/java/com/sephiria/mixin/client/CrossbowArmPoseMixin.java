package com.sephiria.mixin.client;

import com.sephiria.weapon.SephiriaCrossbowItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家拿着赛菲莉娅弩时，第三人称的手持姿势用原版「举着上弦弩」那一套（手臂指向要打的方向）。
 *
 * <p>原版只在<b>弩已上弦</b>时才摆这个姿势：{@code AvatarRenderer#getArmPose} 里判断
 * {@code Items.CROSSBOW && CrossbowItem.isCharged}，然后写进渲染状态的 {@code rightArmPose} /
 * {@code leftArmPose}。赛菲莉娅弩是弹匣式连发、没有上弦状态，所以这里在
 * {@code extractRenderState} 的末尾按「拿着它」补一次（用户 2026-10-22 要求常态就指向要打的方向）。
 *
 * <p>⚠ 注入点特意选 {@code extractRenderState}（public）而不是原版那两个 private 的
 * {@code getArmPose}：**本模组在 Fabric 生产环境里对 private 方法的描述符选择器匹配不上**
 * （2026-10-22 实测：{@code @Inject(method = "getArmPose(...)")} 直接让客户端启动崩在
 * {@code InvalidInjectionException: could not find any targets}），public / protected 的一直没问题。
 *
 * <p>{@code ArmPose.CROSSBOW_HOLD} 在 {@code HumanoidModel} 里会同时摆好两只手臂，和上弦弩在
 * 第三人称的样子一致；挥击时不改（跟原版一样让手臂正常挥动）。只影响第三人称 / 别人眼里的模型，
 * 第一人称的手走 {@code FirstPersonHandsAndItemsRenderer}，不经过这里。
 */
@Mixin(AvatarRenderer.class)
public class CrossbowArmPoseMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"),
			// require = 0：万一以后原版改了方法签名，只是这个姿势失效，不至于让客户端起不来
			require = 0)
	private void sephiria$crossbowHold(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo info) {
		if (avatar.isSwinging()) {
			return;
		}

		if (avatar.getMainHandItem().getItem() instanceof SephiriaCrossbowItem) {
			setPose(state, avatar.getMainArm());
		} else if (avatar.getOffhandItem().getItem() instanceof SephiriaCrossbowItem) {
			setPose(state, avatar.getMainArm().getOpposite());
		}
	}

	private static void setPose(AvatarRenderState state, HumanoidArm arm) {
		if (arm == HumanoidArm.RIGHT) {
			state.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
		} else {
			state.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
		}
	}
}
