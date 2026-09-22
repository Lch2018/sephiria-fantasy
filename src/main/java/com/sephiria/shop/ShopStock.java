package com.sephiria.shop;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 一名玩家当前的商店货架：{@value #GOOD_SLOTS} 个商品格 + 两个宝箱格 + 两个药水格。
 *
 * <p>商品按品质加权随机刷出（权重见 {@link ArtifactLoot.Weights#DEFAULT}），同一批<b>不重复</b>；
 * 刷新方式是往骰子栏放一颗骰子（{@link ShopMenu} 那边接）。
 *
 * <p>限购：宝箱每次刷新<b>共</b>能买 {@value #CHEST_LIMIT} 次（两个宝箱格共用这个额度），
 * 其余商品每格 {@value #GOOD_LIMIT} 次；买光的格子直接清空，界面上就是空槽。
 *
 * <p>货架只在内存里（按玩家存），玩家重登会重新刷一批——和技能存储一样，将来要跨会话保留
 * 再换成数据附件。
 */
public final class ShopStock extends SimpleContainer {
	public static final int GOOD_SLOTS = 6;
	public static final int CHEST_SLOTS = 2;
	public static final int POTION_SLOTS = 2;
	public static final int SLOT_COUNT = GOOD_SLOTS + CHEST_SLOTS + POTION_SLOTS;
	/** 宝箱格在货架里的起始下标（神器宝箱、石板宝箱）。 */
	public static final int CHEST_START = GOOD_SLOTS;
	/** 药水格起始下标；药水物品还没做，这两格先空着。 */
	public static final int POTION_START = GOOD_SLOTS + CHEST_SLOTS;

	public static final int CHEST_LIMIT = 3;
	public static final int GOOD_LIMIT = 1;

	private static final Map<UUID, ShopStock> STOCKS = new HashMap<>();

	/** 每格已买次数；宝箱那两格共用 {@link #CHEST_LIMIT}。 */
	private final int[] bought = new int[SLOT_COUNT];

	public ShopStock() {
		super(SLOT_COUNT);
	}

	/** 某个玩家的货架；第一次打开时刷一批。 */
	public static ShopStock of(ServerPlayer player) {
		return STOCKS.computeIfAbsent(player.getUUID(), uuid -> {
			ShopStock stock = new ShopStock();
			stock.refresh(player.getRandom());
			return stock;
		});
	}

	/** 玩家退出时把货架从缓存里放掉。 */
	public static void forget(ServerPlayer player) {
		STOCKS.remove(player.getUUID());
	}

	/** 重新刷一批货：商品按品质加权抽，宝箱固定两种，药水按品质加权抽，购买记录清零。 */
	public void refresh(RandomSource random) {
		clearContent();

		List<ItemStack> goods = ArtifactLoot.roll(random, GOOD_SLOTS, ArtifactLoot.Pool.BOTH, ArtifactLoot.Weights.DEFAULT);

		for (int index = 0; index < goods.size(); index++) {
			setItem(index, goods.get(index));
		}

		setItem(CHEST_START, new ItemStack(ModItems.ARTIFACT_CHEST));
		setItem(CHEST_START + 1, new ItemStack(ModItems.SLATE_CHEST));

		// 药水格：按品质加权抽（1000/10/5/1），价格定成 0 的药水商店不出售，抽到就留空
		List<ItemStack> potions = ArtifactLoot.rollPotions(random, POTION_SLOTS);

		for (int index = 0; index < potions.size(); index++) {
			ItemStack potion = potions.get(index);

			if (ShopPrices.buy(potion) > 0) {
				setItem(POTION_START + index, potion);
			}
		}

		java.util.Arrays.fill(this.bought, 0);
		setChanged();
	}

	/** 这一格还限购几次（宝箱两格共用额度）。 */
	public int remaining(int index) {
		if (isChestSlot(index)) {
			return Math.max(0, CHEST_LIMIT - chestBought());
		}

		return Math.max(0, GOOD_LIMIT - this.bought[index]);
	}

	/** 宝箱已经买过几次（两个格子加起来）。 */
	public int chestBought() {
		return this.bought[CHEST_START] + this.bought[CHEST_START + 1];
	}

	public static boolean isChestSlot(int index) {
		return index >= CHEST_START && index < CHEST_START + CHEST_SLOTS;
	}

	public static boolean isGoodSlot(int index) {
		return index >= 0 && index < GOOD_SLOTS;
	}

	/**
	 * 买下一格：够钱就扣树叶、把东西塞给玩家、记一次限购。
	 *
	 * <p>返回是否成功——不够钱、已售罄、格子空都算失败，调用方按返回值给音效。
	 */
	public boolean buy(int index, ServerPlayer player) {
		if (index < 0 || index >= SLOT_COUNT) {
			return false;
		}

		ItemStack stack = getItem(index);

		if (stack.isEmpty() || remaining(index) <= 0) {
			return false;
		}

		int price = ShopPrices.buy(stack);

		if (price <= 0 || !com.sephiria.stats.PlayerStats.spendLeaves(player, price)) {
			return false;
		}

		this.bought[index]++;
		// 卖光的格子清空：宝箱格要等共用额度用完（另一格还能买）才清
		if (remaining(index) <= 0 || !isChestSlot(index)) {
			setItem(index, ItemStack.EMPTY);
		}

		ItemStack boughtStack = stack.copy();

		if (!player.getInventory().add(boughtStack)) {
			player.drop(boughtStack, false, Prediction.SERVER_ONLY);
		}

		setChanged();
		return true;
	}

	/** 货架内容的同步视图：客户端只拿「每格已买几次」，价格由物品自己算。 */
	public ContainerData data() {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return index >= 0 && index < SLOT_COUNT ? bought[index] : 0;
			}

			@Override
			public void set(int index, int value) {
				// 服务端这份是货架本身，客户端那份是原版同步用的空壳，不会走到这里
			}

			@Override
			public int getCount() {
				return SLOT_COUNT;
			}
		};
	}

	/** 货架上现有的物品（界面用；客户端拿到的是原版同步过来的副本）。 */
	public List<ItemStack> stacks() {
		List<ItemStack> list = new ArrayList<>(SLOT_COUNT);

		for (int index = 0; index < SLOT_COUNT; index++) {
			list.add(getItem(index));
		}

		return list;
	}
}
