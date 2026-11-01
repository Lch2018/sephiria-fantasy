package com.sephiria.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.sdl.SDLMouse;

import java.nio.FloatBuffer;

/** 切界面前后保住鼠标位置：26.3 从容器界面切普通界面会把光标拉回窗口中心。 */
public final class CursorKeeper {
	private CursorKeeper() {
	}

	public static void show(Minecraft client, Screen screen) {
		FloatBuffer x = BufferUtils.createFloatBuffer(1);
		FloatBuffer y = BufferUtils.createFloatBuffer(1);
		SDLMouse.SDL_GetMouseState(x, y);
		float mouseX = x.get(0);
		float mouseY = y.get(0);

		client.setScreenAndShow(screen);

		long window = client.getWindow().handle();
		// 立即对齐一次，下一帧再 warp 一次：原版回中在 setScreen 之后才生效
		client.mouseHandler.resyncMousePosition();
		client.execute(() -> {
			SDLMouse.SDL_WarpMouseInWindow(window, mouseX, mouseY);
			client.mouseHandler.resyncMousePosition();
		});
	}
}
