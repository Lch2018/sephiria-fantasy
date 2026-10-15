package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.difficulty.RootRetreatBlock;
import com.sephiria.wishpool.WishPoolBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 方块注册表。
 *
 * <p>目前只有一个：<b>根之退</b>（{@code difficulty.RootRetreatBlock}）——石碑状的功能方块，
 * 右键打开调难度的界面。
 *
 * <p>两件事容易漏：① 1.21.2 起 {@code Properties} 必须 {@code setId(...)}（原版用它算默认掉落表
 * 与一些报错信息，不设会在构造方块时抛异常）；② 方块物品要在 {@link ModItems} 里注册
 * （{@code new BlockItem(...)}），否则创造页与背包里都拿不到它。
 */
public final class ModBlocks {
	/** 方块 id（{@code sephiria_fantasy:root_retreat}）。 */
	public static final Identifier ROOT_RETREAT_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "root_retreat");
	/** 方块的注册键：{@code Properties.setId} 与默认掉落表都用它。 */
	public static final ResourceKey<Block> ROOT_RETREAT_KEY = ResourceKey.create(Registries.BLOCK, ROOT_RETREAT_ID);

	/**
	 * 根之退：石碑状的功能方块。
	 *
	 * <p>石头手感、不要求工具（A 测阶段先让它随手就能挖走，掉落表就放在
	 * {@code data/sephiria_fantasy/loot_table/blocks/root_retreat.json}）；
	 * 模型不是整块方块（{@code noOcclusion}，不然贴着它的面会被剔掉）。
	 */
	public static final Block ROOT_RETREAT = Registry.register(
			BuiltInRegistries.BLOCK,
			ROOT_RETREAT_ID,
			new RootRetreatBlock(BlockBehaviour.Properties.of()
					.setId(ROOT_RETREAT_KEY)
					.strength(2.0F, 6.0F)
					.sound(SoundType.STONE)
					.noOcclusion()));

	/** 许愿池 id（{@code sephiria_fantasy:wish_pool}）。 */
	public static final Identifier WISH_POOL_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "wish_pool");
	/** 许愿池的注册键。 */
	public static final ResourceKey<Block> WISH_POOL_KEY = ResourceKey.create(Registries.BLOCK, WISH_POOL_ID);

	/**
	 * 许愿池：四叶草装饰的水池，右键打开抽卡界面，同时会把扔进来的升级宝箱吸走换四叶草。
	 *
	 * <p>与根之退一样：石头手感、不要求工具、掉落表放在
	 * {@code data/sephiria_fantasy/loot_table/blocks/wish_pool.json}；模型不是整块方块所以 noOcclusion。
	 */
	public static final Block WISH_POOL = Registry.register(
			BuiltInRegistries.BLOCK,
			WISH_POOL_ID,
			new WishPoolBlock(BlockBehaviour.Properties.of()
					.setId(WISH_POOL_KEY)
					.strength(2.0F, 6.0F)
					.sound(SoundType.STONE)
					.noOcclusion()));

	private ModBlocks() {
	}

	public static void initialize() {
	}
}
