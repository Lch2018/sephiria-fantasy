package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

/**
 * 在 {@code net.minecraft.client.renderer} 包里开一个窄口子：{@code RenderType.create} 与
 * {@code RenderStateShard} 的 glint 状态片都是包私有的，模组包外拿不到。
 *
 * <p>只被 {@code com.sephiria.client.render.WeaponGlint} 调用：照原版 {@code glint} /
 * {@code glint_direct} 的写法建同构的渲染类型，把贴图换成武器专属的微光贴图。
 * 这是 1.20.1 移植的折中——26.x 有数据驱动的 {@code RenderSetup}，1.20.1 只能从包内组装。
 */
public final class SephiriaGlintRenderTypes {
	private SephiriaGlintRenderTypes() {
	}

	/** GeckoLib 手持模型的微光：同原版 {@code RenderType.entityGlint()}（实体 UV 动画 + ITEM_ENTITY_TARGET）。 */
	public static RenderType entityGlint(String name, ResourceLocation texture) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, true, false))
				.setWriteMaskState(RenderStateShard.COLOR_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
				.setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
				.setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
				.setTexturingState(RenderStateShard.ENTITY_GLINT_TEXTURING)
				.createCompositeState(false);

		return RenderType.create(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, state);
	}

	/** 物品在背包 / 地面 / 展示框里的微光：同原版 {@code RenderType.glint()}。 */
	public static RenderType itemGlint(String name, ResourceLocation texture, boolean direct) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(direct ? RenderStateShard.RENDERTYPE_GLINT_DIRECT_SHADER
						: RenderStateShard.RENDERTYPE_GLINT_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(texture, true, false))
				.setWriteMaskState(RenderStateShard.COLOR_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
				.setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
				.setTexturingState(RenderStateShard.GLINT_TEXTURING)
				.createCompositeState(false);

		return RenderType.create(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, state);
	}
}
