package com.sephiria.difficulty;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 根之退：石碑状的功能方块，右键打开「根之退界面」（调难度用）。
 *
 * <p>难度<b>方案存在方块自己身上</b>（{@link RootRetreatBlockEntity}）：放两个根之退可以各存一套，
 * 界面里点「应用」才把当前方块那份拷到玩家身上（生效的那份）。玩家自己的设置见 {@link RootRetreat}。
 *
 * <p><b>朝向像讲台</b>（用户 2026-10-19 报「始终朝南、图标也反了」之后加的）：放置时正面朝玩家，
 * 结构与 {@code LecternBlock} 同一套；模型本身是「正面朝南」画的，所以方块状态的四个朝向都
 * 相对默认多转了 180°（见 blockstates/root_retreat.json 的注释）。
 */
public class RootRetreatBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public RootRetreatBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** 正面朝玩家：取玩家水平朝向的反方向（与讲台 / 熔炉一致）。 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** 方块实体：存这个方块的方案。 */
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RootRetreatBlockEntity(pos, state);
	}

	/** 放下时记下时间：放够一个游戏日后可以直接交互（用户 2026-10-21）。 */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RootRetreatBlockEntity root) {
			root.setPlacedAt(level.getGameTime());
		}
	}

	/**
	 * 右键（空手也算）打开界面。
	 *
	 * <p>26.3 的入口是 {@code useWithoutItem}：手上拿着方块 / 工具时原版会先走
	 * {@code useItemOn}，没被消费掉才会落到这里，所以空手右键、拿着东西右键都能开。
	 * 界面走菜单（{@link RootRetreatMenu#provider}）：方块那份方案与玩家那份方案都靠
	 * 原版的数据槽同步，不用自己发包。
	 *
	 * <p><b>交互门槛</b>（用户 2026-10-21）：根之退要放在出生点 100 格内，或者放够一个游戏日
	 * （24000 刻）才激活；没激活时右键只弹一行红字，不打开界面。
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level instanceof ServerLevel serverLevel) {
			BlockEntity entity = level.getBlockEntity(pos);

			if (entity instanceof RootRetreatBlockEntity root) {
				if (!root.isActive(serverLevel)) {
					serverPlayer.sendSystemMessage(
							Component.translatable("screen.sephiria_fantasy.root_retreat.inactive")
									.withColor(0xFFFF5555));
					return InteractionResult.SUCCESS;
				}

				serverPlayer.openMenu(RootRetreatMenu.provider(serverPlayer, root));
			}
		}

		return InteractionResult.SUCCESS;
	}

	/** 界面标题（菜单里也会用到同一个键）。 */
	public static Component title() {
		return Component.translatable("block.sephiria_fantasy.root_retreat");
	}
}
