package com.sephiria.beacon;

import com.sephiria.util.MenuLayout;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲莉娅信标的单格容器界面：上面一个格子，下面玩家背包。
 *
 * <p>客户端壳用空容器占位，真正的容器是服务端的方块实体；原版按槽位序号同步那一格。
 */
public class SephiriaBeaconMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	public static final int IMAGE_HEIGHT = 166;
	public static final int SLOT_X = 80;
	public static final int SLOT_Y = 35;
	public static final int PLAYER_ROWS_Y = 84;

	private final Container container;

	/** 客户端构造：只用来搭槽位，内容由原版同步。 */
	public SephiriaBeaconMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1));
	}

	public SephiriaBeaconMenu(int containerId, Inventory inventory, Container container) {
		super(ModMenus.SEPHIRIA_BEACON, containerId);
		this.container = container;
		addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
			@Override
			public int getMaxStackSize() {
				return 3;
			}

			@Override
			public int getMaxStackSize(ItemStack stack) {
				// 附魔币奖励是堆叠 3 个；其他物品仍然只能放 1 件
				return stack.is(ModItems.ENCHANT_COIN) ? 3 : 1;
			}
		});
		MenuLayout.addPlayerInventory(this::addSlot, inventory, 8, PLAYER_ROWS_Y);
	}

	/** 服务端使用：把菜单绑到方块实体那个真实容器上。 */
	public static ExtendedScreenHandlerFactory provider(ServerPlayer player, SephiriaBeaconBlockEntity beacon) {
		return new ExtendedScreenHandlerFactory() {
			@Override
			public void writeScreenOpeningData(ServerPlayer ignored, net.minecraft.network.FriendlyByteBuf buf) {
			}

			@Override
			public Component getDisplayName() {
				return SephiriaBeaconBlock.title();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return new SephiriaBeaconMenu(containerId, inventory, beacon);
			}
		};
	}

	/** 容器里那件物品（界面画光柱颜色提示时可用）。 */
	public ItemStack heldItem() {
		return this.container.getItem(0);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();

		if (index == 0) {
			// 信标格 → 玩家背包
			if (!moveItemStackTo(stack, 1, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, 1, false)) {
			// 玩家背包 → 信标格（只放 1 件）
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return moved;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.container.stillValid(player);
	}
}
