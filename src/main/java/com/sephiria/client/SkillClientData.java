package com.sephiria.client;

import com.sephiria.network.SkillSyncPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端手里的技能存储镜像。
 *
 * <p>技能存储只存在服务端（见 {@code com.sephiria.ability.SkillStorage}），服务端在
 * 数值变化和玩家进服时推 {@link SkillSyncPayload} 过来，HUD 从这份镜像读数。
 */
public final class SkillClientData {
	private static final Map<Identifier, double[]> VALUES = new HashMap<>();

	private SkillClientData() {
	}

	public static void accept(SkillSyncPayload payload) {
		VALUES.put(payload.skill(), new double[]{payload.current(), payload.max()});
	}

	/** 现有存储量；还没同步过时返回 0。 */
	public static double current(Identifier skill) {
		double[] value = VALUES.get(skill);
		return value == null ? 0.0D : value[0];
	}

	/** 存储总量；还没同步过时返回 0。 */
	public static double max(Identifier skill) {
		double[] value = VALUES.get(skill);
		return value == null ? 0.0D : value[1];
	}
}
