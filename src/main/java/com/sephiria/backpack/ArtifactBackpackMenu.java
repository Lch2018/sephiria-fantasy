package com.sephiria.backpack;

import com.sephiria.artifact.BackpackStorable;
import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赛菲利亚背包的容器菜单：上半是背包的 6 × 高度格，下半是玩家自己的背包与快捷栏。
 *
 * <p>服务端用玩家真实的 {@link ArtifactBackpack}，客户端用等大的空壳容器——原版会把服务端的
 * 格子内容同步过来，两边不用自己写同步。格子等级走数据槽（{@code ContainerData}），
 * 只用于在格子里显示等级数字。
 */
public class ArtifactBackpackMenu extends AbstractContainerMenu implements BackpackGridMenu {
	/** 九格宽的玩家背包决定面板宽度；背包区（6 格）在面板里居中。 */
	/** 左侧连击面板的宽度：面板本身 176 宽，左边再加一条连击栏。 */
	public static final int COMBO_PANEL_WIDTH = 96;
	public static final int CONTAINER_WIDTH = 176;
	public static final int IMAGE_WIDTH = COMBO_PANEL_WIDTH + CONTAINER_WIDTH;
	public static final int SLOT_X = COMBO_PANEL_WIDTH + (CONTAINER_WIDTH - ArtifactBackpack.WIDTH * 18) / 2;
	public static final int SLOT_Y = 18;
	/** 背包区与玩家背包区之间的空档。 */
	private static final int PLAYER_ROW_GAP = 13;

	private final Container backpack;
	private final ContainerData slotLevels;

	/** 客户端用的构造：容器是空壳，内容等原版同步。 */
	public ArtifactBackpackMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory,
				new SimpleContainer(ArtifactBackpack.WIDTH * ArtifactBackpack.DEFAULT_HEIGHT),
				new SimpleContainerData(ArtifactBackpack.WIDTH * ArtifactBackpack.DEFAULT_HEIGHT));
	}

	public ArtifactBackpackMenu(int containerId, Inventory playerInventory, Container backpack, ContainerData slotLevels) {
		super(ModMenus.ARTIFACT_BACKPACK, containerId);
		this.backpack = backpack;
		this.slotLevels = slotLevels;

		int rows = backpack.getContainerSize() / ArtifactBackpack.WIDTH;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < ArtifactBackpack.WIDTH; col++) {
				int index = col + row * ArtifactBackpack.WIDTH;

				addSlot(new Slot(backpack, index, SLOT_X + col * 18, SLOT_Y + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return stack.getItem() instanceof BackpackStorable;
					}
				});
			}
		}

		addStandardInventorySlots(playerInventory, COMBO_PANEL_WIDTH + 8, playerRowsY(rows));
		addDataSlots(slotLevels);
	}

	/** 玩家背包区第一行在面板里的 y。 */
	public static int playerRowsY(int rows) {
		return SLOT_Y + rows * 18 + PLAYER_ROW_GAP;
	}

	/** 面板高度：背包区 + 玩家背包三行 + 快捷栏 + 边距。 */
	public static int imageHeight(int rows) {
		return playerRowsY(rows) + 4 * 18 + 4;
	}

	/** 第 index 个背包格子的格子等级（0 表示没有加成）。 */
	public int slotLevel(int index) {
		int count = this.slotLevels.getCount();

		if (count <= 0) {
			return 0;
		}

		return this.slotLevels.get(Math.max(0, Math.min(count - 1, index)));
	}

	/** 容器部分（供界面画格子底与面板）。 */
	public Container container() {
		return this.backpack;
	}

	/** 背包格子的数量（玩家背包那部分不算）。 */
	public int backpackSlotCount() {
		return this.backpack.getContainerSize();
	}

	/**
	 * 这个格子是不是赛菲利亚背包里的（而不是玩家自己的背包）。
	 *
	 * <p>两边格子的 index 会重叠（各自都从 0 开始），所以只能按它在菜单里的位置来判断。
	 */
	public boolean isBackpackSlot(Slot slot) {
		int index = this.slots.indexOf(slot);
		return index >= 0 && index < backpackSlotCount();
	}

	/** 格子等级（按格子对象取，客户端也适用）。 */
	public int slotLevelOf(Slot slot) {
		return slotLevel(this.slots.indexOf(slot));
	}

	/** 服务端使用：接上玩家真实的背包（客户端不会调）。 */
	public static ArtifactBackpackMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		return new ArtifactBackpackMenu(containerId, playerInventory, backpack, backpack.levelData());
	}

	/**
	 * 打开界面用的 {@link MenuProvider}。
	 *
	 * <p>必须是 Fabric 的 {@link ExtendedMenuProvider}：菜单类型是 {@link
	 * net.fabricmc.fabric.api.menu.v1.ExtendedMenuType}，而它的普通 {@code create(id, inventory)}
	 * 是故意抛异常的（必须走带开屏数据的那条路）。这里没有额外数据，用 {@code Unit} 占位。
	 */
	public static MenuProvider provider(ServerPlayer player) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria_fantasy.backpack");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return forPlayer(containerId, inventory, player);
			}
		};
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();
		int backpackSlots = this.backpack.getContainerSize();

		if (index < backpackSlots) {
			// 赛菲利亚背包 → 玩家背包
			if (!moveItemStackTo(stack, backpackSlots, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof BackpackStorable) {
			// 玩家背包 → 赛菲利亚背包（只搬得动神器与石板）
			if (!moveItemStackTo(stack, 0, backpackSlots, false)) {
				return ItemStack.EMPTY;
			}
		} else {
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
		return this.backpack.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.backpack.stopOpen(player);
	}
}
