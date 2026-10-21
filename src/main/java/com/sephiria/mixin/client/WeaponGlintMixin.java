package com.sephiria.mixin.client;

import com.mojang.blaze3d.platform.Transparency;
import com.sephiria.client.render.WeaponGlint;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把匕首 / 刀的物品模型换成自定义颜色的附魔光效。
 *
 * <p>26.3 的物品光效在烘焙模型时就把「用哪套 glint 渲染类型」写进了 {@code MaterialInfo}
 * （原版按物品图集选 {@code Sheets.cutoutItemGlintSheet()}），之后渲染只照着这个字段取。
 * 所以在 {@code MaterialInfo.of} 返回前，按<b>贴图名</b>把 glint 那一项换成
 * {@link WeaponGlint} 自己建的渲染类型；没有匹配的贴图就原样放行（其它物品仍是紫色）。
 *
 * <p>关不显示由物品自己的 {@code isFoil} 控制：匕首有专注、刀满剑意时返回 true，
 * 渲染器才会走 glint 那一遍。
 */
@Mixin(targets = "net.minecraft.client.resources.model.geometry.BakedQuad$MaterialInfo")
public class WeaponGlintMixin {
	// require = 0：万一以后原版把 MaterialInfo.of 改名/改签名，退化成「没有自定义光效」而不是整局起不来
	@Inject(method = "of", at = @At("RETURN"), cancellable = true, require = 0)
	private static void sephiria$replaceGlint(Material.Baked material, Transparency transparency, int tintIndex,
			Direction shadeDirection, int lightEmission,
			CallbackInfoReturnable<BakedQuad.MaterialInfo> callback) {
		BakedQuad.MaterialInfo info = callback.getReturnValue();
		RenderType glint = WeaponGlint.forTexture(info.sprite().contents().name());

		if (glint == null) {
			return;
		}

		// SPECIAL 那一项也一起换：这两把武器不是指南针/时钟，正常只走 STANDARD，
		// 但两个都填上可以避免以后贴图带 mcmeta 时又变回紫色
		callback.setReturnValue(new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(),
				glint, glint, info.tintIndex(), info.shadeDirectionOverride(), info.lightEmission()));
	}
}
