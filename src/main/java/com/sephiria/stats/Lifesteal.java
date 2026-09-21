package com.sephiria.stats;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * HP 偷取（俗称吸血）：玩家造成的<b>所有</b>伤害都按 {@code HP偷取 / 1000} 给自己回血。
 *
 * <p>比如 HP 偷取 = 5、打出 100 点伤害，就回 0.5 点生命。回的是「实际造成的伤害」——
 * 也就是扣完护甲、附魔、减伤之后真正掉了多少血，而不是伤害表上的数字。
 *
 * <p>挂在 {@code AFTER_DAMAGE} 上（伤害已经结算完），这样无敌帧、减伤之类的规则都先跑完，
 * 吸血只看最后真正打出来的那一下。
 */
public final class Lifesteal {
	private static final double PER_POINT = 1000.0D;

	private Lifesteal() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player)) {
				return;
			}

			double lifesteal = PlayerStats.lifestealTotal(player);

			if (lifesteal <= 0.0D) {
				return;
			}

			player.heal((float) (damageTaken * lifesteal / PER_POINT));
		});
	}

	/** 供以后的神器词条用：某个实体挨打时按比例回血（暂时只有玩家自己吃）。 */
	public static void healFrom(LivingEntity attacker, float damage) {
		if (attacker instanceof ServerPlayer player && PlayerStats.lifestealTotal(player) > 0.0D) {
			player.heal((float) (damage * PlayerStats.lifestealTotal(player) / PER_POINT));
		}
	}
}
