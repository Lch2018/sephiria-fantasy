package com.sephiria.shop;

import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModMenus;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * 商店页面：上面 6 个商品格（点一下就买），下面一排是宝箱 / 药水 / 骰子 / 出售。
 *
 * <p>买：点商品格直接结算——扣叶子、把东西塞进玩家背包、记一次限购。货架本身是服务端的
 * {@link ShopStock}，客户端拿到的是原版同步过来的空壳容器，所以价格不用同步（{@link ShopPrices}
 * 是纯函数，两边算出来一样），只有「每格买过几次」走数据槽。
 *
 * <p>刷新：骰子栏里放骰子，再点界面上的「刷新」按钮（{@link #refresh}）——不会放上去就自动刷，
 * 免得手滑白白浪费一颗骰子。
 * 出售：把赛菲利亚的东西放进出售栏，再点界面上的「出售」按钮（{@link #sell}）。
 */
public class ShopMenu extends AbstractContainerMenu {
	public static final int IMAGE_WIDTH = 176;
	/** 货架格的排布：商品一排 6 个，第二排是宝箱 / 药水 / 骰子 / 出售。 */
	public static final int SLOT_STEP = 20;
	public static final int GOOD_X = 30;
	public static final int GOOD_Y = 24;
	public static final int ROW2_Y = 56;
	public static final int CHEST_X = 30;
	public static final int POTION_X = 70;
	public static final int DICE_X = 110;
	public static final int SELL_X = 130;

	/** 非玩家背包的格子数：10 个货架格 + 骰子 + 出售。 */
	public static final int FUNCTION_SLOTS = ShopStock.SLOT_COUNT + 2;
	/** 骰子栏与出售栏在菜单里的下标。 */
	public static final int DICE_SLOT = ShopStock.SLOT_COUNT;
	public static final int SELL_SLOT = ShopStock.SLOT_COUNT + 1;
	/** 「刷新」按钮的位置（相对面板左上角）：放在出售按钮左边。 */
	public static final int REFRESH_BUTTON_X = 74;
	/** 「出售」按钮的位置（相对面板左上角）：放在出售栏正下方，不压到第二排的格子。 */
	public static final int BUTTON_X = 118;
	public static final int BUTTON_Y = 84;
	public static final int BUTTON_WIDTH = 40;
	public static final int BUTTON_HEIGHT = 14;
	/** 按钮行与玩家背包之间的空档。 */
	private static final int BUTTON_ROW_GAP = 4;

	private final Container stock;
	private final Container dice;
	private final Container sell;
	private final ContainerData bought;

	/** 客户端用的构造：空壳容器与空数据槽，内容等原版同步（骰子栏是普通容器，不会触发刷新）。 */
	public ShopMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(ShopStock.SLOT_COUNT), new SimpleContainer(1),
				new SimpleContainer(1), new SimpleContainerData(ShopStock.SLOT_COUNT));
	}

	public ShopMenu(int containerId, Inventory playerInventory, Container stock, Container dice, Container sell,
			ContainerData bought) {
		super(ModMenus.SHOP, containerId);
		this.stock = stock;
		this.dice = dice;
		this.sell = sell;
		this.bought = bought;

		for (int index = 0; index < ShopStock.SLOT_COUNT; index++) {
			int x = index < ShopStock.GOOD_SLOTS
					? GOOD_X + index * SLOT_STEP
					: row2X(index);
			int y = index < ShopStock.GOOD_SLOTS ? GOOD_Y : ROW2_Y;

			addSlot(new Slot(stock, index, x, y) {
				// 货架上的东西只能靠「买」拿到，不能直接拖走
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

		addSlot(new Slot(dice, 0, DICE_X, ROW2_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.getItem() == ModItems.DICE;
			}
		});

		addSlot(new Slot(sell, 0, SELL_X, ROW2_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ShopPrices.sellable(stack);
			}
		});

		addStandardInventorySlots(playerInventory, 8, playerRowsY());
		addDataSlots(bought);
	}

	/** 第二排格子的横坐标（宝箱 2 个、药水 2 个、骰子、出售）。 */
	private static int row2X(int index) {
		if (ShopStock.isChestSlot(index)) {
			return CHEST_X + (index - ShopStock.CHEST_START) * SLOT_STEP;
		}

		return POTION_X + (index - ShopStock.POTION_START) * SLOT_STEP;
	}

	public static int playerRowsY() {
		return BUTTON_Y + BUTTON_HEIGHT + BUTTON_ROW_GAP;
	}

	public static int imageHeight() {
		return playerRowsY() + 4 * 18 + 4;
	}

	/** 服务端使用：接上这名玩家自己的货架。 */
	public static ShopMenu forPlayer(int containerId, Inventory playerInventory, ServerPlayer player) {
		ShopStock stock = ShopStock.of(player);
		return new ShopMenu(containerId, playerInventory, stock, new SimpleContainer(1),
				new SimpleContainer(1), stock.data());
	}

	public static MenuProvider provider(ServerPlayer player) {
		return new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("screen.sephiria_fantasy.shop");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return forPlayer(containerId, inventory, player);
			}
		};
	}

	/** 某一格货架的东西（界面画价格与状态用）。 */
	public ItemStack shopStack(int index) {
		return this.stock.getItem(index);
	}

	/** 某一格还限购几次（宝箱两格共用额度）。 */
	public int remaining(int index) {
		if (index < 0 || index >= ShopStock.SLOT_COUNT) {
			return 0;
		}

		if (ShopStock.isChestSlot(index)) {
			int limit = ShopStock.CHEST_LIMIT;
			return Math.max(0, limit - boughtValue(ShopStock.CHEST_START) - boughtValue(ShopStock.CHEST_START + 1));
		}

		return Math.max(0, ShopStock.GOOD_LIMIT - boughtValue(index));
	}

	private int boughtValue(int index) {
		return this.bought.getCount() > index ? this.bought.get(index) : 0;
	}

	/** 点货架格 = 买它；其余格子走原版逻辑（顺带处理骰子刷新）。 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId >= 0 && slotId < ShopStock.SLOT_COUNT) {
			// 客户端也会走到这里（原版两边都会调 clicked），所以结算与音效都只在服务端做
			if (player instanceof ServerPlayer serverPlayer && this.stock instanceof ShopStock shopStock) {
				buy(serverPlayer, shopStock, slotId);
			}

			return;
		}

		super.clicked(slotId, button, input, player);
	}

	/** 骰子栏里还有没有骰子（刷新按钮的可用状态看它）。 */
	public boolean canRefresh() {
		ItemStack stack = this.dice.getItem(0);
		return stack.getItem() == ModItems.DICE && stack.getCount() > 0;
	}

	/**
	 * 刷新货架：吃掉骰子栏里的一颗骰子，重刷一批货（由界面上的「刷新」按钮触发）。
	 *
	 * <p>一次只吃一颗：栏里放着好几颗时，按几次按钮就刷几次，多的留着下次用。
	 */
	public boolean refresh(ServerPlayer player) {
		if (!canRefresh() || !(this.stock instanceof ShopStock shopStock)) {
			return false;
		}

		this.dice.getItem(0).shrink(1);
		this.dice.setChanged();
		shopStock.refresh(player.getRandom());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.7F, 0.9F);
		broadcastChanges();
		return true;
	}

	private void buy(ServerPlayer player, ShopStock shopStock, int index) {
		ItemStack stack = shopStock.getItem(index);

		if (stack.isEmpty() || shopStock.remaining(index) <= 0) {
			return;
		}

		if (!shopStock.buy(index, player)) {
			// 叶子不够
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 1.2F);
			return;
		}

		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.4F);
		broadcastChanges();
	}

	/** 出售栏里的东西按原价 3 折换叶子（整摞一起卖）。 */
	public boolean sell(ServerPlayer player) {
		ItemStack stack = this.sell.getItem(0);

		if (stack.isEmpty()) {
			return false;
		}

		int price = ShopPrices.sell(stack) * stack.getCount();

		if (price <= 0) {
			return false;
		}

		this.sell.removeItemNoUpdate(0);
		this.sell.setChanged();
		PlayerStats.grantLeaves(player, price);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.3F);
		broadcastChanges();
		return true;
	}

	/** 骰子栏里的东西（界面画提示用）。 */
	public ItemStack diceStack() {
		return this.dice.getItem(0);
	}

	/** 出售栏里的东西。 */
	public ItemStack sellStack() {
		return this.sell.getItem(0);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack moved = stack.copy();

		if (index < ShopStock.SLOT_COUNT) {
			// 货架格拖不动
			return ItemStack.EMPTY;
		}

		if (index < FUNCTION_SLOTS) {
			// 骰子栏 / 出售栏 → 玩家背包
			if (!moveItemStackTo(stack, FUNCTION_SLOTS, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() == ModItems.DICE) {
			if (!moveItemStackTo(stack, DICE_SLOT, DICE_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (ShopPrices.sellable(stack)) {
			if (!moveItemStackTo(stack, SELL_SLOT, SELL_SLOT + 1, false)) {
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

		// 关界面时把骰子栏与出售栏里的东西还给玩家，免得凭空消失。
		// 26.3 的 placeItemBackInInventory 要带 Prediction；照原版铁砧关界面的写法
		// 只在服务端还东西，客户端等服务器同步
		for (Container container : new Container[] { this.dice, this.sell }) {
			ItemStack left = container.removeItemNoUpdate(0);

			if (!left.isEmpty() && player instanceof ServerPlayer) {
				player.getInventory().placeItemBackInInventory(left, Prediction.SERVER_ONLY);
			}
		}
	}

}
