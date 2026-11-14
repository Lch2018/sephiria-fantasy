package com.sephiria.client.backpack;

import com.sephiria.backpack.BackpackGridMenu;
import com.sephiria.client.ClientTalents;
import com.sephiria.compat.ItemData;
import com.sephiria.network.SlateImprintPayload;
import com.sephiria.slate.SlateItem;
import com.sephiria.talent.Talent;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.Slot;

/** 客户端的长按 F 刻印状态（1.20.1 NBT 版）。 */
public final class SlateImprintController {
	public static final int IMPRINT_MILLIS = 1500;

	private static Slot slot;
	private static long startMillis;
	private static boolean sent;

	private SlateImprintController() {
	}

	public static void tickKey(BackpackGridMenu menu, Slot hovered, boolean held) {
		if (!held) {
			if (slot != null) {
				cancel();
			}

			sent = false;
			return;
		}

		if (sent) {
			return;
		}

		if (slot == null) {
			if (!begin(menu, hovered)) {
				return;
			}
		} else if (hovered != slot) {
			cancel();
			return;
		}

		long elapsed = System.currentTimeMillis() - startMillis;
		int progress = (int) Math.min(100L, Math.max(1L, elapsed * 100L / IMPRINT_MILLIS));

		if (slot.hasItem()) {
			ItemData.setInt(slot.getItem(), SlateItem.IMPRINT, progress);
		}

		if (progress >= 100) {
			sent = true;
			FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
			SlateImprintPayload.write(buffer, new SlateImprintPayload(slot.index));
			ClientPlayNetworking.send(SlateImprintPayload.CHANNEL, buffer);
		}
	}

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

		if (!(hovered.getItem().getItem() instanceof SlateItem)) {
			return false;
		}

		slot = hovered;
		startMillis = System.currentTimeMillis();
		ItemData.setInt(hovered.getItem(), SlateItem.IMPRINT, 1);
		return true;
	}

	public static void cancel() {
		if (slot != null && slot.hasItem()) {
			ItemData.remove(slot.getItem(), SlateItem.IMPRINT);
		}

		slot = null;
		startMillis = 0L;
	}

	public static Slot activeSlot() {
		return slot;
	}

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

	/** 在悬停格下方画 13×2 的刻印进度条。 */
	public static void drawBar(GuiGraphics graphics, int leftPos, int topPos) {
		Slot active = activeSlot();

		if (active == null) {
			return;
		}

		int x = leftPos + active.x + 2;
		int y = topPos + active.y + 13;
		graphics.fill(x, y, x + 13, y + 2, 0xFF1A1A1E);
		int width = Math.round(13 * progress());

		if (width > 0) {
			graphics.fill(x, y, x + width, y + 2, 0xFFD49ED8);
		}
	}
}
