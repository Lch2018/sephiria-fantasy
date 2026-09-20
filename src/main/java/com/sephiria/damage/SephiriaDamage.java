package com.sephiria.damage;

import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * 判定"这发伤害来自 SEPHIRIA"，以及"目标是否该无视无敌帧"。
 *
 * <p>判定来源不用自定义伤害类型，而是看两件事，这样以后新增武器或神器都不用改判定逻辑：
 * <ul>
 *   <li><b>投掷物</b>：伤害的直接来源实体实现了 {@link Source}（模组自己的箭矢等）；</li>
 *   <li><b>近战与技能</b>：伤害的攻击者手里拿着实现了 {@link SephiriaWeapon} 的物品。</li>
 * </ul>
 *
 * <p>目标是否无视无敌帧：玩家（PVP）与对玩家有威胁的生物（原版 {@link Enemy} 标记，僵尸、骷髅、
 * 苦力怕等都实现它）无视；牛羊猪、村民这类友好生物保留无敌帧。注意这是"每个目标各自判定"，
 * 所以一次范围攻击里友好生物照样受无敌帧保护。
 */
public final class SephiriaDamage {
	/** 模组投掷物实现的标记：用它就能认出"这是赛菲利亚的伤害"。 */
	public interface Source {
	}

	private SephiriaDamage() {
	}

	public static boolean fromSephiria(DamageSource source) {
		if (source.getDirectEntity() instanceof Source) {
			return true;
		}

		if (source.getEntity() instanceof LivingEntity attacker) {
			return attacker.getMainHandItem().getItem() instanceof SephiriaWeapon
					|| attacker.getOffhandItem().getItem() instanceof SephiriaWeapon;
		}

		return false;
	}

	/**
	 * 最终的受伤倍率：把所有减伤来源乘起来（1.0 表示不减伤）。
	 *
	 * <p>目前两个来源——长棍「回击」后的那段减伤窗口，以及盾剑按住右键的「防御」。
	 * 由 {@code DamageReductionMixin} 在服务端伤害入口使用。
	 */
	public static float damageMultiplier(LivingEntity entity) {
		return SephiriaStaffItem.damageMultiplier(entity) * SephiriaShieldItem.damageMultiplier(entity);
	}

	/**
	 * 该目标是否要无视无敌帧。
	 *
	 * <p>三种情况互相独立、都算数：
	 * <ol>
	 *   <li>玩家——PVP；</li>
	 *   <li>敌对生物（原版 {@link Enemy} 标记：僵尸、骷髅、苦力怕、蜘蛛、末影人等）；</li>
	 *   <li>当前正在攻击玩家的中立生物——狼、北极熊、蜜蜂这类平时中立、被打才反击的生物，
	 *       只要此刻的仇恨目标是玩家，就按威胁处理。</li>
	 * </ol>
	 *
	 * <p>判定逐个目标进行，所以一次范围攻击裹进友好生物时，它们照样保留无敌帧。
	 */
	public static boolean ignoresInvulnerableFrames(Entity target) {
		if (target instanceof Player || target instanceof Enemy) {
			return true;
		}

		return target instanceof Mob mob && mob.getTarget() instanceof Player;
	}
}
