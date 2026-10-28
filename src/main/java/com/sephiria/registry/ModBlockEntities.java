package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.beacon.SephiriaBeaconBlockEntity;
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
 * <p>目前有三个：根之退（存方块自己的难度方案）、许愿池（ticker 吸升级宝箱）与
 * 赛菲莉娅信标（单格物品 + 光柱颜色）。
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

	/** 赛菲莉娅信标的方块实体：存那一件物品，并给原版信标渲染器提供光柱颜色。 */
	public static final ResourceKey<BlockEntityType<?>> SEPHIRIA_BEACON_KEY = ResourceKey.create(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sephiria_beacon"));

	public static final BlockEntityType<SephiriaBeaconBlockEntity> SEPHIRIA_BEACON = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			SEPHIRIA_BEACON_KEY,
			new BlockEntityType<>(SephiriaBeaconBlockEntity::new, Set.of(ModBlocks.SEPHIRIA_BEACON)));

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
