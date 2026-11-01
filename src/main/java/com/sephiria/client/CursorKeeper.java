package com.sephiria.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.sdl.SDLMouse;

/**
 * 切标签时把光标留在原处。
 *
 * <p>26.3 从容器界面（赛菲莉娅背包 / 商店 / 附魔页）换到普通界面时，光标会被原版拉回窗口中心，
 * 链路是：标签栏先关容器 → {@code LocalPlayer.closeContainer()} → {@code Gui.setScreen(null)} 走
 * 「回到游戏」分支 → {@code MouseHandler.grabMouse()}（把 xpos/ypos 写成屏幕中心、
 * SDL_WarpMouseInWindow 到中心、打开相对鼠标模式），紧接着 {@code Gui.setScreen(目标界面)}
 * 又调 {@code releaseMouse()}（同样写中心、再 warp 一次、关掉相对模式）。
 * 两次 warp 都是原版行为，在调用点绕不开，所以这里改成：点标签、关容器<b>之前</b>先记下
 * 光标在窗口内的像素坐标（{@link #capture}），{@code Gui.setScreen} 返回时由
 * {@code GuiCursorRestoreMixin} 调 {@link #afterSetScreen} 把 MouseHandler 的坐标与真实光标一起放回去。
 *
 * <p>刻意<b>不碰</b> {@code mouseGrabbed} 与相对鼠标模式：之前那版手动开关 SDL 相对模式、
 * 用 SDL_WarpMouseGlobal 反复 warp，结果不是没效果就是光标直接消失。这里只调用原版自己也在用的
 * {@code SDL_WarpMouseInWindow}（与 {@code InputConstants.releaseMouse} 是同一条调用），
 * 并且只在「一次标签切换」期间生效，平时任何界面切换都不受影响。
 */
public final class CursorKeeper {
	/**
	 * 待还原状态的存活时间。选卡页是「点标签 → 发包 → 服务端回包才开界面」，要跨 tick 等；
	 * 但也不能无限等：回包丢了的话，不能让下一次无关的 setScreen 把光标拉到一个过期的位置。
	 */
	private static final long PENDING_TIMEOUT_MS = 3000L;

	/** 是否有待还原的位置。 */
	private static boolean pending;
	/** 待还原的位置：窗口内的物理像素，与 {@code MouseHandler.xpos/ypos} 同坐标系。 */
	private static double pendingX;
	private static double pendingY;
	private static long pendingSince;

	private CursorKeeper() {
	}

	/** 记下光标当前位置。已经在等还原时不覆盖（先记下的那次才是关容器前的位置）。 */
	public static void capture(Minecraft client) {
		if (client.mouseHandler == null || isArmed()) {
			return;
		}

		MouseHandler mouse = client.mouseHandler;
		// 未抓取时从 SDL 读真实位置；抓取中（没有界面）这里读到的是上次 grabMouse 写的中心，
		// 不能用——但那种情况本来也不会走到标签栏。
		mouse.resyncMousePosition();
		pendingX = mouse.xpos();
		pendingY = mouse.ypos();
		pending = true;
		pendingSince = System.currentTimeMillis();
	}

	/** 关掉容器菜单，但关之前先记下光标位置（关容器本身会把光标拉回中心）。 */
	public static void closeContainer(Minecraft client) {
		if (client.player == null) {
			return;
		}

		capture(client);
		client.player.closeContainer();
	}

	/** 打开一个客户端界面；光标还原由 mixin 在 setScreen 返回时完成。 */
	public static void show(Minecraft client, Screen screen) {
		capture(client);
		client.setScreenAndShow(screen);
	}

	/** 放弃这次还原（切到由服务端打开的容器页时用，免得过期位置落到别的界面上）。 */
	public static void cancel() {
		pending = false;
	}

	/**
	 * {@code Gui.setScreen} 返回时调用。
	 *
	 * <p>{@code screen == null} 是关容器那一步的过渡界面：此时鼠标正被原版抓取、相对模式开着，
	 * 那一刻动光标既没意义也不安全，所以保留待还原状态，等真正的目标界面。
	 */
	public static void afterSetScreen(Minecraft client, Screen screen) {
		if (!pending) {
			return;
		}

		if (!isArmed()) {
			pending = false;
			return;
		}

		if (screen == null) {
			return;
		}

		pending = false;

		MouseHandler mouse = client.mouseHandler;

		if (mouse == null || mouse.isMouseGrabbed()) {
			// 抓取中等于「在游戏里」，光标归原版管，我们不能动
			return;
		}

		// 先 warp 真实光标，再写回逻辑坐标：SDL 的 warp 要等下一次事件轮询才反映到
		// xpos/ypos，直接写能省掉一帧的错位（此时相对模式已被 releaseMouse 关掉，
		// 所以这一次 warp 和原版 releaseMouse 里那次是同一个语义，只是目标点换成原位置）。
		SDLMouse.SDL_WarpMouseInWindow(client.getWindow().handle(), (float) pendingX, (float) pendingY);
		mouse.xpos = pendingX;
		mouse.ypos = pendingY;
	}

	/** 待还原状态是否还有效（超时就当作没记过）。 */
	private static boolean isArmed() {
		return pending && System.currentTimeMillis() - pendingSince <= PENDING_TIMEOUT_MS;
	}
}
