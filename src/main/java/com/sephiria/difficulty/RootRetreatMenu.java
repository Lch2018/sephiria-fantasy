package com.sephiria.difficulty;

import com.sephiria.registry.ModMenus;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * 根之退界面：<b>不显示物品栏</b>（用户口径「这是一个新的界面」），所以这个菜单一个格子都没有。
 *
 * <p>界面内容：
 * <ul>
 *   <li><b>左列</b>：当前方块的方案——每项一行（名字 + 「3 / 8」+ 加减号）；</li>
 *   <li><b>右上</b>：这个方块方案的「第 x 阶段」与它带来的祝福「经验加成 x%」（每层阶段 +10%）；</li>
 *   <li><b>右下</b>：你自己的方案（正在生效的那份）——第 1 行「第 y 阶段」，之后每项一行「名字 z层」；</li>
 *   <li><b>底部</b>：「清空」（把方块这份清成 0）与「应用」（把方块这份拷到你自己身上）。</li>
 * </ul>
 *
 * <p>数据同步走 {@link ContainerData}（与附魔面板的格子等级同一套）：服务端那份是<b>现算的只读视图</b>
 * （方块那份 + 玩家那份都读实时值），客户端那份是原版同步过来的壳——显示永远以服务端为准，
 * 客户端的加减号 / 清空 / 应用只是请求（这里还会再夹一次范围）。
 */
public class RootRetreatMenu extends AbstractContainerMenu {
	/** 面板宽度：左列（选项）+ 右列（两块信息）。 */
	public static final int IMAGE_WIDTH = 268;
	/** 面板内边距。 */
	public static final int PAD = 8;
	/** 左列选项列表：行高、一次最多画几行、上沿。 */
	public static final int ROW_HEIGHT = 22;
	public static final int VISIBLE_ROWS = 6;
	public static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
	public static final int LIST_TOP = 22;
	/** 左列的行内控件：名字靠左、等级右对齐到 {@link #ROW_LEVEL_RIGHT}，两个按钮排在右边。 */
	public static final int ROW_TEXT_X = PAD;
	public static final int ROW_LEVEL_RIGHT = 116;
	public static final int MINUS_BUTTON_X = 120;
	public static final int PLUS_BUTTON_X = 142;
	public static final int BUTTON_SIZE = 18;
	/** 行内按钮相对行顶的 y 偏移（行高 22、按钮 18 → 上下各留 2）。 */
	public static final int BUTTON_Y_OFFSET = 2;
	/** 右列的左沿与宽度。 */
	public static final int RIGHT_X = 164;
	public static final int RIGHT_WIDTH = 96;
	/** 右上：这个方块的阶段与祝福；右下：你自己的方案。 */
	public static final int BLOCK_STAGE_Y = 26;
	public static final int BLESSING_Y = 40;
	public static final int YOUR_PLAN_TITLE_Y = 62;
	public static final int YOUR_STAGE_Y = 76;
	public static final int YOUR_ROWS_TOP = 90;
	/** 右下的「名字 z层」行高。 */
	public static final int YOUR_ROW_HEIGHT = 11;
	/** 底部两个按钮。 */
	public static final int BUTTON_Y = LIST_TOP + LIST_HEIGHT + 8;
	public static final int WIDE_BUTTON_WIDTH = 72;
	public static final int WIDE_BUTTON_HEIGHT = 18;
	public static final int CLEAR_BUTTON_X = 44;
	public static final int APPLY_BUTTON_X = 152;

	/** 数据槽：0 = 方块那份的阶段，接着是方块那份的各级等级；之后同样一份是玩家自己的。 */
	public static final int DATA_BLOCK_STAGE = 0;
	public static final int DATA_BLOCK_LEVELS = 1;

	/** 服务端用的玩家与方块（客户端壳里都是 null：那边只读同步过来的数据）。 */
	private final ServerPlayer owner;
	private final RootRetreatBlockEntity block;
	private final ContainerData data;

