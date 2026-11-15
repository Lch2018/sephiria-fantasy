package com.sephiria.mixin.client;

import com.sephiria.client.backpack.RotatedSlateScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让界面自己接管旋转石板的图标绘制（1.20.1 版）。
 *
 * <p>26.x 直接在子类重写 {@code extractSlot}、不调 super 即可；1.20.1 的 {@code renderSlot}
 * 是 private，只能在 HEAD 取消。取消的是整格绘制（图标 + 数量/耐久装饰 + 悬停高亮），
 * 界面随后在 {@code renderSlotOverlays} 里画旋转图标，并调用 {@code restoreSlotHighlight}
 * 把悬停高亮补回来。
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	@Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
	private void sephiria$skipRotatedSlate(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
		if (this instanceof RotatedSlateScreen screen && screen.sephiria$handlesRotatedSlot(slot)) {
			ci.cancel();
		}
	}
}
