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
	/** 技能 id：用来反查（技能栏里存的就是它）与拼名字键 {@code sephiria.artifact_skill.<id>}。 */
	String id();

	/** 冷却（tick）。 */
	int cooldownTicks();

	/** 蓝耗（点）：按神器等级算，0 表示不耗蓝（发动型神器不耗蓝）。 */
	double mpCost(int level);

	/** 带玩家上下文的实际蓝耗；默认沿用固定值，水之精灵这类按最大 MP 比例扣蓝的会重写。 */
	default double mpCost(ServerPlayer player, int level) {
		return mpCost(level);
	}

	/** 带玩家上下文的实际冷却；默认沿用固定值，饱满果实桶这类吃 MP 再生的会重写。 */
	default int cooldownTicks(ServerPlayer player, int level) {
		return cooldownTicks();
	}

	/**
	 * 释放前的额外条件（默认恒真）。
	 *
	 * <p>箭雨需要准星在 24 格内指到方块或实体；不满足时不应该扣蓝 / 进冷却。
	 */
	default boolean canCast(ServerPlayer player, int level) {
		return true;
	}

	/** 释放（服务端）。调用前已经检查过冷却、蓝量与 {@link #canCast}。 */
	void cast(ServerPlayer player, int level);
}
