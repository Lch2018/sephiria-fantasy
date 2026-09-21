package com.sephiria.artifact;

import com.sephiria.backpack.EnchantMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 神器附魔币：赛菲利亚道具（既不是武器也不是神器）。
 *
 * <p>右键打开附魔面板，在面板里放入想升级的神器，每点一次「升级」消耗一枚附魔币，
 * 让神器的自身等级 +1（达到该神器的最高等级后就不再消耗）。
 */
public class EnchantCoinItem extends Item {
	public EnchantCoinItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.CONSUME;
		}

		serverPlayer.openMenu(EnchantMenu.provider(serverPlayer));
		return InteractionResult.CONSUME;
	}

	/** 附魔币的名字也用道具品质色（普通）。 */
	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.WHITE);
	}
}
