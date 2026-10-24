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

	/**
	 * 「增加连击数量」的合并次数：合并一次 +1（0..{@value #MAX_COMBO_BONUS}）。
	 *
	 * <p>注意这不是属性值——属性值见 {@link #comboCountOf}：每件神器天生就提供 1 级，
	 * 合并一次变成 2 级（封顶）。
	 */
	public static final DataComponentType<Integer> COMBO_BONUS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "artifact_combo_bonus"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** 「增加连击数量」的合并次数上限（合并到 2 次，但属性值早就在 2 封顶了）。 */
	public static final int MAX_COMBO_BONUS = 2;
	/**
	 * 「增加连击数量」属性的上限（用户 2026-10-24 定）：每件神器默认就提供 1 级连击，
	 * 合并一次变 2 级，之后不再涨（继续合并只涨神器等级）。
	 */
	public static final int MAX_COMBO_COUNT = 2;

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

	/** 这件神器提供的<b>连击数量</b>（提示框里「「坚固」+x」的那个 x）：默认 1，合并过一次变 2。 */
	public static int comboCountOf(ItemStack stack) {
		return Math.min(MAX_COMBO_COUNT, 1 + comboBonusOf(stack));
	}

	/** 合并次数（内部计数，0..{@value #MAX_COMBO_BONUS}）；对外请用 {@link #comboCountOf}。 */
	public static int comboBonusOf(ItemStack stack) {
		return Mth.clamp(stack.getOrDefault(COMBO_BONUS, 0), 0, MAX_COMBO_BONUS);
	}

	/** 改额外连击数（合并时 +1；超过上限夹住）。 */
	public static void setComboBonus(ItemStack stack, int bonus) {
		stack.set(COMBO_BONUS, Mth.clamp(bonus, 0, MAX_COMBO_BONUS));
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
