package com.sephiria.beacon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 赛菲莉娅信标：一个只能放一件物品的方块容器，容器里有东西时会向天空发光柱。
 *
 * <p>方块本身不存数据，物品和光柱颜色都在 {@link SephiriaBeaconBlockEntity} 里。
 * 右键打开一个单格界面；拿走上层物品后光柱立即消失。
 *
 * <p>光柱颜色不是方块状态，所以不能靠 blockstate 变体；客户端渲染直接读方块实体里同步过去的物品。
 */
public class SephiriaBeaconBlock extends Block implements EntityBlock {
	public SephiriaBeaconBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SephiriaBeaconBlockEntity(pos, state);
	}

	/** 右键打开单格容器界面（空手或拿着东西都行，和根之退 / 许愿池一致）。 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof SephiriaBeaconBlockEntity beacon) {
			serverPlayer.openMenu(SephiriaBeaconMenu.provider(serverPlayer, beacon));
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * 玩家挖掉方块时把容器里那件物品吐出来。
	 *
	 * <p>方块自己的掉落（信标本体）仍走 loot_table/blocks/sephiria_beacon.json；
	 * 容器内容不能靠那张表，因为它不知道方块实体里装了什么。
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SephiriaBeaconBlockEntity beacon) {
			ItemStack held = beacon.removeItemNoUpdate(0);

			if (!held.isEmpty()) {
				Block.popResource(level, pos, held);
			}
		}

		return super.playerWillDestroy(level, pos, state, player);
	}

	/** 被爆炸炸掉时同样把内容物吐出来。 */
	@Override
	public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
		if (level.getBlockEntity(pos) instanceof SephiriaBeaconBlockEntity beacon) {
			ItemStack held = beacon.removeItemNoUpdate(0);

			if (!held.isEmpty()) {
				Block.popResource(level, pos, held);
			}
		}

		super.wasExploded(level, pos, explosion);
	}

	/** 界面标题 / 结构定位提示都用这个键。 */
	public static Component title() {
		return Component.translatable("block.sephiria_fantasy.sephiria_beacon");
	}
}
