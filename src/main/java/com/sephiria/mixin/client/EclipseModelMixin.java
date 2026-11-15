package com.sephiria.mixin.client;

import com.sephiria.Sephiria;
import com.sephiria.artifact.EternalEclipseItem;
import com.sephiria.compat.ItemData;
import com.sephiria.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 永恒蚀按「在背包哪半边」换图标（1.20.1 版）。
 *
 * <p>26.x 的物品模型用 {@code minecraft:select} 直接读数据组件；1.20.1 只能走
 * {@code ItemProperties} + {@code overrides}。但那套谓词是在<b>模型烘焙时</b>解析的，
 * 注册晚一步（或资源重载时机不对）就会永久落成 {@code -Infinity}，覆盖项全部失效，
 * 左右两边都回到默认图标。这里不再依赖谓词：在 {@code ItemRenderer.getModel} 返回前
 * 按 NBT 直接换成已经烘好的火焰 / 冰霜模型，所有渲染上下文（GUI / 地面 / 手持）都生效。
 *
 * <p>火焰 / 冰霜模型本来就被 {@code eternal_eclipse.json} 的 overrides 引用、一定已烘焙；
 * 找不到时保留原模型，不会画成紫黑格。
 */
@Mixin(ItemRenderer.class)
public class EclipseModelMixin {
	private static final ModelResourceLocation FLAME = new ModelResourceLocation(
			Sephiria.MOD_ID, "eternal_eclipse_flame", "inventory");
	private static final ModelResourceLocation FROST = new ModelResourceLocation(
			Sephiria.MOD_ID, "eternal_eclipse_frost", "inventory");
	/** 诊断日志条数上限：确认 NBT 读到了、烘焙模型也找到了。 */
	private static int diagnostics;

	@Inject(method = "getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;",
			at = @At("RETURN"), cancellable = true)
	private void sephiria$eclipseModel(ItemStack stack, Level level, LivingEntity entity, int seed,
			CallbackInfoReturnable<BakedModel> callback) {
		if (!stack.is(ModItems.ETERNAL_ECLIPSE)) {
			return;
		}

		int side = ItemData.getInt(stack, EternalEclipseItem.SIDE, 0);

		if (side <= 0) {
			return;
		}

		BakedModel model = Minecraft.getInstance().getModelManager()
				.getModel(side == EternalEclipseItem.Side.RIGHT.id() ? FROST : FLAME);
		boolean found = model != null && model != Minecraft.getInstance().getModelManager().getMissingModel();

		if (diagnostics < 10) {
			diagnostics++;
			Sephiria.LOGGER.info("[SEPHIRIA] eclipse model: side={}, found={}", side, found);
		}

		if (found) {
			callback.setReturnValue(model);
		}
	}
}