	/** 客户端用的构造：空壳数据，内容等原版同步。 */
	public RootRetreatMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null, null, new SimpleContainerData(dataSize()));
	}

	public RootRetreatMenu(int containerId, Inventory playerInventory, ServerPlayer owner,
			RootRetreatBlockEntity block, ContainerData data) {
		super(ModMenus.ROOT_RETREAT, containerId);
		this.owner = owner;
		this.block = block;
		this.data = data;
		// 一个格子都不加：这个界面不碰物品（用户口径「不会显示物品栏和背包」）
		addDataSlots(data);
	}

	/** 玩家那份数据（= 正在生效的方案）在数据槽里的起始下标。 */
	public static int playerStageIndex() {
		return DATA_BLOCK_LEVELS + DifficultyOption.values().length;
	}

	public static int playerLevelsIndex() {
		return playerStageIndex() + 1;
	}

	public static int dataSize() {
		return 2 + 2 * DifficultyOption.values().length;
	}

	/** 面板高度：选项列表 + 底部按钮 + 边距。 */
	public static int imageHeight() {
		return BUTTON_Y + WIDE_BUTTON_HEIGHT + PAD;
	}

	/** 服务端使用：数据槽接方块的方案与玩家的方案（客户端不会走到这里）。 */
	public static RootRetreatMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player,
			RootRetreatBlockEntity block) {
		return new RootRetreatMenu(containerId, playerInventory, player, block, liveData(player, block));
	}

	/** 把菜单打开给玩家（26.3 的菜单必须走 screenOpeningData 这条路，数据用 Unit 占位）。 */
	public static MenuProvider provider(ServerPlayer player, RootRetreatBlockEntity block) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return RootRetreatBlock.title();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return forPlayer(containerId, inventory, player, block);
			}
		};
	}

	/**
	 * 服务端的数据槽：<b>每次读都现算</b>（方块那份读方块实体、玩家那份读附件），
	 * 所以改完等级之后原版下一 tick 就会把新值同步给客户端。
	 */
	private static ContainerData liveData(ServerPlayer player, RootRetreatBlockEntity block) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				DifficultyOption[] options = DifficultyOption.values();
				int levelIndex = index - DATA_BLOCK_LEVELS;
				int playerStage = playerStageIndex();
				int playerLevelIndex = index - playerLevelsIndex();

				if (index == DATA_BLOCK_STAGE) {
					return block == null ? 0 : block.stage();
				}

				if (index == playerStage) {
					return RootRetreat.stageOf(player);
				}

				if (levelIndex >= 0 && levelIndex < options.length) {
					return block == null ? 0 : block.levelOf(options[levelIndex]);
				}

				if (playerLevelIndex >= 0 && playerLevelIndex < options.length) {
					return RootRetreat.levelOf(player, options[playerLevelIndex]);
				}

				return 0;
			}

			@Override
			public void set(int index, int value) {
				// 只读：客户端的改动不会回到这里
			}

			@Override
			public int getCount() {
				return dataSize();
			}
		};
	}

	// ------------------------------------------------------------------ 界面读的四个值

	/** 这个方块方案的总阶段（右上那行「第 x 阶段」）。 */
	public int blockStage() {
		return this.data.get(DATA_BLOCK_STAGE);
	}

	/** 这个方块方案里某一项的等级（左列显示的等级）。 */
	public int blockLevelOf(DifficultyOption option) {
		return this.data.get(DATA_BLOCK_LEVELS + option.ordinal());
	}

	/** 你自己方案的总阶段（右下第一行）。 */
	public int playerStage() {
		return this.data.get(playerStageIndex());
	}

	/** 你自己方案里某一项的等级（右下每行「名字 z层」按它算层数）。 */
	public int playerLevelOf(DifficultyOption option) {
		return this.data.get(playerLevelsIndex() + option.ordinal());
	}

	// ------------------------------------------------------------------ 服务端的三个动作

	/**
	 * 加 / 减某一项（改的是<b>方块这份</b>草稿，玩家那份要等「应用」）。
	 *
	 * <p>客户端的按钮置灰只是显示，这里才是判据——不在这个界面里（或不是服务端）时什么都不做。
	 */
	public void adjust(DifficultyOption option, int delta) {
		if (!canEdit()) {
			return;
		}

		this.block.setLevel(option, this.block.levelOf(option) + delta);
		this.broadcastChanges();
	}

	/** 「清空」：方块这份全部回到 0 级（玩家那份不动）。 */
	public void clear() {
		if (!canEdit()) {
			return;
		}

		this.block.clear();
		this.broadcastChanges();
	}

	/** 「应用」：把方块这份方案拷到玩家身上（之后生效的就是它）。 */
	public void apply() {
		if (!canEdit()) {
			return;
		}

		RootRetreat.applyPlan(this.owner, this.block.levels());
		this.broadcastChanges();
	}

	/** 服务端、而且方块还在（挖掉了就不再改它）。 */
	private boolean canEdit() {
		return this.owner != null && this.block != null && !this.block.isRemoved();
	}

	/** 这个界面不碰物品：Shift 点击一律不搬东西。 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	/** 菜单一直有效：设置挂在玩家与方块上，玩家走开也不影响（与模组其它菜单同一套）。 */
	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
