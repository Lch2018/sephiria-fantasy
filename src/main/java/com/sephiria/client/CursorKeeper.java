package com.sephiria.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLVideo;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 切界面前后保住鼠标位置：26.3 从容器界面切普通界面会把光标拉回窗口中心。
 *
 * <p>回中发生在 setScreen 之后、甚至下一帧，所以不能只在 setScreen 后立刻 warp；
 * 这里记下切换前的位置，在接下来两个客户端 tick 里各 warp 一次。
 */
public final class CursorKeeper {
	private static float pendingX;
	private static float pendingY;
	private static float pendingGlobalX;
	private static float pendingGlobalY;
	private static int pendingTicks;

	private CursorKeeper() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
	}

	public static void show(Minecraft client, Screen screen) {
		FloatBuffer globalX = BufferUtils.createFloatBuffer(1);
		FloatBuffer globalY = BufferUtils.createFloatBuffer(1);
		SDLMouse.SDL_GetGlobalMouseState(globalX, globalY);
		IntBuffer windowX = BufferUtils.createIntBuffer(1);
		IntBuffer windowY = BufferUtils.createIntBuffer(1);
		SDLVideo.SDL_GetWindowPosition(client.getWindow().handle(), windowX, windowY);
		pendingGlobalX = globalX.get(0);
		pendingGlobalY = globalY.get(0);
		pendingX = pendingGlobalX - windowX.get(0);
		pendingY = pendingGlobalY - windowY.get(0);
		pendingTicks = 3;

		client.setScreenAndShow(screen);
		client.mouseHandler.resyncMousePosition();
	}

	private static void tick(Minecraft client) {
		if (pendingTicks <= 0) {
			return;
		}

		pendingTicks--;
		long window = client.getWindow().handle();
		boolean relative = SDLMouse.SDL_GetWindowRelativeMouseMode(window);

		if (relative) {
			SDLMouse.SDL_SetWindowRelativeMouseMode(window, false);
		}

		// 全局坐标优先：窗口坐标在部分 DPI / 相对模式下会被忽略
		if (!SDLMouse.SDL_WarpMouseGlobal(pendingGlobalX, pendingGlobalY)) {
			SDLMouse.SDL_WarpMouseInWindow(window, pendingX, pendingY);
		}

		if (relative) {
			SDLMouse.SDL_SetWindowRelativeMouseMode(window, true);
		}

		client.mouseHandler.resyncMousePosition();
	}
}
