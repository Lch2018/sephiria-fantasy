package com.sephiria.client.render;

import com.sephiria.Sephiria;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * 武器自带的「附魔式微光」：匕首红白、刀蓝白。
 *
 * <p>原版附魔光效的颜色写死在 GLINT 管线的 GlintSampler 里（紫色），没有数据驱动的换色口子。
 * 这里照原版 {@code Sheets} 的写法自己建一套 glint 渲染类型，只把 GlintSampler 换成自己的贴图；
 * 贴图是原版 {@code enchanted_glint_item.png} 按亮度重映射出来的——暗部是目标色、亮部推到白色，
 * 所以看起来是红白 / 蓝白，而不是一块纯色。
 *
 * <p>哪件武器用哪一套由 {@link WeaponGlintMixin} 按<b>物品贴图名</b>分流：剑盾 / 大剑 / 长棍
 * 走不到这里，仍然是普通附魔紫。匕首有两张贴图（背包图标 {@code item/dagger} 与手持
 * {@code item/dagger_3d}），两处都要换，不然一拿在手里颜色就变回紫色。
 */
public final class WeaponGlint {
	private static final Identifier DAGGER_GLINT = id("textures/misc/dagger_glint.png");
	private static final Identifier BLADE_GLINT = id("textures/misc/blade_glint.png");
	private static final Identifier DAGGER_TEXTURE = id("item/dagger");
	private static final Identifier DAGGER_HAND_TEXTURE = id("item/dagger_3d");
	private static final Identifier BLADE_TEXTURE = id("item/blade");
	/** 刀拿在手里走 GeckoLib 的实体模型，贴图是这张（见 {@code SephiriaKatanaItem#createGeoRenderer}）。 */
	private static final Identifier KATANA_TEXTURE = id("textures/item/katana.png");

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
				dagger = create("sephiria_dagger_glint", DAGGER_GLINT);
			}

			return dagger;
		}

		if (texture.equals(BLADE_TEXTURE)) {
			if (blade == null) {
				blade = create("sephiria_blade_glint", BLADE_GLINT);
			}

			return blade;
		}

		return null;
	}

	/**
	 * 刀拿在手里时 GeckoLib 那一遍的 glint 渲染类型。
	 *
	 * <p>GeckoLib 的模型不走物品 quad，`MaterialInfo` 那套够不到它；所以由
	 * {@link WeaponGlintLayer} 在 GeckoLib 的渲染层里用这个渲染类型再画一遍模型。
	 * 管线用实体那一套（{@code ENTITY_SOLID_GLINT}），只把 GlintSampler 换成蓝白贴图，
	 * UV 变换用 ENTITY_GLINT_TEXTURING——和原版附魔盔甲的第二遍完全同构。
	 */
	public static RenderType katanaGlint() {
		if (katana == null) {
			RenderSetup setup = RenderSetup.builder(RenderPipelines.ENTITY_SOLID_GLINT)
					.withTexture("Sampler0", KATANA_TEXTURE)
					.withTexture("GlintSampler", BLADE_GLINT)
					.setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
					.useLightmap()
					.useOverlay()
					.affectsCrumbling()
					.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
					.createRenderSetup();

			katana = RenderType.create("sephiria_blade_entity_glint", setup);
		}

		return katana;
	}

	/**
	 * 与 {@code Sheets} 里原版 {@code item_cutout_glint} 同构：Sampler0 是物品图集、
	 * GlintSampler 换成自定义贴图，UV 变换用 GLINT_TEXTURING（附魔光效的斜向流动感）。
	 */
	private static RenderType create(String name, Identifier glintTexture) {
		RenderSetup setup = RenderSetup.builder(RenderPipelines.ITEM_CUTOUT_GLINT)
				.withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
				.withTexture("GlintSampler", glintTexture)
				.setTextureTransform(TextureTransform.GLINT_TEXTURING)
				.useLightmap()
				.useOverlay()
				.affectsCrumbling()
				.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
				.createRenderSetup();

		return RenderType.create(name, setup);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, path);
	}
}
