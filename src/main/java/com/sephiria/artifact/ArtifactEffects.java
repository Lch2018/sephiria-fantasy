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
 *   <li><b>连击</b>：背包里每件生效的神器各 +1 级，带【唯一】的同种神器只算一次
 *       （三本没有【唯一】的盾牌术教材 = 坚固 +3），达到阈值给额外增益。</li>
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

	/** 神器给的暴击几率加成总和（%）：任何由玩家造成的伤害都吃。 */
	public static double critChanceBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::critChanceBonus);
	}

	/** 神器给的「武器攻击的暴击几率」加成总和（%）：只有武器打出的伤害吃得到。 */
	public static double weaponCritChanceBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::weaponCritChanceBonus);
	}

	/** 神器给的暴击伤害加成总和（%），加在默认的 150% 上。 */
	public static double critDamageBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::critDamageBonus);
	}

	/** 神器给的「无视防御伤害」总和（点）。 */
	public static double ignoreDefenseBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::ignoreDefenseBonus);
	}

	/** 神器给的「普通攻击伤害」加成总和（%）：只加成普通攻击。 */
	public static double normalAttackDamagePercent(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::normalAttackDamagePercentBonus);
	}

	/** 神器给的移动速度加成总和（%）。 */
	public static double moveSpeedPercent(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::moveSpeedPercentBonus);
	}

	/** 神器给的闪避总和（点）。 */
	public static double dodgeBonus(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::dodgeBonus);
	}

	/** 闪避触发时恢复的冲刺次数（弹力带）。 */
	public static double dodgeRestoreCharges(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::dodgeRestoreDashCharges);
	}

	/** 连击给的闪避加成总和（点）。 */
	public static double comboDodgeBonus(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().dodgeBonus(entry.getValue());
		}

		return total;
	}

	/** 暴击溅射比例（%）：0 表示没有这个词条。 */
	public static double critSplashPercent(ServerPlayer player) {
		return sumAffix(player, SephiriaArtifact::critSplashPercent);
	}

	/** 连击给的暴击几率加成总和（%）。 */
	public static double comboCritChancePercent(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().critChancePercent(entry.getValue());
		}

		return total;
	}

	/** 连击给的暴击伤害加成总和（%）。 */
	public static double comboCritDamagePercent(ServerPlayer player) {
		double total = 0.0D;

		for (Map.Entry<ArtifactCombo, Integer> entry : comboLevels(player).entrySet()) {
			total += entry.getKey().critDamagePercent(entry.getValue());
		}

		return total;
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
	 * 各连击的等级：背包里每件生效的神器各记 1 级，带【唯一】的同种神器只算一次
	 * （三本盾牌术教材没有【唯一】→ 坚固 +3；三个力量护符有【唯一】→ 坚固 +1）。
	 * 失效（实际等级为负）的神器不计入。
	 *
	 * <p>这个重载不依赖服务端：客户端用菜单同步过来的容器与格子等级自己算一遍，
	 * 连击面板就能显示等级与颜色，不用再多发一个同步包。
	 */
	public static Map<ArtifactCombo, Integer> comboLevels(Container backpack, java.util.function.IntUnaryOperator slotLevels) {
		Map<ArtifactCombo, Integer> levels = new EnumMap<>(ArtifactCombo.class);
		Set<Item> uniqueCounted = new HashSet<>();

		for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
			ItemStack stack = backpack.getItem(slot);

			if (!(stack.getItem() instanceof SephiriaArtifact artifact)) {
				continue;
			}

			if (effectiveLevel(stack, slotLevels.applyAsInt(slot)) < 0) {
				continue;
			}

			// 【唯一】的同种神器只记一次；没有【唯一】的每个副本都记，所以带三本就是 +3 级
			if (artifact.unique() && !uniqueCounted.add(stack.getItem())) {
				continue;
			}

			// 双连击神器：两种连击各记 1 级
			for (ArtifactCombo combo : artifact.combos()) {
				levels.merge(combo, 1, Integer::sum);
			}
		}

		// 一件都没有的连击也要留一项（连击面板要显示 0 级）
		for (ArtifactCombo combo : ArtifactCombo.values()) {
			levels.putIfAbsent(combo, 0);
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
