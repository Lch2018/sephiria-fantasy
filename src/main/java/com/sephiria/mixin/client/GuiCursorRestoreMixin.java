package com.sephiria.mixin.client;

import com.sephiria.client.CursorKeeper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 光标还原的落点：26.3 的 {@code Gui.setScreen} 里，关容器时走 grabMouse、开界面时走 releaseMouse，
 * 两次都会把光标 warp 到窗口中心。这里在方法<b>整个返回之后</b>（screen.init / resyncMousePosition /
 * updateTitle 都跑完了）把标签切换前记下的位置放回去，保证后面没有原版代码再覆盖它。
 *
 * <p>只在 {@link CursorKeeper} 有「待还原」状态时动作，平时对任何界面切换都没有影响。
 */
@Mixin(Gui.class)
public abstract class GuiCursorRestoreMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "setScreen", at = @At("RETURN"))
	private void sephiria$restoreCursor(Screen screen, CallbackInfo ci) {
		CursorKeeper.afterSetScreen(this.minecraft, screen);
	}
}
