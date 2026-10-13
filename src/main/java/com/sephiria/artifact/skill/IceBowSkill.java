package com.sephiria.artifact.skill;

import com.sephiria.frost.FrostArmament;
import net.minecraft.server.level.ServerPlayer;

/**
 * 冰弓（冰霜武具的「发动型神器」）：向准星方向发射所有储存的箭矢。
 *
 * <p>真正的限制是<b>弹药</b>而不是冷却或蓝耗：弹药在 {@code frost.FrostArmament} 里
 * 每 0.8 秒自动装填 1 发（最多 7），发动时一次打空。所以：
 * ① 蓝耗 0——发动型神器不耗蓝（见 {@link ArtifactSkill#mpCost} 的约定）；
 * ② 冷却只有 {@value #COOLDOWN_TICKS} 刻（0.5 秒），纯粹是防止一只手在同一个 tick 里连按两次；
 * ③ 没弹药时 {@link FrostArmament#castIceBow} 什么都不做（也不会进冷却——见它的注释）。
 */
public final class IceBowSkill implements ArtifactSkill {
	public static final IceBowSkill INSTANCE = new IceBowSkill();

	/** 冷却 0.5 秒：只是防连按，弹药才是真限制。 */
	private static final int COOLDOWN_TICKS = 10;

	private IceBowSkill() {
	}

	@Override
	public String id() {
		return "ice_bow";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return 0.0D;
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		FrostArmament.castIceBow(player, level);
	}
}
