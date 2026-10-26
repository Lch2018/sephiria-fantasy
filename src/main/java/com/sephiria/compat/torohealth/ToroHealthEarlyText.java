package com.sephiria.compat.torohealth;

import net.kairost.torohealth.ToroHealth;
import net.kairost.torohealth.client.particle.TextRenderEntry;
import net.kairost.torohealth.client.particle.TextRenderQueue;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/**
 * 真正碰 ToroHealth 类型的客户端辅助类。
 *
 * <p>{@code LevelRendererTextEarlyMixin} 挂在原版 {@code LevelRenderer} 上，**没装 ToroHealth
 * 时也会被应用**，所以那个 mixin 本身不能出现任何 ToroHealth 的类型引用——否则就是新实例里
 * 只有本模组时，打开世界直接 {@code NoClassDefFoundError: TextRenderQueue}。
 * mixin 里先用 {@code FabricLoader.isModLoaded("torohealth")} 判断，然后才调用这里；
 * 这里才会真正加载 ToroHealth 的类。
 */
public final class ToroHealthEarlyText {
	private ToroHealthEarlyText() {
	}

	public static void flush(LevelRenderState renderState, SubmitNodeStorage storage) {
		for (TextRenderEntry entry : TextRenderQueue.consume()) {
			ToroHealth.textParticleRenderer.render(entry.text(),
					renderState.cameraRenderState.orientation,
					entry.x(), entry.y(), entry.z(), entry.u(), entry.v(), entry.color(),
					storage, entry.light());
		}
	}
}
