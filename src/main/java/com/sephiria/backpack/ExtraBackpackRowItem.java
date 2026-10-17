package com.sephiria.backpack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赛菲莉娅额外背包栏（赛菲莉娅道具）：右键使用，赛菲莉娅背包**永久 +1 格**。
 *
 * <p>格子数写进玩家附件（{@link ArtifactBackpack#addSlot} 内部会走 {@code setChanged} 存档），
 * 所以重登、重启都保留。上限是 {@link ArtifactBackpack#MAX_SLOTS} 格：到顶之后再右键只给一条
 * 提示加「不行」的音效、**不消耗物品**。
 *
 * <p>界面一次只显示 {@link BackpackWindow#SLOTS} 格，多出来的靠滚轮翻
 * （见 {@link ArtifactBackpackMenu}），所以扩出来的格子当场就能滚到，不用关界面重开。
 */
public class ExtraBackpackRowItem extends Item {
	public ExtraBackpackRowItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		// 客户端只负责把「用了」这个动作做出来，真正生效与消耗都只在服务端
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ArtifactBackpack backpack = ArtifactBackpack.of(serverPlayer);

		if (!backpack.addSlot()) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"message.sephiria_fantasy.backpack_slot_max", ArtifactBackpack.MAX_SLOTS), true);
			level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
					SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7F, 1.1F);
			return InteractionResult.FAIL;
		}

		// consume 会自己判断创造模式（无限材料时不扣）
		stack.consume(1, player);
		serverPlayer.sendSystemMessage(Component.translatable(
				"message.sephiria_fantasy.backpack_slot_added", backpack.slots(),
				ArtifactBackpack.MAX_SLOTS), true);
		level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
		return InteractionResult.SUCCESS;
	}
}
