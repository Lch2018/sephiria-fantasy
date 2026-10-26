package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactItem;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 神器附魔面板：左边是功能区（神器槽、附魔币槽、三个升级选项），右边是赛菲莉娅背包的
 * 6 × 高度格区和玩家自己的背包——赛菲莉娅背包里的神器不用先搬回原版背包，直接拖进
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
	/** 背包格区的位置（右列里居中；格区是固定 {@link BackpackWindow#ROWS} 行的可视窗口）。 */
	public static final int BACKPACK_SLOT_X = FUNCTION_AREA_WIDTH + (176 - ArtifactBackpack.WIDTH * 18) / 2;
	public static final int BACKPACK_SLOT_Y = 18;
	/** 数据槽：可视格子的等级占前 {@link BackpackWindow#SLOTS} 个，后面两个是行号。 */
	public static final int DATA_SCROLL_ROW = BackpackWindow.SLOTS;
	/** 最多能滚到第几行（0 = 一屏放得下，不用滚）。 */
	public static final int DATA_SCROLL_MAX = BackpackWindow.SLOTS + 1;
	/** 数据槽：背包真实一共几格（末尾那些"不是格子"的位置靠它算出来）。 */
	public static final int DATA_SLOTS = BackpackWindow.SLOTS + 2;
	/** 数据槽总数（两边必须一致，否则原版同步会错位）。 */
	public static final int DATA_COUNT = BackpackWindow.SLOTS + 3;
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
	private final BackpackWindow window;
	private final ContainerData slotLevels;
	/** 现在滚到第几行；只有服务端会改，客户端从数据槽里读。 */
	private int scrollRow;

	/** 客户端用的构造：空壳容器，内容与数据槽都等原版同步。 */
	public EnchantMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(1), new SimpleContainer(1),
				new SimpleContainer(BackpackWindow.SLOTS), null);
	}

	/**
	 * @param backpack 服务端 = 玩家真实的背包（行数可变）；客户端 = 与可视区等大的空壳
	 * @param realBackpack 服务端的真实背包（客户端传 null）：格子等级按它现算
	 */
	private EnchantMenu(int containerId, Inventory playerInventory, Container artifact, Container coins,
			Container backpack, ArtifactBackpack realBackpack) {
		super(ModMenus.ARTIFACT_ENCHANT, containerId);
		this.artifact = artifact;
		this.coins = coins;
		this.window = new BackpackWindow(backpack);
		this.slotLevels = realBackpack == null
				? new SimpleContainerData(DATA_COUNT)
				: liveData(realBackpack, this.window);

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

		// 赛菲莉娅背包格区：神器与石板都能放，跟背包界面同一套规则。格子建在「可视窗口」上
		// （固定一屏），背包更长时滚轮翻页——见 BackpackWindow
		for (int index = 0; index < BackpackWindow.SLOTS; index++) {
			int col = index % ArtifactBackpack.WIDTH;
			int row = index / ArtifactBackpack.WIDTH;

			addSlot(new Slot(this.window, index, BACKPACK_SLOT_X + col * 18, BACKPACK_SLOT_Y + row * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					// 末尾那些「背后没有格子」的位置不收（放进去会没），见 backpackSlotExists
					return backpackSlotExists(getContainerSlot())
							&& stack.getItem() instanceof BackpackStorable;
				}
				// mayPickup 保持默认 true：受保护物品可以在背包格之间拖，也能拖进神器槽；
				// 「不能进普通背包 / 丢出」由 clicked / quickMoveStack / removed 拦。
			});
		}

		addStandardInventorySlots(playerInventory, FUNCTION_AREA_WIDTH + 8, playerRowsY());
		addDataSlots(this.slotLevels);
	}

	/**
	 * 服务端的只读数据槽：可视格子的等级按窗口现查，再加两个行号（现在第几行 / 最多第几行）。
	 */
	private static ContainerData liveData(ArtifactBackpack backpack, BackpackWindow window) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				if (index == DATA_SCROLL_ROW) {
					return window.row();
				}

				if (index == DATA_SCROLL_MAX) {
					return window.maxRow();
				}

				if (index == DATA_SLOTS) {
					// 背包真实格数：客户端要靠它把末尾那些不是格子的位置藏起来
					return backpack.getContainerSize();
				}

				int real = window.sourceIndex(index);
				return real < 0 ? 0 : backpack.slotLevel(real);
			}

			@Override
			public void set(int index, int value) {
				// 只读：客户端不需要回写
			}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		};
	}

	/** 玩家背包区第一行在面板里的 y（背包格区固定一屏，所以面板高度与背包真实行数无关）。 */
	public static int playerRowsY() {
		return BACKPACK_SLOT_Y + BackpackWindow.ROWS * 18 + PLAYER_ROW_GAP;
	}

	/** 面板高度：背包格区 + 玩家背包三行 + 快捷栏 + 边距。 */
	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：背包格区接玩家真实的背包（客户端不会走到这里）。 */
	public static EnchantMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		return new EnchantMenu(containerId, playerInventory,
				new SimpleContainer(1), new SimpleContainer(1), backpack, backpack);
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

	/** 背包格区的格子数（可视区；神器槽、附魔币槽与玩家背包那部分不算）。 */
	public int backpackSlotCount() {
		return BackpackWindow.SLOTS;
	}

	/**
	 * 这个格子是不是赛菲莉娅背包里的。
	 *
	 * <p>背包格子在菜单里从第 2 个开始（前两个是神器槽与附魔币槽），两边的 index 会和
	 * 其它区域重叠，只能按它在菜单里的位置判断。
	 */
	public boolean isBackpackSlot(Slot slot) {
		return isBackpackSlotIndex(this.slots.indexOf(slot));
	}

	/** 这一格在真实背包里的下标（滚轮翻页 + 末尾空格判定）；不是背包格返回 -1。 */
	@Override
	public int backpackIndex(Slot slot) {
		int index = this.slots.indexOf(slot);

		if (!isBackpackSlotIndex(index)) {
			return -1;
		}

		int visible = index - 2;

		if (!backpackSlotExists(visible)) {
			return -1;
		}

		return scrollRow() * ArtifactBackpack.WIDTH + visible;
	}

	/**
	 * 光标上的神器放进背包格之前先走一次「智慧」二阶段的合并判定。
	 *
	 * <p>与 {@link ArtifactBackpackMenu#clicked} 同一套：合并成功后把被消耗的那件从光标扣掉。
	 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (player instanceof ServerPlayer serverPlayer
				&& slotId >= 2 && slotId < 2 + backpackSlotCount()) {
			Slot slot = this.slots.get(slotId);

			if (isBackpackSlot(slot) && backpackSlotExists(slot)) {
				if ((input == ContainerInput.PICKUP || input == ContainerInput.QUICK_CRAFT)
						&& !this.getCarried().isEmpty()
						&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, this.getCarried())) {
					if (this.getCarried().isEmpty()) {
						this.setCarried(ItemStack.EMPTY);
					}

					this.broadcastChanges();
					return;
				}

				if (input == ContainerInput.SWAP && button >= 0 && button < 9) {
					ItemStack hotbar = serverPlayer.getInventory().getItem(button);

					if (!hotbar.isEmpty()
							&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, hotbar)) {
						serverPlayer.getInventory().setChanged();
						this.broadcastChanges();
						return;
					}
				}
			}
		}

		super.clicked(slotId, button, input, player);
	}

	private boolean isBackpackSlotIndex(int index) {
		return index >= 2 && index < 2 + backpackSlotCount();
	}

	/**
	 * 可视区第 {@code visibleIndex} 格背后**真的有格子**吗（末尾那些不是格子的位置不该出现）。
	 *
	 * <p>与 {@link ArtifactBackpackMenu#backpackSlotExists(int)} 同一套：行号与格数都从数据槽读，
	 * 客户端那份空壳容器不能拿来算。
	 */
	public boolean backpackSlotExists(int visibleIndex) {
		if (visibleIndex < 0 || visibleIndex >= BackpackWindow.SLOTS) {
			return false;
		}

		int real = scrollRow() * ArtifactBackpack.WIDTH + visibleIndex;
		return real >= 0 && real < backpackCapacity();
	}

	/** 便利入口：按菜单里的格子对象问（不在背包格区里一律 false）。 */
	public boolean backpackSlotExists(Slot slot) {
		int index = this.slots.indexOf(slot);
		return isBackpackSlotIndex(index) && backpackSlotExists(index - 2);
	}

	/** 格子等级（按格子对象取，客户端也适用；非背包格子返回 0）。 */
	public int slotLevelOf(Slot slot) {
		int index = this.slots.indexOf(slot);
		return isBackpackSlotIndex(index) ? slotLevel(index - 2) : 0;
	}

	/** 数据槽里的一个数（越界给 0）。 */
	private int data(int index) {
		return index >= 0 && index < this.slotLevels.getCount() ? this.slotLevels.get(index) : 0;
	}

	/** 可视区第 index 格的格子等级（0 表示没有加成）。 */
	public int slotLevel(int index) {
		return data(index);
	}

	/** 现在滚到第几行（客户端读同步值）。 */
	public int scrollRow() {
		return data(DATA_SCROLL_ROW);
	}

	/** 最多能滚到第几行（0 = 一屏放得下，不用滚）。 */
	public int maxScrollRow() {
		return data(DATA_SCROLL_MAX);
	}

	/** 背包真实一共几格（服务端现查背包、客户端读同步值）。 */
	public int backpackCapacity() {
		return data(DATA_SLOTS);
	}

	/**
	 * 滚轮翻页：{@code deltaRows > 0} 往下，返回是否真的动了。
	 *
	 * <p>与 {@link ArtifactBackpackMenu#scrollBackpack} 同一套：改完偏移用
	 * {@code sendAllDataToRemote} 强制全量重推这一屏的格子与数据槽。
	 */
	@Override
	public boolean scrollBackpack(int deltaRows) {
		int next = Mth.clamp(this.scrollRow + deltaRows, 0, this.window.maxRow());

		if (next == this.scrollRow) {
			return false;
		}

		this.scrollRow = next;
		this.window.setRow(next);
		this.sendAllDataToRemote();
		return true;
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
			// 神器槽 → 背包前先试合并（否则会绕过智慧二阶段的合并逻辑，在背包里造出重复）
			if (player instanceof ServerPlayer serverPlayer
					&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, stack)) {
				if (stack.isEmpty()) {
					slot.setByPlayer(ItemStack.EMPTY);
				}

				return ItemStack.EMPTY;
			}

			// 神器槽 → 先回赛菲莉娅背包（它平时的家），放不进去再去玩家背包
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
			// 玩家背包里的神器 → 先送进神器槽；槽占了就试合并，最后才放进背包格区
			if (!moveItemStackTo(stack, 0, 1, false)) {
				if (player instanceof ServerPlayer serverPlayer
						&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, stack)) {
					if (stack.isEmpty()) {
						slot.setByPlayer(ItemStack.EMPTY);
					}

					return ItemStack.EMPTY;
				}

				if (!moveItemStackTo(stack, 2, playerStart, false)) {
					return ItemStack.EMPTY;
				}
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

		ItemStack artifactLeft = this.artifact.removeItemNoUpdate(0);
		ItemStack coinsLeft = this.coins.removeItemNoUpdate(0);

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		if (!artifactLeft.isEmpty()) {
			serverPlayer.getInventory().placeItemBackInInventory(artifactLeft, Prediction.SERVER_ONLY);
		}

		if (!coinsLeft.isEmpty()) {
			serverPlayer.getInventory().placeItemBackInInventory(coinsLeft, Prediction.SERVER_ONLY);
		}
	}
}
