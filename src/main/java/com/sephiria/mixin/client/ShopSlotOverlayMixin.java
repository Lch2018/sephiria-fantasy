package com.sephiria.mixin.client;

import com.sephiria.client.shop.ShopScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 商店格子的浮层（价格 / 售罄蒙层 / 宝箱剩余次数）。
 *
 * <p>1.20.1 的 {@code renderSlot} 是私有的，模组界面没法 override；但 Mixin 可以注入私有方法，
 * 在图标画完之后调回 {@link ShopScreen#drawSlotDecoration}，顺序正好压在最上面。
 * 只对 {@link ShopScreen} 生效，其它容器界面不受影响（只是每次画格子多一次 instanceof）。
 */
@Mixin(AbstractContainerScreen.class)
public class ShopSlotOverlayMixin {
	@Inject(method = "renderSlot", at = @At("TAIL"))
	private void sephiria$shopSlotDecoration(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
		if ((Object) this instanceof ShopScreen screen) {
			screen.drawSlotDecoration(graphics, slot);
		}
	}
}
