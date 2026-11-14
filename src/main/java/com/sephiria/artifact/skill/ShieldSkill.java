package com.sephiria.artifact.skill;

import com.sephiria.stats.ShieldSystem;
import net.minecraft.server.level.ServerPlayer;

/**
 * 护盾（守护，魔法书）：获得 10 点护盾，持续 6 秒；冷却 20 秒，蓝耗 20/12 MP。
 *
 * <p>护盾本身与持续时间都在 {@link ShieldSystem} 里，技能只负责发盾。
 */
public final class ShieldSkill implements ArtifactSkill {
	public static final ShieldSkill INSTANCE = new ShieldSkill();

	private static final int COOLDOWN_TICKS = 400;
	private static final int DURATION_TICKS = 120;
	private static final double SHIELD_AMOUNT = 10.0D;
	private static final double[] MP_COST_BY_LEVEL = { 20.0D, 12.0D };

	private ShieldSkill() {
	}

	@Override
	public String id() {
		return "shield";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return mpCostAt(level);
	}

	/** 该等级的蓝耗（提示框与结算共用）。 */
	public static double mpCostAt(int level) {
		return MP_COST_BY_LEVEL[Math.max(0, Math.min(MP_COST_BY_LEVEL.length - 1, level))];
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		ShieldSystem.grant(player, SHIELD_AMOUNT, DURATION_TICKS);
	}
}
