package com.sephiria.compat.torohealth.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.sephiria.compat.torohealth.ToroHealthEarlyText;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把 ToroHealth 的浮动伤害文字提前到 {@code prepareFrame} 之前提交。
 *
 * <p>26.3 的 {@code LevelRenderer.render} 顺序是：先 {@code submitFeatures} 收集世界几何，
 * 然后 {@code prepareFrame(submitNodeStorage)} 一次性把提交队列 drain 掉，最后才执行
 * {@code addMainPass}。ToroHealth 1.5.2 把自己的文字渲染挂在 {@code addMainPass} 之前——
 * 此时队列已经被 drain，`submitText` / `submitCustomGeometry` 的提交会留到下一帧甚至丢失，
 * 所以它的浮动数字在 26.3 上根本不稳定（双属性自定义几何的回调也从未执行）。
 *
 * <p>这里在 **prepareFrame 调用之前** 把 ToroHealth 的 {@code TextRenderQueue} 消费掉，
 * 让文字进入本帧的提交队列；ToroHealth 自己晚一步的循环只会拿到空队列。
 * 必须在 {@code submitFeatures} 之后（粒子 extract 已经把文字放进队列），
 * {@code prepareFrame} 之前（否则提交无效）。
 *
 * <p>只放在可选的 ToroHealth mixin 配置里；没装 ToroHealth 时整个配置不会加载。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererTextEarlyMixin {
	@Shadow
	@Final
	private LevelRenderState levelRenderState;
	@Shadow
	@Final
	private SubmitNodeStorage submitNodeStorage;

	@Inject(method = "render",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"))
	private void sephiria$flushToroHealthTextBeforePrepare(GraphicsResourceAllocator allocator,
			boolean renderOutline, CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor,
			boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo callback) {
		// 这个 mixin 挂在原版 LevelRenderer 上，没装 ToroHealth 时也会被应用；
		// 所以这里不能直接碰 ToroHealth 的任何类型，否则新实例只有本模组时会直接
		// NoClassDefFoundError。真正的类型引用都放在 ToroHealthEarlyText 里，确认装了再加载它。
		if (!FabricLoader.getInstance().isModLoaded("torohealth")) {
			return;
		}

		ToroHealthEarlyText.flush(this.levelRenderState, this.submitNodeStorage);
	}
}
