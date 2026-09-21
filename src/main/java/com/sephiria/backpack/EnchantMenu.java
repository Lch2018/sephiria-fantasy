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
 * 神器附魔面板：和原版附魔台一个思路——上面放要升级的神器，下面放神器附魔币，
 * 右边是三个升级选项（1 / 2 / 5 级）。
 *
 * <p>升级规则：从附魔币栏扣币，把神器自身等级往上加；<b>不会溢出</b>——比如神器只差 2 级满，
 * 点「5 级」也只扣 2 枚。附魔币不够（少于这次实际需要的数量）时选项在客户端置灰，
 * 服务端还会再判一次，客户端的置灰只是显示。
 */
public class EnchantMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	/** 神器槽与附魔币槽的位置。 */
	public static final int SLOT_X = 26;
	public static final int ARTIFACT_SLOT_Y = 22;
	public static final int COIN_SLOT_Y = 48;
	private static final int PLAYER_ROW_GAP = 13;
	/** 三个升级选项的位置（相对面板左上角）。 */
	public static final int BUTTON_X = 74;
	public static final int BUTTON_Y = 22;
	public static final int BUTTON_WIDTH = 78;
	public static final int BUTTON_HEIGHT = 18;
	public static final int BUTTON_SPACING = 22;
	/** 可选的升级级数。 */
	public static final int[] UPGRADE_STEPS = { 1, 2, 5 };

	private final Container artifact;
	private final Container coins;

	/** 客户端用的构造：空壳容器，内容等原版同步。 */
	public EnchantMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainer(1));
	}

	public EnchantMenu(int containerId, Inventory playerInventory, Container artifact, Container coins) {
		super(ModMenus.ARTIFACT_ENCHANT, containerId);
		this.artifact = artifact;
		this.coins = coins;

		// 只收神器，且不可堆叠
		addSlot(new Slot(artifact, 0, SLOT_X, ARTIFACT_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() instanceof SephiriaArtifact;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		// 附魔币栏：只收附魔币，可以叠
		addSlot(new Slot(coins, 0, SLOT_X, COIN_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() == ModItems.ENCHANT_COIN;
			}
		});

		addStandardInventorySlots(playerInventory, 8, playerRowsY());
	}

	public static int playerRowsY() {
		return COIN_SLOT_Y + 18 + PLAYER_ROW_GAP;
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

	/** 神器槽里的东西。 */
	public ItemStack artifactStack() {
		return this.artifact.getItem(0);
	}

	/** 附魔币栏里的枚数。 */
	public int coinCount() {
		ItemStack stack = this.coins.getItem(0);
		return stack.getItem() == ModItems.ENCHANT_COIN ? stack.getCount() : 0;
	}

	/** 这次点「升级 N 级」实际能升几级（受剩余等级与附魔币数量限制，不溢出）。 */
	public int affordableLevels(int wanted) {
		ItemStack stack = this.artifactStack();

		if (!(stack.getItem() instanceof SephiriaArtifact artifactData)) {
			return 0;
		}

		int missing = artifactData.maxLevel() - ArtifactItem.levelOf(stack);
		return Math.max(0, Math.min(Math.min(wanted, missing), this.coinCount()));
	}

	/**
	 * 升级：从附魔币栏扣掉实际需要的数量，把神器升上去。
	 *
	 * <p>只带「想升几级」，实际升多少由服务端按剩余等级与附魔币重新算，客户端的数字不算数。
	 */
	public boolean upgrade(int wanted) {
		int levels = affordableLevels(wanted);

		if (levels <= 0) {
			return false;
		}

		ItemStack artifactStack = this.artifactStack();
		int level = ArtifactItem.levelOf(artifactStack);

		this.coins.getItem(0).shrink(levels);
		this.coins.setChanged();
		ArtifactItem.setLevel(artifactStack, level + levels);
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

		if (index <= 1) {
			// 两个功能槽 → 玩家背包
			if (!moveItemStackTo(stack, 2, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof SephiriaArtifact) {
			// 神器 → 神器槽
			if (!moveItemStackTo(stack, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() == ModItems.ENCHANT_COIN) {
			// 附魔币 → 附魔币栏
			if (!moveItemStackTo(stack, 1, 2, false)) {
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

		// 关界面时把两个槽里的东西还给玩家，免得凭空消失
		for (Container container : new Container[] { this.artifact, this.coins }) {
			ItemStack left = container.removeItemNoUpdate(0);

			if (!left.isEmpty()) {
				player.getInventory().placeItemBackInInventory(left);
			}
		}
	}
}
