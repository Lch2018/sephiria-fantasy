package com.sephiria.artifact;

import com.sephiria.backpack.ArtifactBackpack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 神器与连击的效果汇总：只统计<b>赛菲利亚背包里</b>的神器（放在原版背包里不生效）。
 *
 * <p>规则：
 * <ol>
 *   <li><b>格子等级</b>：实际等级 = 物品自身等级 + 所处格子的等级，为负则这件神器失效；</li>
 *   <li><b>【唯一】</b>：同一件神器只算等级最高的那一个副本，多个不叠加；</li>
 *   <li><b>连击</b>：同连击的「不同种类」神器每种 +1 级，达到阈值给额外增益。</li>
 * </ol>
 */
public final class ArtifactEffects {
	private ArtifactEffects() {
	}

	/** 一件神器在某个格子里的实际等级（物品等级 + 格子等级），可能为负。 */
	public static int effectiveLevel(ItemStack stack, int slotLevel) {
		return ArtifactItem.levelOf(stack) + slotLevel;
	}

	/** 背包里所有生效神器的物理强度加成（含连击的物理档）。 */
	public static double physicalBonus(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		double total = 0.0D;
		Map<Item, Integer> bestPerKind = new HashMap<>();

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			Slot slotData = read(backpack, slot);

			if (slotData.artifact() == null || slotData.inactive()) {
				continue;
			}

			if (slotData.artifact().unique()) {
				bestPerKind.merge(slotData.item(), slotData.level(), Math::max);
			} else {
				total += slotData.artifact().physicalBonus(slotData.level());
			}
		}

		for (Map.Entry<Item, Integer> entry : bestPerKind.entrySet()) {
			total += ((SephiriaArtifact) entry.getKey()).physicalBonus(entry.getValue());
		}

