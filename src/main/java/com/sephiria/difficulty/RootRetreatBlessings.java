package com.sephiria.difficulty;

import com.sephiria.stats.CombatState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 根之退的祝福（用户 2026-10-21）：
 *
 * <ul>
 *   <li><b>力量</b>（30 阶段，绿色）：战斗状态时获得力量 III，脱战消失；</li>
 *   <li><b>轻盈</b>（20 阶段，绿色）：脱战状态时获得速度 I，脱战消失；摔落伤害 -20%
 *       （摔落那条在 {@code RootRetreatFallMixin}）；</li>
 *   <li><b>勇气</b>（40 阶段，黄色）：玩家 id 变黄（名字颜色在 {@code RootRetreatNameMixin}）。</li>
 * </ul>
 *
 * <p>力量 / 轻盈用<b>短时效果每刻续一次</b>：战斗或脱战状态一变，最多 2 秒就自然消失，不会
 * 永久挂着。玩家自己身上有更强的同名效果时不覆盖。
 */
public final class RootRetreatBlessings {
	/** 每刻续上的效果时长（刻）：状态切换后最多 2 秒消失。 */
	private static final int EFFECT_TICKS = 40;

	private RootRetreatBlessings() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	private static void tick(ServerPlayer player) {
		int stage = RootRetreat.stageOf(player);
		boolean combat = CombatState.isInCombat(player);

		if (stage >= RootRetreat.STRENGTH_STAGE && combat) {
			apply(player, MobEffects.STRENGTH, 2);
		}

		if (stage >= RootRetreat.LIGHTNESS_STAGE && !combat) {
			apply(player, MobEffects.SPEED, 0);
		}
	}

	private static void apply(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
		MobEffectInstance current = player.getEffect(effect);

		// 自己身上有更强的 / 更长的同名效果时不动它（别把玩家自己的药水顶掉）
		if (current != null && current.getAmplifier() > amplifier) {
			return;
		}

		if (current != null && current.getAmplifier() == amplifier && current.getDuration() > EFFECT_TICKS + 20) {
			return;
		}

		player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, true));
	}
}
