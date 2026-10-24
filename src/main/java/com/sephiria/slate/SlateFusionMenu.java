package com.sephiria.slate;

import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import com.sephiria.trader.SephiriaWanderingTrader;
import com.sephiria.trader.TraderFusionUses;
import com.sephiria.util.PlayerItems;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 「石板合成」界面（用户 2026-10-24 定）：两个输入格、一个合成按钮、一个输出格，
 * 上面是 7×7 的合成石板效果预览。
 *
 * <p>预览不是容器格子——它只是按 {@link SlateFusion} 的规则把两块输入石板的图案合并后画出来，
 * 所以客户端自己就能算（输入格的内容原版会同步过来）。真正的「合成」是服务端的动作：
 * 客户端只发一个包，服务端校验两块输入、合并图案、写进输出格，再照常把格子同步回去。
 *
 * <p>输入格只收普通石板（见 {@link SlateFusion#accepts}，合成石板不能再参与合成）；输出格只能拿不能放。
 * 关界面时输入 / 输出格里剩下的东西会还给玩家（背包满就掉在脚下），不会凭空消失。
 */
public class SlateFusionMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;

	/** 预览网格：7×7，一屏画完、不滚动（中心格正好放合成石板）。 */
	public static final int GRID_CELLS = 7;
	public static final int GRID_CELL = 18;
	public static final int GRID_X = (IMAGE_WIDTH - GRID_CELLS * GRID_CELL) / 2;
	public static final int GRID_Y = 18;
	/** 合成石板画在预览网格的正中心（7 列 / 7 行的第 4 格）。 */
	public static final int PREVIEW_ORIGIN = 3;

	/** 下方控制行：左两个输入格、中间合成按钮、右边输出格。 */
	public static final int INPUT_A_X = 24;
	public static final int INPUT_B_X = 42;
	public static final int OUTPUT_X = 134;
	public static final int CONTROL_Y = 156;
	public static final int BUTTON_X = 72;
	public static final int BUTTON_Y = 155;
	public static final int BUTTON_WIDTH = 48;
	public static final int BUTTON_HEIGHT = 18;
	public static final int HINT_Y = 146;

	/** 输入 / 输出格在菜单里的下标。 */
	public static final int INPUT_A = 0;
	public static final int INPUT_B = 1;
	public static final int INPUT_COUNT = 2;
	public static final int OUTPUT = 2;
	public static final int SLOTS = 3;

	public static final int PLAYER_X = 8;

	/** 数据槽：这一次界面里是否已经合成过（0 / 1，成功一次后把「合成」按钮也灰掉）。 */
	private static final int DATA_FUSION_USED = 0;
	private static final int DATA_COUNT = 1;

	private final Container inputs;
	private final Container output;
	/** 服务端：从哪只商人打开的（客户端为 null）；用来记「每个商人每个玩家一次」。 */
	private final SephiriaWanderingTrader trader;
	private final ContainerData data;

	/** 客户端构造：容器是空壳，内容等原版同步。 */
	public SlateFusionMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(INPUT_COUNT), new SimpleContainer(1), null);
	}

	private SlateFusionMenu(int containerId, Inventory playerInventory, Container inputs, Container output,
			SephiriaWanderingTrader trader) {
		super(ModMenus.SLATE_FUSION, containerId);
		this.inputs = inputs;
		this.output = output;
		this.trader = trader;
		this.data = new SimpleContainerData(DATA_COUNT);

		addSlot(new Slot(this.inputs, INPUT_A, INPUT_A_X, CONTROL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return SlateFusion.accepts(stack);
			}
		});
		addSlot(new Slot(this.inputs, INPUT_B, INPUT_B_X, CONTROL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return SlateFusion.accepts(stack);
			}
		});
		// 输出格：只能拿走（点 / Shift 点击都走原版 Slot 的那条路），不能往里放
		addSlot(new Slot(this.output, 0, OUTPUT_X, CONTROL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});

		addStandardInventorySlots(playerInventory, PLAYER_X, playerRowsY());
		addDataSlots(this.data);
	}

	public static int playerRowsY() {
		// 控制行在 155..173；给「物品栏」标签（playerRowsY - 11 = 179）留 6 px 空档
		return 190;
	}

	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/**
	 * 服务端：打开这个界面（没有额外开屏数据，用 {@link Unit} 占位）。
	 *
	 * @param trader 打开界面的那只商人；成功合成一次后记在它身上（每个商人每个玩家一次）。
	 *               没在这只商人身上用过才允许打开。
	 */
	public static void open(ServerPlayer player, SephiriaWanderingTrader trader) {
		if (trader == null || TraderFusionUses.used(trader, player)) {
			return;
		}

		player.openMenu(new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria_fantasy.slate_fusion");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return new SlateFusionMenu(containerId, inventory, new SimpleContainer(INPUT_COUNT),
						new SimpleContainer(1), trader);
			}
		});

		// 商人菜单的 removed 会先把 tradingPlayer 清掉，所以要在 openMenu 之后再定住它：
		// 合成界面开着的时候商人不会走开 / 被卸载，记在它身上的「已使用」才不会丢
		if (player.containerMenu instanceof SlateFusionMenu) {
			trader.setTradingPlayer(player);
		}
	}

	/** 这一次界面里是否已经合成过一次（成功后「合成」按钮置灰）。 */
	public boolean fusionUsed() {
		return this.data.get(DATA_FUSION_USED) != 0;
	}

	public ItemStack inputStack(int index) {
		return index >= 0 && index < INPUT_COUNT ? this.inputs.getItem(index) : ItemStack.EMPTY;
	}

	public ItemStack outputStack() {
		return this.output.getItem(0);
	}

	/** 两块输入都在、输出格还空着，才允许合成。 */
	public boolean canCraft() {
		return this.output.getItem(0).isEmpty()
				&& SlateFusion.accepts(this.inputs.getItem(INPUT_A))
				&& SlateFusion.accepts(this.inputs.getItem(INPUT_B));
	}

	/**
	 * 预览用的合并图案：输出格里已经合成了就预览那一块（结果已经定型），
	 * 否则按现在两个输入格的内容现算。
	 */
	public List<PatternSlateItem.Cell> previewPattern() {
		ItemStack result = this.output.getItem(0);

		if (result.getItem() instanceof FusedSlateItem fused) {
			return fused.pattern(result);
		}

		return SlateFusion.combine(this.inputs.getItem(INPUT_A), this.inputs.getItem(INPUT_B));
	}

	/** 预览网格上是否有东西可画（有输入或已经有合成结果）。 */
	public boolean hasPreview() {
		return !this.output.getItem(0).isEmpty()
				|| SlateFusion.accepts(this.inputs.getItem(INPUT_A))
				|| SlateFusion.accepts(this.inputs.getItem(INPUT_B));
	}

	/** 服务端：把两块输入合成为一块合成石板。 */
	public void fuse(ServerPlayer player) {
		if (!canCraft()) {
			return;
		}

		// 商人已经不在了 / 玩家走远了：服务端不认这次合成
		if (this.trader != null && (!this.trader.isAlive() || this.trader.distanceToSqr(player) > 64.0D)) {
			return;
		}

		// 每个商人每个玩家一次：已经用过就只把按钮灰掉，不再出货
		if (this.trader != null && TraderFusionUses.used(this.trader, player)) {
			this.data.set(DATA_FUSION_USED, 1);
			this.broadcastChanges();
			return;
		}

		ItemStack first = this.inputs.getItem(INPUT_A);
		ItemStack second = this.inputs.getItem(INPUT_B);
		List<PatternSlateItem.Cell> pattern = SlateFusion.combine(first, second);
		ItemStack result = new ItemStack(ModItems.SLATE_OF_FUSION);
		result.set(FusedSlateItem.PATTERN, pattern);
		// 任一输入不可旋转，结果也不可旋转（用户 2026-10-24 定）
		result.set(FusedSlateItem.ROTATABLE, SlateFusion.resultRotatable(first, second));

		this.inputs.setItem(INPUT_A, ItemStack.EMPTY);
		this.inputs.setItem(INPUT_B, ItemStack.EMPTY);
		this.output.setItem(0, result);
		this.inputs.setChanged();
		this.output.setChanged();
		this.data.set(DATA_FUSION_USED, 1);
		if (this.trader != null) {
			TraderFusionUses.mark(this.trader, player);
		}
		this.broadcastChanges();

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
	}

	/** 服务端：按 R 转动某个输入格里的石板。 */
	public void rotateInput(int index) {
		if (index < 0 || index >= INPUT_COUNT) {
			return;
		}

		ItemStack stack = this.inputs.getItem(index);

		if (stack.getItem() instanceof SlateItem slate && slate.rotatable(stack)) {
			SlateItem.rotate(stack);
			this.inputs.setChanged();
			this.broadcastChanges();
		}
	}

	/**
	 * 服务端：商人还活着且在 8 格内；客户端没有商人实体，恒真（由服务端关界面）。
	 * 与商人交易页同一套距离口径。
	 */
	@Override
	public boolean stillValid(Player player) {
		return this.trader == null
				|| (this.trader.isAlive() && this.trader.distanceToSqr(player) <= 64.0D);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack result = stack.copy();

		if (index < SLOTS) {
			// 输入 / 输出格 → 玩家背包
			if (!moveItemStackTo(stack, SLOTS, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			// 玩家背包 → 输入格（输出格 mayPlace=false，进不去）
			if (!moveItemStackTo(stack, 0, INPUT_COUNT, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return result;
	}

	/** 关界面时把输入 / 输出格里剩下的东西还给玩家，并放开商人（打开时把它定住了）。 */
	@Override
	public void removed(Player player) {
		super.removed(player);

		if (player instanceof ServerPlayer serverPlayer) {
			returnAll(serverPlayer, this.output, 1);
			returnAll(serverPlayer, this.inputs, INPUT_COUNT);
		}

		if (this.trader != null) {
			this.trader.setTradingPlayer(null);
		}
	}

	private static void returnAll(ServerPlayer player, Container container, int size) {
		for (int index = 0; index < size; index++) {
			ItemStack stack = container.getItem(index);

			if (!stack.isEmpty()) {
				PlayerItems.give(player, stack);
				container.setItem(index, ItemStack.EMPTY);
			}
		}
	}
}
