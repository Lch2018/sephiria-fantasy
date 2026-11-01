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
		pendingX = globalX.get(0) - windowX.get(0);
		pendingY = globalY.get(0) - windowY.get(0);
		pendingTicks = 3;

		client.setScreenAndShow(screen);
		client.mouseHandler.resyncMousePosition();
	}

	private static void tick(Minecraft client) {
		if (pendingTicks <= 0) {
			return;
		}

		pendingTicks--;
		SDLMouse.SDL_WarpMouseInWindow(client.getWindow().handle(), pendingX, pendingY);
		client.mouseHandler.resyncMousePosition();
	}
}
