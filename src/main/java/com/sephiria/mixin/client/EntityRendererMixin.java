package com.sephiria.mixin.client;

import com.sephiria.client.ClientDebuffs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 敌人脚下的减益层数标签（1.20.1 版）。
 *
 * <p>挂在 {@code EntityRenderDispatcher.render} 的 TAIL，而不是 {@code EntityRenderer.render}
 * 或 {@code renderNameTag}：前者在多数渲染器里被 override 且不调 super，后者只有带名牌的实体
 * 才会走到——普通敌人就永远看不到层数。Dispatcher 这一层是每个实体都必经的，且参数里的
 * x/y/z 是「相对相机的位置」，TAIL 时 pose 已经回到相机坐标系，正好在这里平移过去画。
 *
 * <p>朝向照抄原版名牌：乘相机旋转的四元数，所以字永远正对镜头；字号是名牌的 0.7 倍，
 * 多个减益从脚踝往上排。
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {
	/** 字号：原版名牌是世界缩放 0.025，这里再乘 0.7。 */
	private static final float LABEL_SCALE = 0.0175F;
	/** 超过这个距离（格）就不画：远处的小字看不清还添乱。 */
	private static final double LABEL_RANGE = 32.0D;

	@Inject(method = "render", at = @At("TAIL"))
	private void sephiria$drawDebuffLabels(Entity entity, double x, double y, double z, float yaw,
			float partialTick, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || entity.getId() == client.player.getId()) {
			return;
		}

		if (client.player.distanceToSqr(entity) > LABEL_RANGE * LABEL_RANGE) {
			return;
		}

		List<Component> labels = ClientDebuffs.labelsFor(entity.getId());

		if (labels.isEmpty()) {
			return;
		}

		Font font = client.font;
		pose.pushPose();
		// 脚踝高度；负的 y 缩放让后面的负 y 变成「往上排」
		pose.translate(x, y + 0.3D, z);
		pose.mulPose(client.gameRenderer.getMainCamera().rotation());
		pose.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

		for (int index = 0; index < labels.size(); index++) {
			Component label = labels.get(index);
			font.drawInBatch(label, -font.width(label) / 2.0F, -index * 10.0F, 0xFFFF7777, false,
					pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x40000000,
					LightTexture.FULL_BRIGHT);
		}

		pose.popPose();
	}
}
