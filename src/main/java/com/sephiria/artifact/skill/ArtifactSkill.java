package com.sephiria.artifact.skill;

import net.minecraft.server.level.ServerPlayer;

/**
 * 神器技能：由「发动型神器」与「获得技能」两种词条给出的主动效果。
 *
 * <p>技能由神器物品自己提供（{@link com.sephiria.artifact.SephiriaArtifact#skill()}），
 * 数值随神器等级变化——所以同一件神器带两件（没有【唯一】时）在技能页里会各占一条、数值不同。
 *
 * <p>冷却与蓝耗由 {@link ArtifactSkills} 统一处理，{@link #cast} 只负责效果本身。
 */
public interface ArtifactSkill {
	/**
	 * 技能攻击 / 锁定距离的统一倍率：用户 2026-11-15 要求所有发动型神器与魔法书技能
	 * 的射程 / 锁定距离提高到原来的 150%。只乘「能打多远 / 能锁多远」，不乘 AOE 半径。
	 */
	double RANGE_MULTIPLIER = 1.5D;

	/** 技能 id：用来反查（技能栏里存的就是它）与拼名字键 {@code sephiria.artifact_skill.<id>}。 */
	String id();

	/** 冷却（tick）。 */
	int cooldownTicks();

	/** 蓝耗（点）：按神器等级算，0 表示不耗蓝（发动型神器不耗蓝）。 */
	double mpCost(int level);

	/** 实际冷却（tick）：默认就是基础值；满果实水桶这类会按属性现算的技能覆盖它。 */
	default int cooldownTicks(ServerPlayer player, int level) {
		return cooldownTicks();
	}

	/** 实际蓝耗：默认按等级；水之精灵这类按最大 MP 百分比的技能覆盖它。 */
	default double mpCost(ServerPlayer player, int level) {
		return mpCost(level);
	}

	/** 施放前的额外条件（需要看准星目标等）；默认没有。 */
	default boolean canCast(ServerPlayer player, int level) {
		return true;
	}

	/** 释放（服务端）。调用前已经检查过冷却与蓝量。 */
	void cast(ServerPlayer player, int level);
}
