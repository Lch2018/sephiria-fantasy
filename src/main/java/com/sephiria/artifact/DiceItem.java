package com.sephiria.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 骰子（赛菲莉娅道具）：可以堆叠，放进商店的骰子栏消耗一枚来刷新商品。
 *
	 * <p>本身没有右键功能——刷新是商店页面的事。自然生成的箱子里有 10% 概率开出来（见
	 * {@code com.sephiria.shop.ChestLoot}）。
 */
public class DiceItem extends Item {
	public DiceItem(Properties properties) {
		super(properties);
	}

	/** 骰子是稀有道具，名字跟品质色。 */
	@Override
	public Component getName(ItemStack stack) {
		return ItemQualities.nameOf(this);
	}
}
