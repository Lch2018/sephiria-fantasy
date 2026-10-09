package com.sephiria.mixin.client;

import com.sephiria.client.ClientDebuffs;
import com.sephiria.client.DebuffLabelHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 减益层数标签：敌人脚下的小字（「触电：x」）。
 *
 * <p>26.3 的实体渲染拆成「提取状态 → 提交绘制」两步，提交那一步已经拿不到实体了，
 * 所以提取时先把实体 id 记进渲染状态（{@link DebuffLabelHolder}），提交时再按 id 查
 * 客户端的减益层数（{@link ClientDebuffs}）决定画不画。
 *
 * <p>画字借用原版名牌那条路（{@code SubmitNodeCollector#submitNameTag}）——它会自动朝向
 * 镜头、带半透明底、走原版文字渲染，省得自己拼世界坐标投影。位置在实体原点（脚）稍上方，
 * 字号比名牌再小一档。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	/** 字号：原版名牌是世界缩放 0.025，这里再乘 0.7 —— 比名牌小一档的「小字」。 */
	private static final float LABEL_SCALE = 0.7F;
	/** 位置：名牌内部还会再 +0.5 格，所以传 -0.4 —— 落在脚踝高度（再被字号缩放拉近地面）。 */
	private static final Vec3 LABEL_OFFSET = new Vec3(0.0D, -0.4D, 0.0D);
	/** 多行标签（多个减益）的行距，本地坐标、会被字号缩放。 */
	private static final double LABEL_LINE_GAP = 0.35D;
	/** 超过这个距离（格）就不画：远处的小字看不清还添乱。 */
	private static final double LABEL_RANGE = 32.0D;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void sephiria$rememberEntityId(Entity entity, EntityRenderState state, float partialTick,
			CallbackInfo ci) {
		((DebuffLabelHolder) state).sephiria$setEntityId(entity.getId());
	}

	@Inject(method = "submit", at = @At("TAIL"))
	private void sephiria$drawDebuffLabels(EntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();

		if (client.player == null || state.distanceToCameraSq > LABEL_RANGE * LABEL_RANGE) {
			return;
		}

		int entityId = ((DebuffLabelHolder) state).sephiria$entityId();

		// 自己身上不显示：第一人称低头会看到自己脚边挂着一行字，很怪
		if (entityId < 0 || entityId == client.player.getId()) {
			return;
		}

		List<Component> labels = ClientDebuffs.labelsFor(entityId);

		if (labels.isEmpty()) {
			return;
		}

		pose.pushPose();
		pose.scale(LABEL_SCALE, LABEL_SCALE, LABEL_SCALE);

		for (int index = 0; index < labels.size(); index++) {
			// 多个减益时一行一个，从脚下往上排
			Vec3 offset = LABEL_OFFSET.add(0.0D, index * LABEL_LINE_GAP, 0.0D);
			collector.submitNameTag(pose, offset, 0, labels.get(index), true,
					LightCoordsUtil.FULL_BRIGHT, camera);
		}

		pose.popPose();
	}
}
