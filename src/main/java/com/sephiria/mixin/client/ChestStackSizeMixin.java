package com.sephiria.mixin.client;

import com.sephiria.artifact.ChestItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 宝箱的「没开过叠 16 / 开过只 1 个」。
 *
 * <p>1.20.1 的堆叠上限是物品级常量（{@code Item.Properties#stacksTo}），没有 26.x 的
 * max_stack_size 数据组件，所以这里在 {@code ItemStack#getMaxStackSize} 返回前按这一份的 NBT
 * 覆写：开过的宝箱（{@link ChestItem#opened}）返回 1，没开过的走物品自己的 16。
 * 合并判的是「同物品 + 同 NBT」，开过的和没开过的自然也不会并堆。
 */
@Mixin(ItemStack.class)
public class ChestStackSizeMixin {
	@Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
	private void sephiria$openedChestSize(CallbackInfoReturnable<Integer> callback) {
		ItemStack self = (ItemStack) (Object) this;

		if (self.getItem() instanceof ChestItem && ChestItem.opened(self)) {
			callback.setReturnValue(1);
		}
	}
}
