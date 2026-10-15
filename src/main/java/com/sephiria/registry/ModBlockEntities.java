package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.difficulty.RootRetreatBlockEntity;
import com.sephiria.wishpool.WishPoolBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * 方块实体注册表。
 *
 * <p>目前只有根之退一个：它要<b>跟着方块存自己的方案</b>（放两个根之退可以各存一套难度设置，
 * 见 {@code difficulty.RootRetreatBlockEntity}）。
 *
 * <p>26.3 的 {@link BlockEntityType} 构造函数是公开的、也没有 Builder（原版自己也是
 * {@code Registry.register(..., new BlockEntityType<>(factory, Set.of(blocks)))}），
 * 注册键要显式给——它同时是存档里方块实体的 id。
 */
public final class ModBlockEntities {
	public static final ResourceKey<BlockEntityType<?>> ROOT_RETREAT_KEY = ResourceKey.create(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "root_retreat"));

	/** 根之退的方块实体：存那个方块上的难度方案。 */
	public static final BlockEntityType<RootRetreatBlockEntity> ROOT_RETREAT = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			ROOT_RETREAT_KEY,
			new BlockEntityType<>(RootRetreatBlockEntity::new, Set.of(ModBlocks.ROOT_RETREAT)));

	/** 许愿池的方块实体（只负责吸升级宝箱，不存数据）。 */
	public static final ResourceKey<BlockEntityType<?>> WISH_POOL_KEY = ResourceKey.create(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool"));

	public static final BlockEntityType<WishPoolBlockEntity> WISH_POOL = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			WISH_POOL_KEY,
			new BlockEntityType<>(WishPoolBlockEntity::new, Set.of(ModBlocks.WISH_POOL)));

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
