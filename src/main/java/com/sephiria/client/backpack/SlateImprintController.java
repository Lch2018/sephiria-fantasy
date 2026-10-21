package com.sephiria.client.backpack;

import com.sephiria.backpack.BackpackGridMenu;
import com.sephiria.client.ClientTalents;
import com.sephiria.network.SlateImprintPayload;
import com.sephiria.slate.SlateBaseItem;
import com.sephiria.slate.SlateItem;
import com.sephiria.talent.PatienceTalent;
import com.sephiria.talent.Talent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.Slot;

/**
 * 客户端的长按 F 刻印状态：鼠标指着一块石板、按住 F {@value #IMPRINT_MILLIS} 毫秒，
 * 把进度写进石板物品的 {@link SlateItem#IMPRINT} 组件，原版就会用耐久条把它画出来。
 *
 * <p>按键改成 F 之后不再拦截鼠标左键：左键恢复原版「按下即拿起 / 拖动」行为
 * （用户 2026-10-21 明确去掉了之前「按下不拿起、松手才拿起」的验证）。
 *
 * <p>为什么用 {@link System#currentTimeMillis()} 而不是 tick：打开界面时客户端 tick 可能停住，
 * 用 tick 算进度条会一直停在 0；真实时间不受影响。按键状态由界面每帧用
 * {@code InputConstants.isKeyDown(InputConstants.KEY_F)} 轮询（26.3 的 Screen 没有 keyReleased）。
 *
 * <p>进度只写在客户端那份物品副本上；攒满时发一个只带菜单格子编号的包，服务端再按真实背包下标
 * 删除石板、写进 {@code SlateImprint} 存档。中途松开 F 取消，石板留在格子里。
 */
public final class SlateImprintController {
	/** 攒满需要的真实时间（毫秒）。 */
	public static final int IMPRINT_MILLIS = 1500;

	private static Slot slot;
	private static long startMillis;
	private static boolean sent;

	private SlateImprintController() {
	}

	/**
	 * 每帧推进一次（由背包页 / 附魔页的 {@code extractRenderState} 调）。
	 *
	 * @param held F 键当前是否按住（界面轮询 {@code InputConstants.isKeyDown}）
	 */
	public static void tickKey(BackpackGridMenu menu, Slot hovered, boolean held) {
		if (!held) {
			if (slot != null) {
				cancel();
			}

			// 松开 F 才算一次「长按」结束；下一次按住重新开始
			sent = false;
			return;
		}

		// 这一轮已经刻印成功：等松开 F 之前不再重复发
		if (sent) {
			return;
		}

		if (slot == null) {
			if (!begin(menu, hovered)) {
				return;
			}
		} else if (hovered != slot) {
			// 鼠标移开：取消这次长按；还按着 F 移回来会重新从 0 开始
			cancel();
			return;
		}

		long elapsed = System.currentTimeMillis() - startMillis;
		int progress = (int) Math.min(100L, Math.max(1L, elapsed * 100L / IMPRINT_MILLIS));

		if (slot.hasItem()) {
			slot.getItem().set(SlateItem.IMPRINT, progress);
		}

		if (progress >= 100) {
			sent = true;
			ClientPlayNetworking.send(new SlateImprintPayload(slot.index));
		}
	}

	/** 这一格能不能开始长按刻印（F 按住、鼠标指着背包里的普通石板）。 */
	private static boolean begin(BackpackGridMenu menu, Slot hovered) {
		sent = false;

		if (hovered == null || !hovered.hasItem()) {
			return false;
		}

		if (ClientTalents.stage(Talent.SURVIVAL) < 3) {
			return false;
		}

		if (!menu.isBackpackSlot(hovered) || menu.backpackIndex(hovered) < 0) {
			return false;
		}

		if (!(hovered.getItem().getItem() instanceof SlateItem)
				|| hovered.getItem().getItem() instanceof SlateBaseItem
				|| PatienceTalent.isBase(hovered.getItem())) {
			return false;
		}

		slot = hovered;
		startMillis = System.currentTimeMillis();
		hovered.getItem().set(SlateItem.IMPRINT, 1);
		return true;
	}

	/** 取消 / 松开 F / 关界面：把进度条从物品副本上清掉。 */
	public static void cancel() {
		if (slot != null && slot.hasItem()) {
			slot.getItem().remove(SlateItem.IMPRINT);
		}

		slot = null;
		startMillis = 0L;
	}

	/** 正在刻印的格子（界面用来画进度条；没有在刻印时是 null）。 */
	public static Slot activeSlot() {
		return slot;
	}

	/** 当前进度（0..1）。界面自己画条用——不依赖物品组件同步，旋转过的石板也照样显示。 */
	public static float progress() {
		if (slot == null) {
			return 0.0F;
		}

		if (sent) {
			return 1.0F;
		}

		long elapsed = System.currentTimeMillis() - startMillis;
		return Math.min(1.0F, elapsed / (float) IMPRINT_MILLIS);
	}
}
