package com.sephiria.client.render;

import com.sephiria.Sephiria;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

/**
 * 武器自带的「附魔式微光」：匕首红白、刀蓝白。
 *
 * <p>26.1.2 的 GLINT 管线只有一个 {@code Sampler0}：原版 {@code RenderTypes} 直接把它绑到
 * {@code enchanted_glint_item.png}，所以没有数据驱动的换色口子。这里照原版 {@code glint} /
 * {@code entity_glint} 的写法自己建渲染类型，只把 {@code Sampler0} 换成自己的贴图；
 * 贴图是原版 {@code enchanted_glint_item.png} 按亮度重映射出来的——暗部是目标色、亮部推到白色，
 * 所以看起来是红白 / 蓝白，而不是一块纯色。
 *
 * <p>哪件武器用哪一套由 {@link com.sephiria.mixin.client.WeaponGlintMixin} 按<b>物品贴图名</b>分流：
 * 剑盾 / 大剑 / 长棍走不到这里，仍然是普通附魔紫。匕首有两张贴图（背包图标 {@code item/dagger}
 * 与手持 {@code item/dagger_3d}），两处都要换，不然一拿在手里颜色就变回紫色。
 */
public final class WeaponGlint {
	private static final Identifier DAGGER_GLINT = id("textures/misc/dagger_glint.png");
	private static final Identifier BLADE_GLINT = id("textures/misc/blade_glint.png");
	private static final Identifier DAGGER_TEXTURE = id("item/dagger");
	private static final Identifier DAGGER_HAND_TEXTURE = id("item/dagger_3d");
	private static final Identifier BLADE_TEXTURE = id("item/blade");

	/** 建一次就够（RenderType 会按名字进原版的缓存）；模型烘焙期间会被反复问，所以缓存住。 */
	private static RenderType dagger;
	private static RenderType blade;
	private static RenderType katana;

	private WeaponGlint() {
	}

	/** 这件物品贴图对应的自定义微光；不是这两把武器就返回 null。 */
	public static RenderType forTexture(Identifier texture) {
		if (texture.equals(DAGGER_TEXTURE) || texture.equals(DAGGER_HAND_TEXTURE)) {
			if (dagger == null) {
				dagger = create("sephiria_dagger_glint", DAGGER_GLINT, TextureTransform.GLINT_TEXTURING);
			}

			return dagger;
		}

		if (texture.equals(BLADE_TEXTURE)) {
			if (blade == null) {
				blade = create("sephiria_blade_glint", BLADE_GLINT, TextureTransform.GLINT_TEXTURING);
			}

			return blade;
		}

		return null;
	}

	/**
	 * 刀拿在手里时 GeckoLib 那一遍的 glint 渲染类型。
	 *
	 * <p>GeckoLib 的模型不走物品 quad，{@code ItemFeatureRenderer} 那套够不到它；所以由
	 * {@link WeaponGlintLayer} 在 GeckoLib 的渲染层里用这个渲染类型再画一遍模型。
	 * 管线用 GLINT，UV 变换用 ENTITY_GLINT_TEXTURING——和原版附魔盔甲的第二遍完全同构。
	 */
	public static RenderType katanaGlint() {
		if (katana == null) {
			katana = create("sephiria_blade_entity_glint", BLADE_GLINT,
					TextureTransform.ENTITY_GLINT_TEXTURING);
		}

		return katana;
	}

	/**
	 * 与 26.1.2 原版 {@code RenderTypes.GLINT} / {@code ENTITY_GLINT} 同构：
	 * 只把 {@code Sampler0} 换成自定义微光贴图，UV 变换决定是物品还是实体那一套。
	 */
	private static RenderType create(String name, Identifier glintTexture, TextureTransform transform) {
		RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
				.withTexture("Sampler0", glintTexture)
				.setTextureTransform(transform)
				.createRenderSetup();

		return RenderType.create(name, setup);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, path);
	}
}
