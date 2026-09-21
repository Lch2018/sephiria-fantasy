package com.sephiria.shop;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.ChestItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.slate.SlateItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 商店的价目表：买价按品质，卖价 = 买价 × {@value #SELL_PERCENT}%。
 *
 * <p>价目只跟物品种类（品质）有关，与神器等级、格子等级无关——所以客户端不用等服务端同步价格，
 * 自己按手里的物品算一遍就是同一个数（{@link #buy} 是纯函数）。
 *
 * <p>货币是「树叶」（{@link com.sephiria.stats.PlayerStats}）。
 *
 * <p><b>收不收</b>（{@link #sellable}）与<b>卖不卖</b>（{@link #buy}）是两件事：商店会卖宝箱，
 * 但不收宝箱——只有神器、石板、药水能卖给商店，骰子 / 附魔币 / 宝箱这些稀有道具不收购。
 */
public final class ShopPrices {
	/** 白 / 蓝 / 黄 / 红 / 彩（石板为粉）五档买价。 */
	private static final int[] BY_RARITY = { 500, 1000, 1500, 2000, 3000 };
	/** 药水的五档买价：白 50、蓝 3000、黄 10086、红与彩 0（0 = 商店不出售）。 */
	private static final int[] POTION_BY_RARITY = { 50, 3000, 10086, 0, 0 };
	/** 宝箱（神器 / 石板 / 升级）买价：用户定价，三种箱子同价。 */
	public static final int CHEST_PRICE = 1500;
	/** 出售折扣：原价的 3 折。 */
	public static final int SELL_PERCENT = 30;

	private ShopPrices() {
	}

	/**
	 * 商店买价；商店不出售的东西返回 0（骰子、附魔币，以及价格定为 0 的红药水）。
	 *
	 * <p>顺序要紧：宝箱不是神器/石板/药水，先单独判掉。
	 */
	public static int buy(ItemStack stack) {
		Item item = stack.getItem();

		if (item instanceof ChestItem) {
			return CHEST_PRICE;
		}

		if (item instanceof SephiriaPotionItem potion) {
			return byPotionRarity(potion.rarity());
		}

		if (item instanceof SephiriaArtifact || item instanceof SlateItem) {
			return byRarity(ArtifactLoot.rarityOf(item));
		}

		return 0;
	}

	/** 商店收不收这种东西：只有神器、石板、药水，而且要卖得出价。 */
	public static boolean sellable(ItemStack stack) {
		Item item = stack.getItem();
		boolean wanted = item instanceof SephiriaArtifact || item instanceof SlateItem
				|| item instanceof SephiriaPotionItem;

		return wanted && buy(stack) > 0;
	}

	/** 出售价：原价的 3 折，向下取整。 */
	public static int sell(ItemStack stack) {
		return sellable(stack) ? buy(stack) * SELL_PERCENT / 100 : 0;
	}

	private static int byRarity(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> BY_RARITY[0];
			case ADVANCED -> BY_RARITY[1];
			case RARE -> BY_RARITY[2];
			case LEGENDARY -> BY_RARITY[3];
			// 神器最高是羁绊、石板最高是永恒，两者同价
			case BOND, ETERNAL -> BY_RARITY[4];
		};
	}

	private static int byPotionRarity(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> POTION_BY_RARITY[0];
			case ADVANCED -> POTION_BY_RARITY[1];
			case RARE -> POTION_BY_RARITY[2];
			case LEGENDARY -> POTION_BY_RARITY[3];
			case BOND, ETERNAL -> POTION_BY_RARITY[4];
		};
	}
}
