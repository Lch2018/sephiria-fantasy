package com.sephiria.artifact;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.util.Numbers;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 永恒蚀（太阳剑 + 冰霜武具，羁绊品质）：【唯一】冰霜武具与太阳剑的伤害各 +6/13/21/30%（0..3 级）。
 *
 * <p><b>双连击</b>神器：同时给太阳剑与冰霜武具各加 1 级（见 {@link #combos()}）。
 *
 * <p><b>这套神器的核心是「冰火对调」</b>——看它待在赛菲莉娅背包的哪半边（6 格宽，前三列算左半边）：
 * <ul>
 *   <li><b>左半边</b>：冰霜武具（剑气 / 锤子 / 冰长矛 / 冰弓）改成<b>火焰属性</b>——
 *       伤害按火元素强度结算、伤害类型换成 {@code fire_attack}，特效从冰蓝变成焰黄；</li>
 *   <li><b>右半边</b>：太阳剑改成<b>冰属性</b>——伤害按冰元素强度结算、伤害类型换成
 *       {@code ice_sun_sword}，剑痕与掉落的太阳剑从火红变成冰蓝。</li>
 * </ul>
 * 两边的判定与效果在 {@link #sideOf} / {@code frost.FrostArmament} / {@code sun.SunSword} 里。
 *
 * <p>第三行词条是<b>冰霜武具的攻击也会触发太阳剑</b>：剑气 / 锤击 / 长矛 / 箭矢打中的那一下
 * 与武器攻击同一套口径地投一支太阳剑（见 {@code sun.SunSword#onFrostArmamentHit}）。
 *
 * <p><b>图标会跟着换色</b>：左半边焰黄、右半边冰蓝、不在背包里是默认那张。颜色只能由客户端
 * 按物品组件选，所以这里把「在哪半边」写进 {@link #SIDE}（{@code ArtifactBackpack} 在内容变化时
 * 对账、离开背包时清掉），物品模型定义再按它挑贴图（{@code items/eternal_eclipse.json}）。
 */
public class EternalEclipseItem extends ArtifactItem {
	/** 各等级的「冰霜武具的伤害 / 太阳剑伤害」加成（%）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 6.0D, 13.0D, 21.0D, 30.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	/** 神器待在赛菲莉娅背包的哪半边：决定这套「冰火对调」往哪边倒。 */
	public enum Side {
		/** 不在赛菲莉娅背包里（或格子等级把它压失效了）：两套都不改，图标是默认那张。 */
		NONE(0),
		/** 左半边（前三列）：冰霜武具改成火焰属性。 */
		LEFT(1),
		/** 右半边（后三列）：太阳剑改成冰属性。 */
		RIGHT(2);

		private final int id;

		Side(int id) {
			this.id = id;
		}

		/** 组件里存的值（0/1/2）。 */
		public int id() {
			return this.id;
		}

		/** 组件值 → 枚举（认不出来的当「不在背包」）。 */
		public static Side byId(int id) {
			return switch (id) {
				case 1 -> LEFT;
				case 2 -> RIGHT;
				default -> NONE;
			};
		}
	}

	/**
	 * 「在哪半边」写进物品组件：客户端按它挑图标（物品模型定义里的 {@code minecraft:select}
	 * 直接读这个组件，所以不需要任何客户端代码）。
	 *
	 * <p>只有<b>装饰性</b>的用途——玩法判定一律现查背包格子（{@link #sideOf}），
	 * 免得有人用指令给一份带组件的物品就把效果骗到手。
	 */
	public static final DataComponentType<Integer> SIDE = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "eternal_eclipse_side"),
			DataComponentType.<Integer>builder()
					.persistent(Codec.INT)
					.networkSynchronized(ByteBufCodecs.VAR_INT)
					.build());

	public EternalEclipseItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	/** 双连击：太阳剑与冰霜武具各 +1 级。 */
	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.SUN_SWORD, ArtifactCombo.FROST_ARMAMENT);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.eternal_eclipse.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double frostArmamentDamagePercent(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double sunSwordDamagePercentBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		Component value = Component.literal(Numbers.format(this.frostArmamentDamagePercent(level)))
				.withColor(COLOUR_BONUS);

		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.eclipse_sides"),
				Component.translatable("artifact.sephiria_fantasy.affix.frost_armament_damage", value),
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_damage", value),
				Component.translatable("artifact.sephiria_fantasy.affix.eclipse_sun_trigger"));
	}

	// ------------------------------------------------------------------ 在哪半边

	/** 这个格子属于左半边还是右半边（背包 {@value ArtifactBackpack#WIDTH} 格宽，前一半的列算左边）。 */
	public static Side sideOfSlot(int slot) {
		return slot % ArtifactBackpack.WIDTH < ArtifactBackpack.WIDTH / 2 ? Side.LEFT : Side.RIGHT;
	}

	/**
	 * 这件神器现在待在赛菲莉娅背包的哪半边（没带 / 被格子等级压失效 = {@link Side#NONE}）。
	 *
	 * <p>玩法判定（{@code FrostArmament} / {@code SunSword}）走这里现查背包，不看物品组件——
	 * 组件只负责图标颜色。
	 *
	 * <p>带了多份（【唯一】但玩家可能留了副本）时按【唯一】的同一套口径取<b>实际等级最高</b>的那一份
	 * ——等级一样时取格子靠前的那份。图标仍按每份自己所在的列画（所以第二份的颜色是它自己的位置），
	 * 但效果只认最高级那一份，与别的【唯一】神器一致。
	 */
	public static Side sideOf(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		Side best = Side.NONE;
		int bestLevel = -1;

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof EternalEclipseItem)) {
				continue;
			}

			int level = ArtifactEffects.effectiveLevel(stack, backpack.slotLevel(slot));

			if (level >= 0 && level > bestLevel) {
				bestLevel = level;
				best = sideOfSlot(slot);
			}
		}

		return best;
	}

	/**
	 * 对账一遍背包里所有永恒蚀的 {@link #SIDE} 组件（{@code ArtifactBackpack} 每次内容变化时调用）。
	 *
	 * <p>和属性那套一样是「按当前状态对账」而不是「记住上次算出的值」：格子里是哪一件、
	 * 在第几列都现查，所以搬动、交换、石板改格子等级都会跟着更新。
	 */
	public static void stamp(ArtifactBackpack backpack) {
		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (stack.getItem() instanceof EternalEclipseItem) {
				stack.set(SIDE, sideOfSlot(slot).id());
			}
		}
	}

	/** 离开背包的那一份清掉组件（图标回到默认那张）。 */
	public static void clear(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof EternalEclipseItem) {
			stack.remove(SIDE);
		}
	}

	/**
	 * 不在赛菲莉娅背包里（被指令给到手上、创造模式中键克隆出来的一份）时把「在哪半边」清掉。
	 *
	 * <p>背包里的那些不会走到这里——赛菲莉娅背包是独立的容器，只有玩家自己背包里的物品会被 tick；
	 * 这一道只是兜底，免得复制出来的副本带着别人的颜色。
	 */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (stack.has(SIDE)) {
			stack.remove(SIDE);
		}
	}
}
