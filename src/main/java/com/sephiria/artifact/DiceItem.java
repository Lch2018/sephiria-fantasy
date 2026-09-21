package com.sephiria.artifact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 骰子（赛菲利亚道具）：可以堆叠，放进商店的骰子栏消耗一枚来刷新商品。
 *
 * <p>本身没有右键功能——刷新是商店页面的事。自然生成的箱子里有 10% 概率开出来（见
 * {@code com.sephiria.shop.DiceLoot}）。
 */
public class DiceItem extends Item {
	public DiceItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.WHITE);
	}
}
