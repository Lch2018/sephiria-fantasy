package com.sephiria.shop;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.ChestItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.registry.ModItems;
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
 */
public final class ShopPrices {
	/** 白 / 蓝 / 黄 / 红 / 彩（石板为粉）五档买价。 */
	private static final int[] BY_RARITY = { 500, 1000, 1500, 2000, 3000 };
	/** 宝箱（神器 / 石板 / 升级）买价：用户定价，三种箱子同价。 */
	public static final int CHEST_PRICE = 1500;
	/** 骰子买价：用户没定价，暂按普通品质算。 */
	public static final int DICE_PRICE = 500;
	/** 神器附魔币买价：按普通品质算。 */
	public static final int COIN_PRICE = 500;
	/** 出售折扣：原价的 3 折。 */
	public static final int SELL_PERCENT = 30;

	private ShopPrices() {
	}

	/**
	 * 商店买价；不收的东西返回 0。
	 *
	 * <p>顺序要紧：宝箱与骰子、附魔币都不是神器/石板，{@link ArtifactLoot#rarityOf} 会把它们
	 * 当普通品质算成 500，所以先单独判掉。
	 */
	public static int buy(ItemStack stack) {
		Item item = stack.getItem();

		if (item instanceof ChestItem) {
			return CHEST_PRICE;
		}

		if (item == ModItems.DICE) {
			return DICE_PRICE;
		}

		if (item == ModItems.ENCHANT_COIN) {
			return COIN_PRICE;
		}

		if (item instanceof SephiriaArtifact || item instanceof SlateItem) {
			return byRarity(ArtifactLoot.rarityOf(item));
		}

		return 0;
	}

	/** 出售价：原价的 3 折，向下取整。 */
	public static int sell(ItemStack stack) {
		return buy(stack) * SELL_PERCENT / 100;
	}

	/** 商店收不收这种物品。 */
	public static boolean sellable(ItemStack stack) {
		return buy(stack) > 0;
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
}
