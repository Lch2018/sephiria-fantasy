package com.sephiria.artifact;

import com.sephiria.backpack.ExtraBackpackRowItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * 赛菲莉娅道具（附魔币 / 骰子 / 额外背包栏 / 宝箱）的品质。
 *
 * <p>刻意不让这些道具实现 {@link Quality}：那个接口的口径是「抽奖奖池里的东西」，
 * 让宝箱之类的道具也实现它，会连带改变 {@link ArtifactLoot#rarityOf} 在售价与许愿池里的结果。
 * 名字和提示框都走这一处，免得两边各判一次、以后加道具时漏改一边。
 */
public final class ItemQualities {
	private ItemQualities() {
	}

	/** 没有道具品质的物品返回 {@code null}。 */
	public static ArtifactRarity of(Item item) {
		if (item instanceof EnchantCoinItem || item instanceof ExtraBackpackRowItem) {
			return ArtifactRarity.LEGENDARY;
		}

		if (item instanceof DiceItem) {
			return ArtifactRarity.RARE;
		}

		if (item instanceof ChestItem chest) {
			return chest.kind().rareOnly() ? ArtifactRarity.LEGENDARY : ArtifactRarity.RARE;
		}

		return null;
	}

	/** 道具名字：有品质就按品质上色，否则保持原版白名。 */
	public static Component nameOf(Item item) {
		Component name = Component.translatable(item.getDescriptionId());
		ArtifactRarity rarity = of(item);
		return rarity == null ? name : rarity.style(name.getString());
	}
}
