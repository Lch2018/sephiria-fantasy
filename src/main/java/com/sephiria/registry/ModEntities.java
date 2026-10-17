package com.sephiria.registry;

import com.sephiria.Sephiria;
import com.sephiria.trader.SephiriaTraderLlama;
import com.sephiria.trader.SephiriaWanderingTrader;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.equine.Llama;

/**
 * 本模组自己注册的生物：赛菲莉娅流浪商人 + 它的商队羊驼。
 *
 * <p>尺寸、分类与属性表全部照抄原版同族（流浪商人 0.6×1.95、商队羊驼 0.9×1.87，都是
 * CREATURE；属性分别是怪物基础属性与 {@code Llama.createAttributes()}）——用户口径
 * 「与原版流浪商人的区别只有外观和商品」，所以这几项一点都不能自己调。
 *
 * <p>战利品表由 {@code build(ResourceKey)} 自动指向 {@code sephiria_fantasy:entities/<名字>}，
 * 两个文件在 {@code data/sephiria_fantasy/loot_table/entities/} 下：商人空表（原版流浪商人也
 * 什么都不掉）、羊驼与原生商队羊驼一样掉皮革。
 */
public final class ModEntities {
	public static final EntityType<SephiriaWanderingTrader> SEPHIRIA_WANDERING_TRADER = register(
			"sephiria_wandering_trader",
			EntityType.Builder.of(SephiriaWanderingTrader::new, MobCategory.CREATURE).sized(0.6F, 1.95F));

	public static final EntityType<SephiriaTraderLlama> SEPHIRIA_TRADER_LLAMA = register(
			"sephiria_trader_llama",
			EntityType.Builder.of(SephiriaTraderLlama::new, MobCategory.CREATURE).sized(0.9F, 1.87F));

	private ModEntities() {
	}

	/** 注册发生在类加载时，由 {@link Sephiria#onInitialize()} 触发。 */
	public static void initialize() {
		FabricDefaultAttributeRegistry.register(SEPHIRIA_WANDERING_TRADER, Mob.createMobAttributes());
		FabricDefaultAttributeRegistry.register(SEPHIRIA_TRADER_LLAMA, Llama.createAttributes());
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
				Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
