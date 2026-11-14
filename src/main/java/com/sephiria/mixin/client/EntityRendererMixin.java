package com.sephiria.mixin.client;

import com.sephiria.client.ClientDebuffs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** 敌人脚下的减益层数标签（1.20.1 版：挂在名牌渲染之后）。 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	@Inject(method = "renderNameTag", at = @At("TAIL"))
	private void sephiria$drawDebuffLabels(Entity entity, Component name, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || entity.getId() == client.player.getId()) {
			return;
		}

		List<Component> labels = ClientDebuffs.labelsFor(entity.getId());

		if (labels.isEmpty()) {
			return;
		}

		Font font = client.font;
		pose.pushPose();
		pose.translate(0.0F, 10.0F, 0.0F);

		for (Component label : labels) {
			font.drawInBatch(label, -font.width(label) / 2.0F, 0.0F, 0xFFFF7777, false,
					pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x40000000, light);
			pose.translate(0.0F, 9.0F, 0.0F);
		}

		pose.popPose();
	}
}
