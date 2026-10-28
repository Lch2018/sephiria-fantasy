package com.sephiria.worldgen;

import com.mojang.serialization.MapCodec;
import com.sephiria.registry.ModStructures;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 赛菲莉娅宝藏的结构类型：单方块、地表、1×1。
 *
 * <p>不直接用 {@code SinglePieceStructure}：它会给「最低点低于海平面」的候选直接判死，
 * 河流 / 沙滩这类水面群系经常正好卡在海平面，结构就永远刷不出来。这里改用
 * {@link Structure#onTopOfChunkCenter} 只做群系与区块中心检查，实际高度交给
 * {@link SephiriaTreasurePiece#postProcess} 的地面高度与露天检查。
 */
public class SephiriaTreasureStructure extends Structure {
	public static final com.mojang.serialization.Codec<SephiriaTreasureStructure> CODEC =
			Structure.simpleCodec(SephiriaTreasureStructure::new);

	public SephiriaTreasureStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> {
			ChunkPos chunk = context.chunkPos();
			builder.addPiece(new SephiriaTreasurePiece(context.random(),
					chunk.getMinBlockX(), chunk.getMinBlockZ()));
		});
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.SEPHIRIA_TREASURE;
	}
}
