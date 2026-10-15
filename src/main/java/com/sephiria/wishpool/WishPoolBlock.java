package com.sephiria.wishpool;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 许愿池：四叶草装饰的水池方块，右键打开抽卡界面。
 *
 * <p>用户口径（2026-10-15）：「与根之退不同，许愿池只是打开抽卡界面的入口和获取四叶草的渠道」——
 * 所以方块本身<b>不存任何东西</b>（能量与锚定都是玩家的），方块实体只负责把扔进来的升级宝箱吸走。
 */
public class WishPoolBlock extends Block implements EntityBlock {
	public WishPoolBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WishPoolBlockEntity(pos, state);
	}

	/** 方块实体每刻吸一次升级宝箱（见 {@link WishPoolBlockEntity#serverTick}）。 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		// 原版惯例：只给自己那个方块实体的类型挂 ticker
		if (level.isClientSide() || type != com.sephiria.registry.ModBlockEntities.WISH_POOL) {
			return null;
		}

		return (tickLevel, pos, tickState, entity) ->
				WishPoolBlockEntity.serverTick(tickLevel, pos, tickState, (WishPoolBlockEntity) entity);
	}

	/** 右键（空手也算）打开抽卡界面。 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(WishPoolMenu.provider(serverPlayer, pos));
		}

		return InteractionResult.SUCCESS;
	}

	/** 界面标题（菜单里也会用到同一个键）。 */
	public static Component title() {
		return Component.translatable("block.sephiria_fantasy.wish_pool");
	}
}
