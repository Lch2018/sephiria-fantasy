package com.sephiria.client.render;

import com.sephiria.Sephiria;
import com.sephiria.weapon.SephiriaDaggerItem;
import com.sephiria.weapon.SephiriaKatanaItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SephiriaGlintRenderTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 武器自带的「附魔式微光」：匕首红白、刀蓝白。
 *
 * <p>26.x 的 GLINT 管线可以通过 {@code RenderSetup} 换 {@code Sampler0}；1.20.1 的
 * {@code RenderType.glint()} 把贴图写死在 {@code ItemRenderer.ENCHANTED_GLINT_ITEM}，
 * 所以由 {@code SephiriaGlintRenderTypes}（放在原版包里）照原版那套状态片重建一份，
 * 只把贴图换成按亮度重映射出来的红白 / 蓝白（暗部是目标色、亮部推到白色）。
 *
 * <p>哪件武器用哪一套由 {@link com.sephiria.mixin.client.WeaponGlintMixin} 在
 * {@code ItemRenderer.render} 里按物品类分流：匕首有两张贴图（背包图标与手持），
 * 都走同一个物品渲染入口，所以一拿在手里也不会变回紫色。
 *
 * <p>刀拿在手里时是 GeckoLib 骨骼模型，不走物品 quad；26.x 那层 GeckoLib glint 还没移植，
 * 这里只覆盖刀在背包 / 地面 / 展示框里的模型。
 */
public final class WeaponGlint {
	private static final ResourceLocation DAGGER_GLINT = id("textures/misc/dagger_glint.png");
	private static final ResourceLocation BLADE_GLINT = id("textures/misc/blade_glint.png");

	/** 建一次就够（RenderType 自己会按名字进缓存）。 */
	private static RenderType dagger;
	private static RenderType daggerDirect;
	private static RenderType blade;
	private static RenderType bladeDirect;

	private WeaponGlint() {
	}

	/** 这件物品对应的自定义微光；不是匕首 / 刀就返回 null。 */
	public static RenderType forStack(ItemStack stack, boolean direct) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		if (stack.getItem() instanceof SephiriaDaggerItem) {
			if (direct) {
				if (daggerDirect == null) {
					daggerDirect = SephiriaGlintRenderTypes.itemGlint("sephiria_dagger_glint_direct", DAGGER_GLINT, true);
				}

				return daggerDirect;
			}

			if (dagger == null) {
				dagger = SephiriaGlintRenderTypes.itemGlint("sephiria_dagger_glint", DAGGER_GLINT, false);
			}

			return dagger;
		}

		if (stack.getItem() instanceof SephiriaKatanaItem) {
			if (direct) {
				if (bladeDirect == null) {
					bladeDirect = SephiriaGlintRenderTypes.itemGlint("sephiria_blade_glint_direct", BLADE_GLINT, true);
				}

				return bladeDirect;
			}

			if (blade == null) {
				blade = SephiriaGlintRenderTypes.itemGlint("sephiria_blade_glint", BLADE_GLINT, false);
			}

			return blade;
		}

		return null;
	}

	private static ResourceLocation id(String path) {
		return new ResourceLocation(Sephiria.MOD_ID, path);
	}
}
