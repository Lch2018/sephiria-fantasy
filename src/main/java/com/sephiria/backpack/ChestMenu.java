package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ChestItem;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import com.sephiria.util.PlayerItems;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
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
 * 宝箱页面：上面 {@value ChestItem#MAX_REWARDS} 格（基础 5 格，急智二阶段 6 格）随机奖励
 * （只展示、点一件直接拿走），下面是玩家背包。
 *
 * <p>奖励在<b>打开时</b>才随机（服务端算），点中哪一格就把那一格的东西塞进玩家背包，同时消耗掉
 * 手里的宝箱——按设计「选择后宝箱消失，在物品栏中用选择的神器填充该栏」。
 */
public class ChestMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	public static final int SLOT_X = 26;
	public static final int SLOT_Y = 22;
	/** 奖励区与玩家背包之间的空档：给骰子格下方的「刷新」按钮留位置。 */
	private static final int PLAYER_ROW_GAP = 33;
	/** 奖励格的横向间距与每行格数。 */
	private static final int SLOT_STEP = 20;
	/** 奖励格总槽位数（含天赋多出来的那一格；没解锁时最后一格不显示/不可点）。 */
	public static final int REWARD_SLOTS = ChestItem.MAX_REWARDS;
	/** 骰子格：奖励格右边空出来的位置。 */
	public static final int DICE_X = 148;
	/** 「刷新」按钮的位置：放在骰子格正下方（用户 2026-10-21 指定）。 */
	public static final int REROLL_BUTTON_X = 144;
	public static final int REROLL_BUTTON_Y = 42;

	/** 数据槽：这一箱实际开几格。 */
	private static final int DATA_REWARDS = 0;
	/** 数据槽：还剩几次免费刷新（天赋「智慧」三阶段每个宝箱 1 次）。 */
	private static final int DATA_FREE_REROLLS = 1;
	private static final int DATA_COUNT = 2;

	private final Container rewards;
	private final Container dice;
	private final InteractionHand hand;
	/** 这一箱实际开几格（5 或 6）+ 剩余免费刷新：服务端写、客户端读。 */
	private final ContainerData data;
	/** 服务端：开箱的玩家（客户端为 null）。筛奖励 / 刷新要用随机、幸运与天赋。 */
	private final ServerPlayer owner;
	/** 是否已经选过（选完就把剩余奖励清掉，防止再拿）。 */
	private boolean claimed;

	/** 客户端用的构造：奖励格数先按 5，服务端打开菜单时会把真实值同步过来。 */
	public ChestMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(ChestItem.MAX_REWARDS), new SimpleContainer(1),
				InteractionHand.MAIN_HAND, ChestItem.REWARDS, null);
	}

	public ChestMenu(int containerId, Inventory playerInventory, Container rewards, InteractionHand hand,
			int rewardCount, ServerPlayer owner) {
		this(containerId, playerInventory, rewards, new SimpleContainer(1), hand, rewardCount, owner);
	}

	private ChestMenu(int containerId, Inventory playerInventory, Container rewards, Container dice,
			InteractionHand hand, int rewardCount, ServerPlayer owner) {
		super(ModMenus.CHEST, containerId);
		this.rewards = rewards;
		this.dice = dice;
		this.hand = hand;
		this.owner = owner;
		this.data = new SimpleContainerData(DATA_COUNT);
		this.data.set(DATA_REWARDS, rewardCount);
		this.data.set(DATA_FREE_REROLLS, owner == null ? 0 : freeRerollsFor(owner, hand));
		addDataSlots(this.data);

		for (int index = 0; index < ChestItem.MAX_REWARDS; index++) {
			addSlot(new Slot(rewards, index, SLOT_X + index * SLOT_STEP, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}

				@Override
				public boolean mayPickup(Player player) {
					return false;
				}
			});
		}

		// 骰子格：只收骰子；刷新按钮优先用免费次数，没有再吃这里的骰子
		addSlot(new Slot(dice, 0, DICE_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() == ModItems.DICE;
			}
		});

		addStandardInventorySlots(playerInventory, 8, playerRowsY());
	}

	/** 这一箱的免费刷新次数：智慧三阶段且这件宝箱还没用过免费刷新时 1 次。 */
	private static int freeRerollsFor(ServerPlayer player, InteractionHand hand) {
		if (player == null
				|| com.sephiria.talent.Talents.stage(player, com.sephiria.talent.Talent.WISDOM) < 3) {
			return 0;
		}

		ItemStack held = player.getItemInHand(hand);
		return ChestItem.freeRerollUsed(held) ? 0 : 1;
	}

	/** 这一箱实际有几格可选（5 或 6）。 */
	public int rewardCount() {
		return Math.max(1, Math.min(ChestItem.MAX_REWARDS, this.data.get(DATA_REWARDS)));
	}

	/** 剩余免费刷新次数（客户端读同步值）。 */
	public int freeRerolls() {
		return Math.max(0, this.data.get(DATA_FREE_REROLLS));
	}

	/** 刷新按钮能不能点：有免费次数，或骰子格里还有骰子。 */
	public boolean canReroll() {
		if (this.claimed) {
			return false;
		}

		if (freeRerolls() > 0) {
			return true;
		}

		ItemStack stack = this.dice.getItem(0);
		return stack.getItem() == ModItems.DICE && stack.getCount() > 0;
	}

	public static int playerRowsY() {
		return SLOT_Y + 18 + PLAYER_ROW_GAP;
	}

	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：按手里的宝箱种类抽好奖励再打开（已经抽过的箱子沿用原来那一批）。 */
	public static MenuProvider provider(ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		ChestItem.Kind kind = held.getItem() instanceof ChestItem chest ? chest.kind() : ChestItem.Kind.ARTIFACT;
		Container rewards = new SimpleContainer(ChestItem.MAX_REWARDS);
		int choices = ChestItem.choicesFor(player);

		java.util.List<ItemStack> rolled = ChestItem.rewardsOf(held, player.getRandom(), kind,
				com.sephiria.stats.PlayerStats.luckTotal(player), choices);

		for (int index = 0; index < rolled.size(); index++) {
			rewards.setItem(index, rolled.get(index));
		}

		// 已经抽过的箱子沿用存着的那一批（长度可能是 5 或 6），界面按它的实际格数显示
		int rewardCount = Math.max(1, Math.min(ChestItem.MAX_REWARDS, rolled.size()));

		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria_fantasy.chest");
			}

				@Override
				public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
					return new ChestMenu(containerId, inventory, rewards, hand, rewardCount, player);
				}
		};
	}

	/** 点奖励格 = 选它。 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId >= 0 && slotId < rewardCount() && !this.claimed) {
			claim(player, slotId);
			return;
		}

		super.clicked(slotId, button, input, player);
	}

	private void claim(Player player, int index) {
		ItemStack reward = this.rewards.removeItemNoUpdate(index);

		if (reward.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		this.claimed = true;
		ItemStack held = player.getItemInHand(this.hand);
		held.shrink(1);
		player.getInventory().setChanged();

		// 交付走 PlayerItems.give：背包满时掉在脚下（别用 Inventory#add 的返回值判断，见它顶部）
		PlayerItems.give(serverPlayer, reward);

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
		// closeContainer 是 protected，服务端这边直接把界面关掉
		serverPlayer.closeContainer();
	}

	/**
	 * 刷新奖励：优先用免费次数（智慧三阶段每个宝箱 1 次），没有免费次数就吃骰子格里的一颗骰子。
	 *
	 * <p>按钮在客户端只是显示；真正能不能刷由这里再判一次。
	 */
	public boolean reroll(ServerPlayer player) {
		if (!canReroll()) {
			return false;
		}

		ItemStack held = player.getItemInHand(this.hand);

		if (!(held.getItem() instanceof ChestItem chest)) {
			return false;
		}

		if (freeRerolls() > 0) {
			this.data.set(DATA_FREE_REROLLS, freeRerolls() - 1);
			ChestItem.markFreeRerollUsed(held);
		} else {
			ItemStack diceStack = this.dice.getItem(0);

			if (diceStack.getItem() != ModItems.DICE || diceStack.getCount() <= 0) {
				return false;
			}

			diceStack.shrink(1);
			this.dice.setChanged();
		}

		java.util.List<ItemStack> rolled = ChestItem.reroll(held, player.getRandom(), chest.kind(),
				com.sephiria.stats.PlayerStats.luckTotal(player), rewardCount());

		for (int index = 0; index < ChestItem.MAX_REWARDS; index++) {
			this.rewards.setItem(index, index < rolled.size() ? rolled.get(index) : ItemStack.EMPTY);
		}

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.7F, 0.9F);
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

		// 玩家背包里的骰子 → 骰子格（奖励格与骰子格本身不参与快速移动）
		if (index > REWARD_SLOTS && stack.getItem() == ModItems.DICE
				&& moveItemStackTo(stack, REWARD_SLOTS, REWARD_SLOTS + 1, false)) {
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			return ItemStack.EMPTY;
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		// 关界面时把骰子格里的骰子还给玩家（免费刷新次数不返还：它属于这一箱）
		ItemStack left = this.dice.removeItemNoUpdate(0);

		if (!left.isEmpty() && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.getInventory().placeItemBackInInventory(left,
					net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}
}
