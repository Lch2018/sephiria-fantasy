package com.sephiria.trader;

import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.player.Player;
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
 * <p>商品：9 格（7 格道具 + 2 格药水），**全部免费**（2026-10-24 用户定：这是一份礼物）。
 * 交易走**自定义界面** {@link SephiriaTraderMenu}（右键商人打开，不再用原版交易页）：
 * 点一格直接拿走一件，卖完不补货。道具池是「神器附魔币 / 骰子 / 赛菲莉娅额外背包栏」
 * 三选一（每格独立等概率），药水池是**全部**赛菲莉娅药水（按物品注册表现扫，以后新增的
 * 药水自动进来）。每格 {@value #STOCK} 件。
 */
public class SephiriaWanderingTrader extends WanderingTrader {
	/** 道具出售位数量。 */
	public static final int GOOD_SLOTS = 7;
	/** 药水出售位数量。 */
	public static final int POTION_SLOTS = 2;
	/** 交易界面的总格数（道具 7 + 药水 2）：界面按这个铺格子，别只数道具那 7 格。 */
	public static final int TOTAL_SLOTS = GOOD_SLOTS + POTION_SLOTS;
	/**
	 * 报价里那份「花费」的数量（只是占位）。交易走自定义界面 {@link SephiriaTraderMenu}，
	 * **实际不收任何东西**（2026-10-24 用户定：全部免费、一份礼物）；这里写 1 是因为
	 * 原版 {@code MerchantOffer} 必须有花费，别的界面万一读到也不会显示成 0。
	 */
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
	 * 一条「卖」的报价：1 件货（自定义界面里免费领）。
	 *
	 * <p>花费写 1 绿宝石只是占位（原版 {@code MerchantOffer} 必须有花费）；本模组自己打开的
	 * 界面 {@link SephiriaTraderMenu} 不收钱，原版交易页也在 {@link #mobInteract} 里换掉了。
	 * 价格倍率给 0，需求/次数都不会把价格推上去。
	 */
	private static MerchantOffer offer(Item item) {
		return new MerchantOffer(new ItemCost(Items.EMERALD, PRICE), new ItemStack(item), STOCK, TRADE_XP, 0.0F);
	}

	/**
	 * 右键打开自定义交易界面（2026-10-24 起不再用原版交易页）。
	 *
	 * <p>界面是 {@link SephiriaTraderMenu}：点一格免费拿走一件，库存仍记在这份 offers 上
	 * （卖完不补货）。打开时 {@code setTradingPlayer} 会把商人定住，关界面时放开。
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.isAlive() || player.isSpectator()) {
			return super.mobInteract(player, hand);
		}

		if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
			SephiriaTraderMenu.open(serverPlayer, this);
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * 拿走第 index 格：还有货就返回那件东西并标记售出（不补货）。
	 *
	 * <p>{@code offers} 是原版流浪商人那份数据（随存档走），每个 offer 的 {@code uses}
	 * 就是这一格已卖出几件——{@code maxUses = STOCK}，所以拿一次就售罄。
	 */
	public ItemStack takeGood(int index) {
		MerchantOffers offers = this.getOffers();

		if (index < 0 || index >= offers.size()) {
			return ItemStack.EMPTY;
		}

		MerchantOffer offer = offers.get(index);

		if (offer.isOutOfStock()) {
			return ItemStack.EMPTY;
		}

		ItemStack good = offer.getResult().copy();
		offer.increaseUses();
		return good;
	}
}
