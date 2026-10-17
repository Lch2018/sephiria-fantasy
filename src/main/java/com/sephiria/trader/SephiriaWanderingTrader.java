package com.sephiria.trader;

import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛菲莉娅流浪商人：一身红袍的流浪商人，卖的是赛菲莉娅的东西。
 *
 * <p>除了外观和商品，其余全部沿用原版流浪商人：刷新时机、几率、地点、随从数量由原版的
 * {@code WanderingTraderSpawner} 决定（{@code mixin/WanderingTraderSpawnerMixin} 只把
 * 孵出来的生物换成这一族），离场机制继承，**卖完不补货**（原版流浪商人没有补货这条路）
 * ——用户口径「与原版流浪商人的区别只有外观和商品」。
 *
 * <p>商品：9 个交易位全是「卖」的方向（玩家只付绿宝石、不收货），7 格道具 + 2 格药水。
 * 道具池是「神器附魔币 / 骰子 / 赛菲莉娅额外背包栏」三选一（每格独立等概率），药水池是
 * **全部**赛菲莉娅药水（按物品注册表现扫，以后新增的药水自动进来）。每格
 * {@value #PRICE} 绿宝石、{@value #STOCK} 件。
 */
public class SephiriaWanderingTrader extends WanderingTrader {
	/** 道具出售位数量。 */
	public static final int GOOD_SLOTS = 7;
	/** 药水出售位数量。 */
	public static final int POTION_SLOTS = 2;
	/** 每格售价（绿宝石）。 */
	public static final int PRICE = 1;
	/** 每格库存：买一次就售罄，而且永不补货（与原版流浪商人一致）。 */
	public static final int STOCK = 1;
	/** 每笔交易给商人的经验（原版流浪商人也给一点）。 */
	private static final int TRADE_XP = 1;

	public SephiriaWanderingTrader(EntityType<? extends WanderingTrader> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * 铺货：9 格全是「卖」，每格独立等概率抽。
	 *
	 * <p>和原版一样通过 {@code getOffers()} 拿列表（它自己会在第一次访问时建好空表再调进来，
	 * 不会递归）。
	 */
	@Override
	protected void updateTrades(ServerLevel level) {
		MerchantOffers offers = this.getOffers();
		offers.clear();

		List<Item> goods = goodPool();
		List<Item> potions = potionPool();
		RandomSource random = this.getRandom();

		for (int slot = 0; slot < GOOD_SLOTS; slot++) {
			offers.add(offer(goods.get(random.nextInt(goods.size()))));
		}

		for (int slot = 0; slot < POTION_SLOTS; slot++) {
			offers.add(offer(potions.get(random.nextInt(potions.size()))));
		}
	}

	/** 道具出售位的三件货（用户 2026-10-16 定的口径），每格等概率三选一。 */
	private static List<Item> goodPool() {
		return List.of(ModItems.ENCHANT_COIN, ModItems.DICE, ModItems.EXTRA_BACKPACK_ROW);
	}

	/** 药水出售位：按物品注册表现扫所有赛菲莉娅药水——以后加新药水不用动这里。 */
	private static List<Item> potionPool() {
		List<Item> potions = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof SephiriaPotionItem) {
				potions.add(item);
			}
		}

		// 理论上不会空（再生药水就在注册表里），留着兜底免得刷新时抽空列表炸掉
		return potions.isEmpty() ? List.of(ModItems.REGENERATION_POTION) : potions;
	}

	/**
	 * 一条「卖」的报价：玩家付 {@value #PRICE} 绿宝石、拿 1 件。
	 *
	 * <p>只有 costA、没有 costB —— 原版就是靠「第二份花费为空」画成单方向的交易格；
	 * 价格倍率给 0，需求/次数都不会把价格推上去，价格永远是 1。
	 */
	private static MerchantOffer offer(Item item) {
		return new MerchantOffer(new ItemCost(Items.EMERALD, PRICE), new ItemStack(item), STOCK, TRADE_XP, 0.0F);
	}
}
