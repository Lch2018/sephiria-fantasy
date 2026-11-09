package com.sephiria.stats;

import com.sephiria.difficulty.RootRetreat;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * HP 偷取（俗称吸血）：玩家造成的<b>所有</b>伤害都按 {@code HP偷取 / 1000} 给自己回血。
 *
 * <p>比如 HP 偷取 = 5、打出 100 点伤害，就回 0.5 点生命。回的是「不完全的税前伤害」：
 * 护甲、附魔、玩家防御力、根之退「防护铠甲」以及其它减伤都算进去，<b>唯独把「顽强之躯」
 * 那一层减伤加回来</b>——所以它比最终实扣伤害高，但不是完全没扣减伤的面板值。
 *
 * <p>实现上 AFTER_DAMAGE 只拿得到最终伤害，所以用最终伤害 ÷ 顽强之躯倍率反推；其余减伤都是
 * 乘算，反推后仍保留在结果里。
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

			double base = lifestealBase(player, damageTaken);
			double lifesteal = PlayerStats.lifestealTotal(player);

			if (lifesteal > 0.0D) {
				player.heal((float) (base * lifesteal / PER_POINT));
			}

			// MP 吸收与 HP 偷取同一口径：每点 = 伤害的 0.1%。回蓝要同步 HUD，所以走 gainMp
			double mpAbsorb = PlayerStats.mpAbsorbTotal(player);

			if (mpAbsorb > 0.0D) {
				PlayerStats.gainMp(player, base * mpAbsorb / PER_POINT);
			}
		});
	}

	/**
	 * HP 偷取 / MP 吸收用的伤害基数：把「顽强之躯」那一层加回去，其它减伤全部保留。
	 *
	 * <p>根之退的难度设置挂在攻击者自己身上，所以这里查的是 {@code player}。
	 */
	public static double lifestealBase(ServerPlayer player, float damageTaken) {
		double tenacious = RootRetreat.tenaciousBodyMultiplier(player);
		return tenacious <= 0.0D ? damageTaken : damageTaken / tenacious;
	}

	/** 供以后的神器词条用：某个实体挨打时按比例回血（暂时只有玩家自己吃）。 */
	public static void healFrom(LivingEntity attacker, float damage) {
		if (!(attacker instanceof ServerPlayer player)) {
			return;
		}

		double lifesteal = PlayerStats.lifestealTotal(player);

		if (lifesteal > 0.0D) {
			player.heal((float) (damage * lifesteal / PER_POINT));
		}

		double mpAbsorb = PlayerStats.mpAbsorbTotal(player);

		if (mpAbsorb > 0.0D) {
			PlayerStats.gainMp(player, damage * mpAbsorb / PER_POINT);
		}
	}
}
