package com.sephiria.registry;

import com.mojang.serialization.MapCodec;
import com.sephiria.Sephiria;
import com.sephiria.worldgen.SephiriaTreasurePiece;
import com.sephiria.worldgen.SephiriaTreasureStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * 世界生成注册表：赛菲莉娅宝藏的结构类型与结构片段类型。
 *
 * <p>结构本身的内容（群系标签、结构集、间距）都是数据驱动的 JSON；这里只注册代码侧的两个
 * 类型 id，JSON 里的 {@code type} 与存档里的结构片段都靠它们解析。
 */
public final class ModStructures {
	public static final StructureType<SephiriaTreasureStructure> SEPHIRIA_TREASURE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
					new ResourceLocation(Sephiria.MOD_ID, "sephiria_treasure"),
					new StructureType<SephiriaTreasureStructure>() {
						@Override
						public com.mojang.serialization.Codec<SephiriaTreasureStructure> codec() {
							return SephiriaTreasureStructure.CODEC;
						}
					});

	public static final StructurePieceType SEPHIRIA_TREASURE_PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
					new ResourceLocation(Sephiria.MOD_ID, "sephiria_treasure_piece"),
					(context, tag) -> new SephiriaTreasurePiece(tag));

	private ModStructures() {
	}

	public static void initialize() {
	}
}
