package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.registry.ModItems;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 神器附魔面板：左边一个「待升级神器」槽 + 玩家背包，外加一个「升级」按钮（按钮由界面画，
 * 点击后发 {@link com.sephiria.network.UpgradeArtifactPayload}）。
 *
 * <p>升级规则：每点一次消耗一枚神器附魔币，把槽里神器的自身等级 +1；到最高等级就不再消耗。
 */
public class EnchantMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	public static final int SLOT_X = 26;
	public static final int SLOT_Y = 26;
	private static final int PLAYER_ROW_GAP = 13;
	/** 「升级」按钮在界面里的位置（相对面板左上角）。 */
	public static final int BUTTON_X = 70;
	public static final int BUTTON_Y = 24;
	public static final int BUTTON_WIDTH = 80;
	public static final int BUTTON_HEIGHT = 18;

	private final Container artifact;

	/** 客户端用的构造：空壳容器，内容等原版同步。 */
	public EnchantMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1));
	}

	public EnchantMenu(int containerId, Inventory playerInventory, Container artifact) {
		super(ModMenus.ARTIFACT_ENCHANT, containerId);
		this.artifact = artifact;

		// 只收神器，且不可堆叠
		addSlot(new Slot(artifact, 0, SLOT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof SephiriaArtifact;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		addStandardInventorySlots(playerInventory, 8, playerRowsY());
	}

	public static int playerRowsY() {
		return SLOT_Y + 18 + PLAYER_ROW_GAP;
	}

	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：把菜单打开给玩家。 */
	public static MenuProvider provider(ServerPlayer player) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria.enchant");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return new EnchantMenu(containerId, inventory);
			}
		};
	}

	/** 玩家身上现有的神器附魔币数量。 */
	public static int coinCount(Player player) {
		int count = 0;

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (stack.getItem() == ModItems.ENCHANT_COIN) {
				count += stack.getCount();
			}
		}

		return count;
	}

	/** 消耗一枚附魔币；没有则返回 false。 */
	public static boolean consumeCoin(Player player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (stack.getItem() == ModItems.ENCHANT_COIN) {
				stack.shrink(1);
				player.getInventory().setChanged();
				return true;
			}
		}

		return false;
	}

	/** 「升级」：消耗一枚附魔币把槽里的神器 +1 级；返回是否真的升了（服务端调用）。 */
	public boolean upgrade(Player player) {
		ItemStack stack = this.artifact.getItem(0);

		if (!(stack.getItem() instanceof SephiriaArtifact artifactData)) {
			return false;
		}

		int level = ArtifactItem.levelOf(stack);

		if (level >= artifactData.maxLevel()) {
			return false;
		}

		if (!consumeCoin(player)) {
			return false;
		}

		ArtifactItem.setLevel(stack, level + 1);
		this.artifact.setChanged();
		this.broadcastChanges();
		return true;
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
			// 神器槽 → 玩家背包
			if (!moveItemStackTo(stack, 1, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof SephiriaArtifact) {
			// 玩家背包 → 神器槽
			if (!moveItemStackTo(stack, 0, 1, false)) {
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
		return true;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		// 关界面时把槽里的神器还给玩家，免得凭空消失
		ItemStack left = this.artifact.removeItemNoUpdate(0);

		if (!left.isEmpty()) {
			player.getInventory().placeItemBackInInventory(left);
		}
	}
}
