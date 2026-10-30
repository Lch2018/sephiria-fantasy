package com.sephiria.client;

import com.sephiria.network.SkillSyncPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端手里的技能存储镜像（1.21.1 版：技能 id 用 {@link ResourceLocation}）。
 *
 * <p>技能存储只存在服务端（见 {@code com.sephiria.ability.SkillStorage}），服务端在
 * 数值变化和玩家进服时推 {@link SkillSyncPayload} 过来，HUD 从这份镜像读数。
 */
public final class SkillClientData {
	private static final Map<ResourceLocation, double[]> VALUES = new HashMap<>();

	private SkillClientData() {
	}

	public static void accept(SkillSyncPayload payload) {
		VALUES.put(payload.skill(), new double[]{payload.current(), payload.max()});
	}

	/** 换世界时清空：不清的话上一个世界的专注会让匕首多亮一瞬（见 JOIN 注册处）。 */
	public static void reset() {
		VALUES.clear();
	}

	/** 现有存储量；还没同步过时返回 0。 */
	public static double current(ResourceLocation skill) {
		double[] value = VALUES.get(skill);
		return value == null ? 0.0D : value[0];
	}

	/** 存储总量；还没同步过时返回 0。 */
	public static double max(ResourceLocation skill) {
		double[] value = VALUES.get(skill);
		return value == null ? 0.0D : value[1];
	}
}
