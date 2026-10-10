package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
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
 * 神器附魔面板：左边是功能区（神器槽、附魔币槽、三个升级选项），右边是赛菲利亚背包的
 * 6 × 高度格区和玩家自己的背包——赛菲利亚背包里的神器不用先搬回原版背包，直接拖进
 * 神器槽就能升级。
 *
 * <p>升级规则：从附魔币栏扣币，把神器自身等级往上加；<b>不会溢出</b>——比如神器只差 2 级满，
 * 点「5 级」也只扣 2 枚。附魔币不够（少于这次实际需要的数量）时选项在客户端置灰，
 * 服务端还会再判一次，客户端的置灰只是显示。
 *
 * <p>服务端这边背包格区接的是玩家真实的 {@link ArtifactBackpack}（往里放的东西由它自己
 * 写回附件存档）；神器槽与附魔币槽是菜单私有的空壳，内容靠原版同步，关界面时再还回去。
 * 格子等级走数据槽（{@link ContainerData}），只用来把数字画在格子里。
 */
public class EnchantMenu extends AbstractContainerMenu implements BackpackGridMenu {
	/** 左侧功能区的宽度：神器槽、附魔币槽和升级选项都在这 96 像素里。 */
	public static final int FUNCTION_AREA_WIDTH = 96;
	/** 右列沿用原版背包的 176 像素版面（背包格区在其内居中），总宽 = 左 + 右。 */
	public static final int IMAGE_WIDTH = FUNCTION_AREA_WIDTH + 176;
	/** 神器槽与附魔币槽在功能区里横向居中（按 18px 格底算）。 */
	public static final int SLOT_X = (FUNCTION_AREA_WIDTH - 18) / 2;
	public static final int ARTIFACT_SLOT_Y = 22;
	public static final int COIN_SLOT_Y = 60;
	/** 背包格区的位置（右列里居中，行数见 {@link #BACKPACK_ROWS}）。 */
	public static final int BACKPACK_SLOT_X = FUNCTION_AREA_WIDTH + (176 - ArtifactBackpack.WIDTH * 18) / 2;
	public static final int BACKPACK_SLOT_Y = 18;
	/**
	 * 背包格区的行数：客户端构造时不知道真实高度，先按默认行数开（原版会把格子内容同步过来，
	 * 行数两边一致就行）。做扩容之后，这里要和客户端壳容器的大小一起改成跟真实高度对齐。
	 */
	public static final int BACKPACK_ROWS = ArtifactBackpack.DEFAULT_HEIGHT;
	/** 背包格区与玩家背包区之间的空档。 */
	private static final int PLAYER_ROW_GAP = 13;
	/** 三个升级选项放在功能区底部。 */
	public static final int BUTTON_X = 8;
	public static final int BUTTON_Y = 96;
	public static final int BUTTON_WIDTH = 78;
	public static final int BUTTON_HEIGHT = 18;
	public static final int BUTTON_SPACING = 22;
	/** 可选的升级级数。 */
	public static final int[] UPGRADE_STEPS = { 1, 2, 5 };

	private final Container artifact;
	private final Container coins;
	private final Container backpack;
	private final ContainerData slotLevels;

