package com.sephiria.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

/**
 * 1.21.1 没有 26.3 的 {@code addStandardInventorySlots}，这里补一份同布局：
 * 3 行主背包 + 1 行快捷栏，坐标与 26.3 的调用点一致。
 */
public final class MenuLayout {
	private MenuLayout() {
	}

	public static void addPlayerInventory(Consumer<Slot> addSlot, Inventory inventory, int x, int y) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot.accept(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
			}
		}

		for (int column = 0; column < 9; column++) {
			addSlot.accept(new Slot(inventory, column, x + column * 18, y + 58));
		}
	}
}