		// 连击的物理档
		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().physicalBonus(entry.getValue());
		}

		return total;
	}

	/** 背包里所有生效神器的攻击速度加成（百分点）。 */
	public static double attackSpeedBonus(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		double total = 0.0D;
		Map<Item, Integer> bestPerKind = new HashMap<>();

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			Slot slotData = read(backpack, slot);

			if (slotData.artifact() == null || slotData.inactive()) {
				continue;
			}

			if (slotData.artifact().unique()) {
				bestPerKind.merge(slotData.item(), slotData.level(), Math::max);
			} else {
				total += slotData.artifact().attackSpeedBonus(slotData.level());
			}
		}

		for (Map.Entry<Item, Integer> entry : bestPerKind.entrySet()) {
			total += ((SephiriaArtifact) entry.getKey()).attackSpeedBonus(entry.getValue());
		}

		return total;
	}

	/** 神器给的「特殊攻击伤害」加成总和（%）。 */
	public static double specialAttackBonus(ServerPlayer player) {
		return sumAffix(player, (artifact, level) -> artifact.specialAttackBonus(level));
	}

	/** 神器给的「近战攻击范围」加成总和（%）。 */
	public static double meleeRangePercent(ServerPlayer player) {
		return sumAffix(player, (artifact, level) -> artifact.meleeRangePercentBonus(level));
	}

	/** 神器给的冲刺存储上限加成（次）。 */
	public static int artifactDashCharges(ServerPlayer player) {
		return (int) sumAffix(player, SephiriaArtifact::dashChargesBonus);
	}

	/** 神器给的冲刺恢复速度加成（%）。 */
	public static double dashRegenPercent(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::dashRegenPercentBonus);
	}

	/** 神器给的「最高元素伤害」加成（点）。 */
	public static double highestElementBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::highestElementBonus);
	}

	/** 连击给的攻击速度加成总和（%）。 */
	public static double comboAttackSpeedPercent(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().attackSpeedPercent(entry.getValue());
		}

		return total;
	}

	/** 连击给的武器伤害加成总和（%）。 */
	public static double weaponDamagePercent(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().weaponDamagePercent(entry.getValue());
		}

		return total;
	}

	/** 冲刺存储上限加成（次）：连击 + 神器。 */
	public static int dashChargeBonus(ServerPlayer player) {
		int total = artifactDashCharges(player);

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().dashCharges(entry.getValue());
		}

		return total;
	}

	/** 一件神器在某个等级下的加成（%）。 */
	@FunctionalInterface
	private interface Affix {
		double at(SephiriaArtifact artifact, int level);
	}

	/** 按【唯一】规则汇总某一种神器词条：同类只取等级最高的那一个副本。 */
	private static double sumAffix(ServerPlayer player, Affix affix) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		double total = 0.0D;
		Map<Item, Integer> bestPerKind = new HashMap<>();

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			Slot slotData = read(backpack, slot);

			if (slotData.artifact() == null || slotData.inactive()) {
				continue;
			}

			if (slotData.artifact().unique()) {
				bestPerKind.merge(slotData.item(), slotData.level(), Math::max);
			} else {
				total += affix.at(slotData.artifact(), slotData.level());
			}
		}

		for (Map.Entry<Item, Integer> entry : bestPerKind.entrySet()) {
			total += affix.at((SephiriaArtifact) entry.getKey(), entry.getValue());
		}

		return total;
	}
	/** 连击的物理伤害增幅（%）：把所有连击当前档位的增幅加起来。 */
	public static double physicalAmpPercent(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().physicalAmpPercent(entry.getValue());
		}

		return total;
	}

	/**
	 * 神器的「物理强度 +x%%」百分比加成（%%）：先把各来源加起来再整体乘上去。
	 *
	 * <p>目前还没有带这种词条的神器（连击给的是物理伤害增幅，不是物理强度百分比），所以恒为 0；
	 * 将来有这类词条时在这里按【唯一】规则汇总即可。
	 */
	public static double physicalPercentBonus(ServerPlayer player) {
		return 0.0D;
	}

	/**
	 * 各连击的等级：同一连击下「不同种类」的神器各记 1 级——同一个神器放多个也只算一次，
	 * 卸下就掉回去。失效（实际等级为负）的神器不计入。
	 *
	 * <p>这个重载不依赖服务端：客户端用菜单同步过来的容器与格子等级自己算一遍，
	 * 连击面板就能显示等级与颜色，不用再多发一个同步包。
	 */
	public static Map<ArtifactCombo, Integer> comboLevels(Container backpack, java.util.function.IntUnaryOperator slotLevels) {
		Map<ArtifactCombo, Set<Item>> kinds = new EnumMap<>(ArtifactCombo.class);

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof SephiriaArtifact artifact)) {
				continue;
			}

			if (effectiveLevel(stack, slotLevels.applyAsInt(slot)) < 0) {
				continue;
			}

			kinds.computeIfAbsent(artifact.combo(), combo -> new HashSet<>()).add(stack.getItem());
		}

		Map<ArtifactCombo, Integer> levels = new EnumMap<>(ArtifactCombo.class);

		for (ArtifactCombo combo : ArtifactCombo.values()) {
			levels.put(combo, kinds.getOrDefault(combo, Set.of()).size());
		}

		return levels;
	}

	/** 服务端版本：直接读玩家的背包。 */
	public static Map<ArtifactCombo, Integer> comboLevels(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		return comboLevels(backpack, backpack::slotLevel);
	}

	private static Slot read(ArtifactBackpack backpack, int slot) {
		ItemStack stack = backpack.getItem(slot);
		return new Slot(stack, effectiveLevel(stack, backpack.slotLevel(slot)));
	}

	/** 一个格子里的东西：物品、神器接口、实际等级（可能为负）。 */
	private record Slot(ItemStack stack, int level) {
		boolean inactive() {
			return this.level < 0;
		}

		SephiriaArtifact artifact() {
			return this.stack.getItem() instanceof SephiriaArtifact artifact ? artifact : null;
		}

		Item item() {
			return this.stack.getItem();
		}
	}
}
