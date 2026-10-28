package com.sephiria.worldgen;

import com.sephiria.beacon.BeaconLoot;
import com.sephiria.beacon.SephiriaBeaconBlockEntity;
import com.sephiria.registry.ModBlocks;
import com.sephiria.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * 赛菲莉娅宝藏：地表上一个孤零零的信标，内容物在生成时按群系抽好。
 *
 * <p>用 {@link ScatteredFeaturePiece} 的「平均地面高度」把 1×1 的方块贴到地表；
 * 放之前再查一次露天（{@code canSeeSky}），树上 / 洞穴顶下的点就不放，免得光柱被挡。
 */
public class SephiriaTreasurePiece extends ScatteredFeaturePiece {
	public SephiriaTreasurePiece(RandomSource random, int chunkMinX, int chunkMinZ) {
		// 放在区块最小角：/locate structure 给的就是这个坐标，玩家传送过来正好落在信标上
		// （以前放在区块中心，locate 坐标和实际方块差 +8/+8，传过去会什么都看不到）
		super(ModStructures.SEPHIRIA_TREASURE_PIECE, chunkMinX, 64, chunkMinZ,
				1, 1, 1, Direction.NORTH);
	}

	public SephiriaTreasurePiece(CompoundTag tag) {
		super(ModStructures.SEPHIRIA_TREASURE_PIECE, tag);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		// 宽 / 高 / 深 / 地面高度由 ScatteredFeaturePiece 保存；不调 super 的话重载后尺寸会变 0
		super.addAdditionalSaveData(context, tag);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
		if (!this.updateAverageGroundHeight(level, box, 0)) {
			return;
		}

		BlockPos pos = this.getWorldPos(0, 0, 0);

		// 贴到这一列最高的非空气方块之上（WORLD_SURFACE 包含树叶）。
		// 这样树冠 / 岩层不会把光柱挡住；locate 传送到的地面位置也就在附近。
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
				pos.getX(), pos.getZ());

		if (top > pos.getY()) {
			this.boundingBox.move(0, top - pos.getY(), 0);
			pos = this.getWorldPos(0, 0, 0);
		}

		// 只做边界检查：不再用 canSeeSky 当放行条件。
		// 世界生成阶段的高度图和最终地形不完全一致（树冠 / 结构自身 / 高度图缓存），
		// 用 canSeeSky 会让一部分本该生成的结构整座消失；上面已经贴到这一列最高处，
		// 光柱不会被地形挡住。
		if (!box.isInside(pos)) {
			return;
		}

		this.placeBlock(level, ModBlocks.SEPHIRIA_BEACON.defaultBlockState(), 0, 0, 0, box);

		if (level.getBlockEntity(pos) instanceof SephiriaBeaconBlockEntity beacon) {
			beacon.setItem(0, BeaconLoot.roll(level, pos, random));
		}
	}
}
