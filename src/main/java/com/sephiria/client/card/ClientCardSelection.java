package com.sephiria.client.card;

import com.sephiria.card.CardSelection;
import com.sephiria.network.CardSelectionPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端侧的选卡镜像：服务端把三张卡推过来，界面从这里取。
 *
 * <p>只做缓存与拷贝：能否刷新 / 能否选择仍由服务端判，这里的数据只用于绘制。
 */
public final class ClientCardSelection {
	private static List<ItemStack> cards = List.of();
	private static final boolean[] REFRESHED = new boolean[CardSelection.CARDS];
	private static int chances;
	private static float chargeProgress;
	/**
	 * 主动关掉选卡界面后的一小段「静默期」。
	 *
	 * <p>关界面时可能还有一个刷新回包在路上；那个包带着 {@code open = true}，
	 * 如果直接按它开界面，玩家刚关掉的窗口会自己弹回来。静默期内不自动开，
	 * 但玩家点标签栏主动打开时会先清掉它（那是明确的意图）。
	 */
	private static long suppressUntil;

	private ClientCardSelection() {
	}

	/** 服务端发来一份权威状态（由 {@code SephiriaClient} 的包处理器调用）。 */
	public static void accept(CardSelectionPayload payload) {
		List<ItemStack> copied = new ArrayList<>(CardSelection.CARDS);

		for (int index = 0; index < CardSelection.CARDS; index++) {
			copied.add(index < payload.cards().size() ? payload.cards().get(index).copy() : ItemStack.EMPTY);
			REFRESHED[index] = payload.refreshUsed(index);
		}

		cards = List.copyOf(copied);
		chances = Math.max(0, payload.chances());
		chargeProgress = Math.max(0.0F, Math.min(1.0F, payload.chargeProgress()));
	}

	/** 清空（退出服务器时调用，免得上个服的三张卡留到下一个服）。 */
	public static void clear() {
		cards = List.of();
		chances = 0;
		chargeProgress = 0.0F;
		suppressUntil = 0L;

		for (int index = 0; index < REFRESHED.length; index++) {
			REFRESHED[index] = false;
		}
	}

	public static List<ItemStack> cards() {
		return cards;
	}

	public static boolean refreshed(int index) {
		return index >= 0 && index < REFRESHED.length && REFRESHED[index];
	}

	public static int chances() {
		return chances;
	}

	public static float chargeProgress() {
		return chargeProgress;
	}

	/** 关掉界面后压一小段时间的自动打开（刷新回包可能在路上）。 */
	public static void suppressAutoOpen() {
		suppressUntil = System.currentTimeMillis() + 1000L;
	}

	public static boolean autoOpenSuppressed() {
		return System.currentTimeMillis() < suppressUntil;
	}

	/** 玩家自己点标签栏打开：这是明确的打开意图，清掉静默期。 */
	public static void clearSuppress() {
		suppressUntil = 0L;
	}
}
