package com.sephiria.artifact;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 神器的基类：管住「等级」这一个组件，以及神器共同的通用行为。
 *
 * <p>等级放在物品的数据组件里（和刀的入鞘状态一样），堆叠、保存、联机同步都由原版负责。
 * 神器都不可堆叠（注册时 {@code stacksTo(1)}）。名字按品质上色。
 */
public abstract class ArtifactItem extends Item implements SephiriaArtifact, BackpackStorable {
	public static final DataComponentType<Integer> LEVEL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_level"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	protected ArtifactItem(Properties properties) {
		super(properties);
	}

	/** 物品自身的等级（不含格子等级），范围 0..最高等级。 */
	public static int levelOf(ItemStack stack) {
		int max = stack.getItem() instanceof SephiriaArtifact artifact ? artifact.maxLevel() : 0;
		return Mth.clamp(stack.getOrDefault(LEVEL, 0), 0, max);
	}

	/** 改等级（附魔币升级接进来时用）；超过上限会被夹住。 */
	public static void setLevel(ItemStack stack, int level) {
		int max = stack.getItem() instanceof SephiriaArtifact artifact ? artifact.maxLevel() : 0;
		stack.set(LEVEL, Mth.clamp(level, 0, max));
	}

	/** 名字按品质上色（白色 / 蓝 / 黄 / 红 / 彩色）。 */
	@Override
	public Component getName(ItemStack stack) {
		return this.rarity().style(Component.translatable(this.getDescriptionId()).getString());
	}

	/** 表格里按等级取值，越界时取到边界那一档。 */
	protected static double valueAt(double[] table, int level) {
		return table[Mth.clamp(level, 0, table.length - 1)];
	}
}
