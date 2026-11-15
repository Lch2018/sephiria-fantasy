package com.sephiria.client.backpack;

import net.minecraft.world.inventory.Slot;

/**
 * 由界面自己绘制旋转石板的标记接口。
 *
 * <p>1.20.1 的 {@code AbstractContainerScreen#renderSlot} 是 private，没法像 26.x 那样在子类里
 * 整个跳过某一格；{@code AbstractContainerScreenMixin} 在 HEAD 处问界面「这一格你自己画吗」，
 * 是的话就取消原版绘制。旧图标根本不会进渲染缓冲区，也就不会再出现「旧图标盖住旋转图标」。
 */
public interface RotatedSlateScreen {
	/** 这一格是否由界面自己画（原版 renderSlot 不再画它的未旋转图标）。 */
	boolean sephiria$handlesRotatedSlot(Slot slot);
}
