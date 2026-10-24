package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.BackpackStorable;
import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

import java.util.Map;

/**
 * 赛菲莉娅背包的容器菜单：上半是背包的可视区（固定 {@link BackpackWindow#ROWS} 行，
 * 背包更高时滚轮翻页），下半是玩家自己的背包与快捷栏。
 *
 * <p>服务端用玩家真实的 {@link ArtifactBackpack}，客户端用等大的空壳容器——原版会把服务端的
 * 格子内容同步过来，两边不用自己写同步。数据槽（{@code ContainerData}）里放三样东西：
 * 可视格子的等级、现在滚到第几行 / 一共能滚几行、以及各连击的档位。
 * 连击必须由服务端按**整只背包**算好发下来——可视区之外的神器客户端收不到
 * （一滚轮，面板上的连击档位就会按"看得见的那几行"少算）。
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

	/** 数据槽：现在滚到第几行（0 = 顶上）。 */
	public static final int DATA_SCROLL_ROW = BackpackWindow.SLOTS;
	/** 数据槽：最多能滚到第几行（0 = 一屏放得下，不用滚）。 */
	public static final int DATA_SCROLL_MAX = BackpackWindow.SLOTS + 1;
	/** 数据槽：背包真实一共几格（末尾那些"不是格子"的位置靠它算出来）。 */
	public static final int DATA_SLOTS = BackpackWindow.SLOTS + 2;
	/** 数据槽：各连击的档位从这里开始，按 {@link ArtifactCombo} 的枚举顺序排。 */
	public static final int DATA_COMBO_START = BackpackWindow.SLOTS + 3;
	/** 数据槽总数（两边必须一致，否则原版同步会错位）。 */
	public static final int DATA_COUNT = DATA_COMBO_START + ArtifactCombo.values().length;

	private final Container backpack;
	private final BackpackWindow window;
	private final ContainerData slotLevels;
	/**
	 * 开这个菜单的玩家：26.3 的 {@code AbstractContainerMenu} 不再存玩家字段，而连击档位
	 * （服务端那份）要用到玩家身上的药水加成，所以自己留一份（菜单只活到界面关掉为止，
	 * 不是那种「长期缓存存实体」的坑）。
	 */
	private final Player owner;
	/** 现在滚到第几行；只有服务端会改，客户端从数据槽里读。 */
	private int scrollRow;

	/** 客户端用的构造：容器是空壳，内容与数据槽都等原版同步。 */
	public ArtifactBackpackMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(BackpackWindow.SLOTS), null);
	}

	/**
	 * @param backpack 服务端 = 玩家真实的背包（行数可变）；客户端 = 与可视区等大的空壳
	 * @param realBackpack 服务端的真实背包（客户端传 null）：格子等级与连击档位都按它现算
	 */
	private ArtifactBackpackMenu(int containerId, Inventory playerInventory, Container backpack,
			ArtifactBackpack realBackpack) {
		super(ModMenus.ARTIFACT_BACKPACK, containerId);
		this.backpack = backpack;
		this.owner = playerInventory.player;
		this.window = new BackpackWindow(backpack);
		this.slotLevels = realBackpack == null
				? new SimpleContainerData(DATA_COUNT)
				: liveData(realBackpack, this.window, this.owner);

		for (int index = 0; index < BackpackWindow.SLOTS; index++) {
			int col = index % ArtifactBackpack.WIDTH;
			int row = index / ArtifactBackpack.WIDTH;

			addSlot(new Slot(this.window, index, SLOT_X + col * 18, SLOT_Y + row * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					// 只有神器与石板能放；末尾那些「背后没有格子」的位置一律不收（放进去会没）
					return backpackSlotExists(getContainerSlot())
							&& stack.getItem() instanceof BackpackStorable;
				}
				// mayPickup 保持默认 true：石板基础 / 合并过的神器也要能在背包格之间拖动，
				// 「不能带出背包」由 clicked / quickMoveStack / removed 在「放到哪里」那一步拦。
			});
		}

		addStandardInventorySlots(playerInventory, COMBO_PANEL_WIDTH + 8, playerRowsY(BackpackWindow.ROWS));
		addDataSlots(this.slotLevels);
	}

	/**
	 * 服务端的只读数据槽：可视格子的等级按窗口现查，连击档位按整只背包现算。
	 *
	 * <p>原版每刻对账一次这些数值（{@code broadcastChanges}），算错的代价只是「滚轮翻页时
	 * 连击面板数字不对」，所以宁可每次都现算，也不用再加一层缓存失效逻辑。
	 *
	 * <p>{@code player} 只有服务端会传（客户端那份数据槽是空数组、等原版同步）。
	 */
	private static ContainerData liveData(ArtifactBackpack backpack, BackpackWindow window, Player player) {
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

					if (index >= DATA_COMBO_START) {
						int combo = index - DATA_COMBO_START;

						if (combo >= ArtifactCombo.values().length) {
							return 0;
						}

						// 走服务端那一份：它把药水「索莱拉的偶然」给的连击加成也算进去了
						// （这个数据槽只在服务端求值，客户端读的是同步过来的数）
						Map<ArtifactCombo, Integer> levels = player instanceof ServerPlayer serverPlayer
								? ArtifactEffects.comboLevels(serverPlayer)
								: ArtifactEffects.comboLevels(backpack, backpack::slotLevel);

						return levels.getOrDefault(ArtifactCombo.values()[combo], 0);
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

	/** 玩家背包区第一行在面板里的 y。 */
	public static int playerRowsY(int rows) {
		return SLOT_Y + rows * 18 + PLAYER_ROW_GAP;
	}

	/** 面板高度：背包区 + 玩家背包三行 + 快捷栏 + 边距。 */
	public static int imageHeight(int rows) {
		return playerRowsY(rows) + 4 * 18 + 4;
	}

	/** 数据槽里的一个数（越界给 0）。 */
	private int data(int index) {
		return index >= 0 && index < this.slotLevels.getCount() ? this.slotLevels.get(index) : 0;
	}

	/** 可视区第 index 格的格子等级（0 表示没有加成）。 */
	public int slotLevel(int index) {
		return data(index);
	}

	/** 某个连击当前的档位（服务端按整只背包算好同步过来的）。 */
	public int comboLevel(ArtifactCombo combo) {
		return data(DATA_COMBO_START + combo.ordinal());
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

	/** 背包格子的数量（可视区；玩家背包那部分不算）。 */
	public int backpackSlotCount() {
		return BackpackWindow.SLOTS;
	}

	/**
	 * 这个格子是不是赛菲莉娅背包里的（而不是玩家自己的背包）。
	 *
	 * <p>两边格子的 index 会重叠（各自都从 0 开始），所以只能按它在菜单里的位置来判断。
	 */
	@Override
	public boolean isBackpackSlot(Slot slot) {
		int index = this.slots.indexOf(slot);
		return index >= 0 && index < backpackSlotCount();
	}

	/** 这一格在真实背包里的下标（滚轮翻页 + 末尾空格判定）；不是背包格返回 -1。 */
	@Override
	public int backpackIndex(Slot slot) {
		int visible = this.slots.indexOf(slot);

		if (visible < 0 || visible >= backpackSlotCount() || !backpackSlotExists(visible)) {
			return -1;
		}

		return scrollRow() * ArtifactBackpack.WIDTH + visible;
	}

	/** 格子等级（按格子对象取，客户端也适用）。 */
	public int slotLevelOf(Slot slot) {
		return slotLevel(this.slots.indexOf(slot));
	}

	/**
	 * 可视区第 {@code visibleIndex} 格背后**真的有格子**吗。
	 *
	 * <p>格子数是按「一次 +1 格」长的，所以最后一屏的末尾常常多出几个位置——它们不是格子：
	 * 界面连底都不画、也放不进东西（{@code Slot.mayPlace} 也走这里）。
	 *
	 * <p>判定要用「现在滚到第几行」和「背包真实格数」两个数，而且**两个都必须从数据槽来**：
	 * 客户端的容器是原版同步用的空壳（恒等于可视区大小）、它自己的行号也永远是 0，拿它们算的话
	 * 末尾那些位置在客户端会被当成真格子（2026-10-17 的 bug）。
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
		return isBackpackSlot(slot) && backpackSlotExists(this.slots.indexOf(slot));
	}

	/**
	 * 滚轮翻页：{@code deltaRows > 0} 往下，返回是否真的动了。
	 *
	 * <p>视窗挪位等于这一屏的格子整体换了一批，所以改完用 {@code sendAllDataToRemote} 强制全量重推
	 * ——它把 30 个格子、数据槽（等级 / 行号 / 连击档位）和手上拿着的那份一起发给客户端，
	 * 正是开界面时用的那条路（原版的 {@code broadcastChanges} 走"只推变了的"那套，
	 * 滚动后每格的期望值都变了，能用但要绕一圈）。
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

	/** 服务端使用：接上玩家真实的背包（客户端不会调）。 */
	public static ArtifactBackpackMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		return new ArtifactBackpackMenu(containerId, playerInventory, backpack, backpack);
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

	/**
	 * 光标上的神器放进背包格之前先走一次「智慧」二阶段的合并判定。
	 *
	 * <p>只拦 PICKUP（左/右键放置）：合并成功后把被消耗的那件从光标扣掉，不能再让原版
	 * 把它放进格子里（否则会出现「格子里原有那件 + 光标上这件」的复制）。
	 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (player instanceof ServerPlayer serverPlayer
				&& slotId >= 0 && slotId < backpackSlotCount()) {
			Slot slot = this.slots.get(slotId);

			if (isBackpackSlot(slot) && backpackSlotExists(slot)) {
				// 左/右键放置与拖拽：合并光标上那件
				if ((input == ContainerInput.PICKUP || input == ContainerInput.QUICK_CRAFT)
						&& !this.getCarried().isEmpty()
						&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, this.getCarried())) {
					if (this.getCarried().isEmpty()) {
						this.setCarried(ItemStack.EMPTY);
					}

					this.broadcastChanges();
					return;
				}

				// 数字键交换：被换进来的快捷栏那件也要合并
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

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();
		int backpackSlots = backpackSlotCount();

		if (index < backpackSlots) {
			// 赛菲莉娅背包 → 玩家背包
			if (!moveItemStackTo(stack, backpackSlots, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof BackpackStorable) {
			// 玩家背包 → 赛菲莉娅背包：智慧二阶段先试合并
			if (player instanceof ServerPlayer serverPlayer
					&& com.sephiria.talent.WisdomTalent.tryMerge(serverPlayer, stack)) {
				if (stack.isEmpty()) {
					slot.setByPlayer(ItemStack.EMPTY);
				}

				return ItemStack.EMPTY;
			}

			// 只搬得动神器与石板；只能进当前这一屏
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
