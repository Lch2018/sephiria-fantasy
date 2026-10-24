package com.sephiria.trader;

import com.sephiria.registry.ModMenus;
import com.sephiria.util.PlayerItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
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
 * 赛菲莉娅流浪商人的自定义交易界面：一排 {@value #SLOTS} 格商品（7 格道具 + 2 格药水），
 * 点一格直接拿走（免费），
 * 下面是玩家背包。
 *
 * <p>为什么不用原版交易界面（2026-10-24 用户要求）：原版 {@code MerchantOffer} 的花费只能写死
 * 一件物品、数量还会被夹到 ≥1，想「任意树苗 / 免费」都得靠注入私有方法绕，既脆又只能做一半。
 * 这里完全自己实现界面，商品与库存仍用原版那份数据：{@code offers} 是商人身上的存货
 * （每个 offer 的 uses 就是「还剩几件」，跟存档走），{@link SephiriaWanderingTrader#takeGood}
 * 拿货并标记售出，**收不收钱完全由本界面决定——现在是免费（一份礼物）**。
 */
public class SephiriaTraderMenu extends AbstractContainerMenu {
	/** 商品格数：道具 7 格 + 药水 2 格 = 9（与原版的 9 个交易位一致）。 */
	public static final int SLOTS = SephiriaWanderingTrader.TOTAL_SLOTS;
	public static final int IMAGE_WIDTH = 176;
	/** 商品格：一排 9 格，从 x = 8 起每格 18。 */
	public static final int SLOT_X = 8;
	public static final int SLOT_Y = 20;
	/** 商品格与玩家背包之间的空档（原来留给提示行，提示去掉后收回到 18）。 */
	private static final int PLAYER_ROW_GAP = 18;
	/** 面板最下面给「石板合成」按钮留的一行（在玩家背包下面，靠左）。 */
	public static final int FUSION_BUTTON_X = 8;
	public static final int FUSION_BUTTON_WIDTH = 64;
	public static final int FUSION_BUTTON_HEIGHT = 18;

	/** 数据槽：这个玩家在这只商人身上用过石板合成了吗（0 / 1）。 */
	private static final int DATA_FUSION_USED = 0;
	private static final int DATA_COUNT = 1;

	private final Container goods;
	/** 服务端：商人实体（客户端为 null，靠槽位同步拿商品）。 */
	private final SephiriaWanderingTrader trader;
	/** 服务端：打开界面的玩家（客户端为 null）；「每个商人每个玩家一次」的记录按他查。 */
	private final ServerPlayer owner;
	/** 数据槽：同步给客户端刷新「石板合成」按钮的置灰状态。 */
	private final ContainerData data;

	/** 客户端构造：商品先空着，打开菜单时服务端会把 9 格内容与按钮状态同步过来。 */
	public SephiriaTraderMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new SimpleContainer(SLOTS), null, null);
	}

	public SephiriaTraderMenu(int containerId, Inventory playerInventory, Container goods,
			SephiriaWanderingTrader trader, ServerPlayer owner) {
		super(ModMenus.TRADER, containerId);
		this.goods = goods;
		this.trader = trader;
		this.owner = owner;
		this.data = new SimpleContainerData(DATA_COUNT);

		// 打开界面时先按商人身上的记录决定按钮状态；用掉之后再打开就是 1
		if (trader != null && owner != null && TraderFusionUses.used(trader, owner)) {
			this.data.set(DATA_FUSION_USED, 1);
		}

		for (int index = 0; index < SLOTS; index++) {
			addSlot(new Slot(goods, index, SLOT_X + index * 18, SLOT_Y) {
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
		addDataSlots(this.data);
	}

	public SephiriaWanderingTrader trader() {
		return this.trader;
	}

	/** 客户端读数据槽：石板合成按钮是否置灰。 */
	public boolean fusionUsed() {
		return this.data.get(DATA_FUSION_USED) != 0;
	}

	/** 服务端：这个玩家还能不能打开石板合成（没在这只商人身上用过、商人还活着且在附近）。 */
	public boolean canOpenFusion() {
		return this.trader != null && this.owner != null && this.trader.isAlive()
				&& this.trader.distanceToSqr(this.owner) <= 64.0D
				&& !TraderFusionUses.used(this.trader, this.owner);
	}

	public static int playerRowsY() {
		return SLOT_Y + 18 + PLAYER_ROW_GAP;
	}

	/** 「石板合成」按钮在面板里的 y（玩家背包下面，留 6 px 空档）。 */
	public static int fusionButtonY() {
		return playerRowsY() + 4 * 18 + 6;
	}

	public static int imageHeight() {
		return fusionButtonY() + FUSION_BUTTON_HEIGHT + 4;
	}

	/** 服务端：按商人现在的存货铺界面（已经领掉的格子留空）。 */
	public static void open(ServerPlayer player, SephiriaWanderingTrader trader) {
		Container goods = new SimpleContainer(SLOTS);
		var offers = trader.getOffers();

		for (int index = 0; index < SLOTS && index < offers.size(); index++) {
			var offer = offers.get(index);
			goods.setItem(index, offer.isOutOfStock() ? ItemStack.EMPTY : offer.getResult().copy());
		}

		trader.setTradingPlayer(player);
		// ⚠ 菜单是用 ExtendedMenuType 注册的，必须用 ExtendedMenuProvider 打开
		// （SimpleMenuProvider 会直接抛「must be opened with an ExtendedMenuProvider!」）
		player.openMenu(new ExtendedMenuProvider<Unit>() {
			@Override
			public Unit getScreenOpeningData(ServerPlayer ignored) {
				return Unit.INSTANCE;
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("entity.sephiria_fantasy.sephiria_wandering_trader");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
				return new SephiriaTraderMenu(containerId, inventory, goods, trader, player);
			}
		});
	}

	/** 点商品格 = 免费拿走它。 */
	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId >= 0 && slotId < SLOTS) {
			if (player instanceof ServerPlayer serverPlayer) {
				take(serverPlayer, slotId);
			}

			return;
		}

		super.clicked(slotId, button, input, player);
	}

	private void take(ServerPlayer player, int index) {
		if (this.trader == null || this.trader.isRemoved() || this.trader.distanceToSqr(player) > 64.0D) {
			return;
		}

		ItemStack good = this.trader.takeGood(index);

		if (good.isEmpty()) {
			return;
		}

		this.goods.setItem(index, ItemStack.EMPTY);
		// 背包满时掉在脚下（别用 Inventory#add 的返回值判断，见 PlayerItems 顶部）
		PlayerItems.give(player, good);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
		this.broadcastChanges();
	}

	/**
	 * 客户端没有商人实体（{@code trader == null}），距离由服务端判并在失效时关掉界面；
	 * 这里对客户端一律返回 true，免得界面刚开就被自己关掉。
	 */
	@Override
	public boolean stillValid(Player player) {
		return this.trader == null || (this.trader.isAlive() && this.trader.distanceToSqr(player) <= 64.0D);
	}

	/** 这一页不做快速移动（商品格不能拖，玩家背包也不用在这里搬）。 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	/** 关界面时放开商人，让它恢复走动（打开时 setTradingPlayer 会把它定住）。 */
	@Override
	public void removed(Player player) {
		super.removed(player);

		if (this.trader != null) {
			this.trader.setTradingPlayer(null);
		}
	}
}
