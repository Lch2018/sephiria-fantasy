package com.sephiria.backpack;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ChestItem;
import com.sephiria.registry.ModMenus;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 宝箱页面：上面 {@value ChestItem#REWARDS} 格随机奖励（只展示、点一件直接拿走），下面是玩家背包。
 *
 * <p>奖励在<b>打开时</b>才随机（服务端算），点中哪一格就把那一格的东西塞进玩家背包，同时消耗掉
 * 手里的宝箱——按设计「选择后宝箱消失，在物品栏中用选择的神器填充该栏」。
 */
public class ChestMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	public static final int SLOT_X = 26;
	public static final int SLOT_Y = 22;
	private static final int PLAYER_ROW_GAP = 13;
	/** 奖励格的横向间距与每行格数。 */
	private static final int SLOT_STEP = 20;
	/** 奖励格数量（界面用它判断哪些格子是奖励格）。 */
	public static final int REWARD_SLOTS = ChestItem.REWARDS;

	private final Container rewards;
	private final InteractionHand hand;
	/** 是否已经选过（选完就把剩余奖励清掉，防止再拿）。 */
	private boolean claimed;

	/** 客户端用的构造：空壳容器，内容等原版同步。 */
	public ChestMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(ChestItem.REWARDS), InteractionHand.MAIN_HAND);
	}

	public ChestMenu(int containerId, Inventory playerInventory, Container rewards, InteractionHand hand) {
		super(ModMenus.CHEST, containerId);
		this.rewards = rewards;
		this.hand = hand;

		for (int index = 0; index < ChestItem.REWARDS; index++) {
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

		addStandardInventorySlots(playerInventory, 8, playerRowsY());
	}

	public static int playerRowsY() {
		return SLOT_Y + 18 + PLAYER_ROW_GAP;
	}

	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：按手里的宝箱种类抽好奖励再打开。 */
	public static MenuProvider provider(ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		ChestItem.Kind kind = held.getItem() instanceof ChestItem chest ? chest.kind() : ChestItem.Kind.ARTIFACT;
		Container rewards = new SimpleContainer(ChestItem.REWARDS);

		java.util.List<ItemStack> rolled = ArtifactLoot.roll(player.getRandom(),
				Math.min(ChestItem.REWARDS, ArtifactLoot.MAX_ROLL), kind.withSlates(), kind.weights());

		for (int index = 0; index < rolled.size(); index++) {
			rewards.setItem(index, rolled.get(index));
		}

		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria.chest");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return new ChestMenu(containerId, inventory, rewards, hand);
			}
		};
	}

	/** 点奖励格 = 选它。 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId >= 0 && slotId < ChestItem.REWARDS && !this.claimed) {
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

		if (!player.getInventory().add(reward)) {
			player.drop(reward, false);
		}

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
		// closeContainer 是 protected，服务端这边直接把界面关掉
		serverPlayer.closeContainer();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