	/** 客户端用的构造：空壳容器，内容等原版同步。 */
	public EnchantMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainer(1),
				new SimpleContainer(ArtifactBackpack.WIDTH * BACKPACK_ROWS),
				new SimpleContainerData(ArtifactBackpack.WIDTH * BACKPACK_ROWS));
	}

	public EnchantMenu(int containerId, Inventory playerInventory, Container artifact, Container coins,
			Container backpack, ContainerData slotLevels) {
		super(ModMenus.ARTIFACT_ENCHANT, containerId);
		this.artifact = artifact;
		this.coins = coins;
		this.backpack = backpack;
		this.slotLevels = slotLevels;

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

		// 赛菲利亚背包格区：神器与石板都能放，跟背包界面同一套规则
		int rows = backpack.getContainerSize() / ArtifactBackpack.WIDTH;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < ArtifactBackpack.WIDTH; col++) {
				addSlot(new Slot(backpack, col + row * ArtifactBackpack.WIDTH,
						BACKPACK_SLOT_X + col * 18, BACKPACK_SLOT_Y + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return stack.getItem() instanceof BackpackStorable;
					}
				});
			}
		}

		addStandardInventorySlots(playerInventory, FUNCTION_AREA_WIDTH + 8, playerRowsY());
		addDataSlots(slotLevels);
	}

	/** 玩家背包区第一行在面板里的 y（按默认行数算，见 {@link #BACKPACK_ROWS} 的说明）。 */
	public static int playerRowsY() {
		return BACKPACK_SLOT_Y + BACKPACK_ROWS * 18 + PLAYER_ROW_GAP;
	}

	/** 面板高度：背包格区 + 玩家背包三行 + 快捷栏 + 边距。 */
	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：背包格区接玩家真实的背包（客户端不会走到这里）。 */
	public static EnchantMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		return new EnchantMenu(containerId, playerInventory,
				new SimpleContainer(1), new SimpleContainer(1), backpack, backpack.levelData());
	}

	/** 把菜单打开给玩家（26.3 的菜单必须走 screenOpeningData 这条路，数据用 Unit 占位）。 */
	public static MenuProvider provider(ServerPlayer player) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria_fantasy.enchant");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return forPlayer(containerId, inventory, player);
			}
		};
	}

	/** 背包格区的格子数（神器槽、附魔币槽与玩家背包那部分不算）。 */
	public int backpackSlotCount() {
		return this.backpack.getContainerSize();
	}

	/**
	 * 这个格子是不是赛菲利亚背包里的。
	 *
	 * <p>背包格子在菜单里从第 2 个开始（前两个是神器槽与附魔币槽），两边的 index 会和
	 * 其它区域重叠，只能按它在菜单里的位置判断。
	 */
	public boolean isBackpackSlot(Slot slot) {
		return isBackpackSlotIndex(this.slots.indexOf(slot));
	}

	private boolean isBackpackSlotIndex(int index) {
		return index >= 2 && index < 2 + backpackSlotCount();
	}

	/** 格子等级（按格子对象取，客户端也适用；非背包格子返回 0）。 */
	public int slotLevelOf(Slot slot) {
		int index = this.slots.indexOf(slot);
		return isBackpackSlotIndex(index) ? slotLevel(index - 2) : 0;
	}

	/** 第 index 个背包格子的格子等级（0 表示没有加成）。 */
	public int slotLevel(int index) {
		int count = this.slotLevels.getCount();
		return count <= 0 ? 0 : this.slotLevels.get(Math.max(0, Math.min(count - 1, index)));
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

	/**
	 * Shift 点击的走向：往「能对这个东西做事的地方」送，不行就送回它的家。
	 * 神器 → 神器槽（升级），石板 → 背包格区，附魔币 → 附魔币栏。
	 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();
		int backpackSlots = this.backpackSlotCount();
		// 玩家背包区在菜单里的起始下标（前两个是神器槽与附魔币槽，然后是背包格区）
		int playerStart = 2 + backpackSlots;

		if (index == 0) {
			// 神器槽 → 先回赛菲利亚背包（它平时的家），放不进去再去玩家背包
			if (!moveItemStackTo(stack, 2, playerStart, false)
					&& !moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (index == 1) {
			// 附魔币槽 → 玩家背包
			if (!moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (index < playerStart) {
			if (stack.getItem() instanceof SephiriaArtifact) {
				// 背包里的神器 → 神器槽（这个界面的本意），占着就先回玩家背包
				if (!moveItemStackTo(stack, 0, 1, false)
						&& !moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				// 背包里的石板 → 玩家背包
				if (!moveItemStackTo(stack, playerStart, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			}
		} else if (stack.getItem() == ModItems.ENCHANT_COIN) {
			// 玩家背包里的附魔币 → 附魔币栏
			if (!moveItemStackTo(stack, 1, 2, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof SephiriaArtifact) {
			// 玩家背包里的神器 → 先送进神器槽，占着就放进背包格区
			if (!moveItemStackTo(stack, 0, 1, false) && !moveItemStackTo(stack, 2, playerStart, false)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof BackpackStorable) {
			// 玩家背包里的石板 → 背包格区
			if (!moveItemStackTo(stack, 2, playerStart, false)) {
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

		// 关界面时把两个功能槽里的东西还给玩家，免得凭空消失；背包格区里的东西本来就是
		// 玩家的（attachment 会自己存），留在原地。
		// 26.3 的 placeItemBackInInventory 要带 Prediction；照原版铁砧关界面的写法
		// 只在服务端还东西，客户端等服务器同步
		for (Container container : new Container[] { this.artifact, this.coins }) {
			ItemStack left = container.removeItemNoUpdate(0);

			if (!left.isEmpty() && player instanceof ServerPlayer) {
				player.getInventory().placeItemBackInInventory(left, Prediction.SERVER_ONLY);
			}
		}
	}
}
