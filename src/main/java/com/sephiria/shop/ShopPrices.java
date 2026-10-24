package com.sephiria.shop;

import com.sephiria.artifact.ArtifactLoot;
import com.sephiria.artifact.ArtifactRarity;
import com.sephiria.artifact.ChestItem;
import com.sephiria.artifact.SephiriaArtifact;
import com.sephiria.potion.SephiriaPotionItem;
import com.sephiria.registry.ModItems;
import com.sephiria.slate.SlateItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * 商店的价目表：买价按品质，卖价 = 买价 × {@value #SELL_PERCENT}%。
 *
 * <p>价目只跟物品种类（品质）有关，与神器等级、格子等级无关——所以客户端不用等服务端同步价格，
 * 自己按手里的物品算一遍就是同一个数（{@link #buy} 是纯函数）。
 *
 * <p>货币是「叶子」（{@link com.sephiria.stats.PlayerStats}）；谈判力给的折扣只折算买价
 * （{@link #discounted}），卖价仍按原价的 3 折，免得低买高卖套利。
 *
 * <p><b>收不收</b>（{@link #sellable}）与<b>卖不卖</b>（{@link #buy}）是两件事：商店会卖宝箱，
 * 但不收宝箱——能卖给商店的只有神器、石板、药水，以及 {@link #SELL_ONLY} 里单独定价的原版材料
 * （铜锭）；骰子 / 附魔币 / 宝箱这些稀有道具不收购。
 */
public final class ShopPrices {
	/** 白 / 蓝 / 黄 / 红 / 彩 五档买价：神器用这套。 */
	private static final int[] BY_RARITY = { 250, 500, 750, 1000, 1500 };
	/**
	 * 石板的五档买价（用户 2026-10-17：石板价格下调 30%）——所以不再与神器共用一张表。
	 * 175 / 350 / 525 / 700 / 1050 恰好是神器那套的 7 折，向下取整不亏分。
	 */
	private static final int[] SLATE_BY_RARITY = { 175, 350, 525, 700, 1050 };
	/** 药水的五档买价：白 25、蓝 1500、黄 5043、红与彩 0（0 = 商店不出售）。 */
	private static final int[] POTION_BY_RARITY = { 25, 1500, 5043, 0, 0 };
	/** 宝箱（神器 / 升级）买价：用户定价。 */
	public static final int CHEST_PRICE = 750;
	/**
	 * 石板宝箱买价：比另两种箱子便宜 30%（用户 2026-10-18 定的，750 × 0.7 = 525），
	 * 与石板本身「比神器便宜 30%」是同一条口径。
	 */
	public static final int SLATE_CHEST_PRICE = 525;
	/** 出售折扣：原价的 3 折。 */
	public static final int SELL_PERCENT = 30;
	/**
	 * 商店<b>只收不卖</b>的东西：固定出售价（叶子/个），与买价无关。
	 *
	 * <p>铜锭是第一个——原版材料本来卖不出叶子，用户 2026-10-16 给了它 1 叶子的出口。
	 * 放在这里而不是走「买价 × 3 折」：这些东西商店根本不进货，没有买价可折算。
	 */
	private static final Map<Item, Integer> SELL_ONLY = Map.of(Items.COPPER_INGOT, 1);

	private ShopPrices() {
	}

	/**
	 * 商店买价；商店不出售的东西返回 0（骰子、附魔币，以及价格定为 0 的红药水）。
	 *
	 * <p>顺序要紧：宝箱不是神器/石板/药水，先单独判掉。
	 */
	public static int buy(ItemStack stack) {
		Item item = stack.getItem();

		// 天国只是「看起来是永恒」：不进经济系统，既不能买也不能卖
		// 合成石板同理：它只从石板合成得到，不进商店（卖回商店会让它变相成为经济道具）
		if (item == ModItems.SLATE_OF_HEAVEN || item == ModItems.SLATE_OF_FUSION) {
			return 0;
		}

		if (item instanceof ChestItem chest) {
			// 稀有石板宝箱按石板宝箱的价走，稀有神器宝箱按神器宝箱的价走（两者都只能卖，不出售）
			boolean slateChest = chest.kind() == ChestItem.Kind.SLATE
					|| chest.kind() == ChestItem.Kind.RARE_SLATE;
			return slateChest ? SLATE_CHEST_PRICE : CHEST_PRICE;
		}

		if (item instanceof SephiriaPotionItem potion) {
			return byPotionRarity(potion.rarity());
		}

		if (item instanceof SlateItem) {
			return bySlateRarity(ArtifactLoot.rarityOf(item));
		}

		if (item instanceof SephiriaArtifact) {
			return byRarity(ArtifactLoot.rarityOf(item));
		}

		return 0;
	}

	/** 商店收不收这种东西：神器、石板、药水（要卖得出价），外加 {@link #SELL_ONLY} 里的原版材料。 */
	public static boolean sellable(ItemStack stack) {
		Item item = stack.getItem();
		boolean wanted = item instanceof SephiriaArtifact || item instanceof SlateItem
				|| item instanceof SephiriaPotionItem;

		return (wanted && buy(stack) > 0) || SELL_ONLY.containsKey(item);
	}

	/** 出售价：只收不卖的按 {@link #SELL_ONLY} 的定价，其余按原价 3 折向下取整。 */
	public static int sell(ItemStack stack) {
		Integer fixed = SELL_ONLY.get(stack.getItem());

		if (fixed != null) {
			return fixed;
		}

		return sellable(stack) ? buy(stack) * SELL_PERCENT / 100 : 0;
	}

	/** 按折扣（%）折算后的实付价：向下取整，别让四舍五入让玩家多掏钱。 */
	public static int discounted(int base, double discountPercent) {
		return (int) (base * (1.0D - discountPercent / 100.0D));
	}

	private static int byRarity(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> BY_RARITY[0];
			case ADVANCED -> BY_RARITY[1];
			case RARE -> BY_RARITY[2];
			case LEGENDARY -> BY_RARITY[3];
			// 神器最高是羁绊
			case BOND, ETERNAL -> BY_RARITY[4];
		};
	}

	/** 石板的买价（自家那张 7 折的表）。 */
	private static int bySlateRarity(ArtifactRarity rarity) {
		return switch (rarity) {
			case COMMON -> SLATE_BY_RARITY[0];
			case ADVANCED -> SLATE_BY_RARITY[1];
			case RARE -> SLATE_BY_RARITY[2];
			case LEGENDARY -> SLATE_BY_RARITY[3];
			// 石板最高是永恒（与神器那边的羁绊同档）
			case BOND, ETERNAL -> SLATE_BY_RARITY[4];
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
