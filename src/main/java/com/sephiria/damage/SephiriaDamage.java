package com.sephiria.damage;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.stats.PseudoRandom;
import com.sephiria.stats.PlayerStats;
import com.sephiria.weapon.SephiriaShieldItem;
import com.sephiria.weapon.SephiriaStaffItem;
import com.sephiria.weapon.SephiriaWeapon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 判定"这发伤害来自 SEPHIRIA"，以及"目标是否该无视无敌帧"。
 *
 * <p>判定来源不用自定义伤害类型，而是看两件事，这样以后新增武器或神器都不用改判定逻辑：
 * <ul>
 *   <li><b>投掷物</b>：伤害的直接来源实体实现了 {@link Source}（模组自己的箭矢等）；</li>
 *   <li><b>近战与技能</b>：伤害的攻击者手里拿着实现了 {@link SephiriaWeapon} 的物品。</li>
 * </ul>
 *
 * <p>此外这里还挂着「暴击」与「无视防御伤害」两件事——它们都必须发生在服务端伤害入口
 * （见 {@code DamageReductionMixin}），因为只有那里才拿得到"这一下打出去多少"。
 *
 * <p>目标是否无视无敌帧：玩家（PVP）与对玩家有威胁的生物（原版 {@link Enemy} 标记，僵尸、骷髅、
 * 苦力怕等都实现它）无视；牛羊猪、村民这类友好生物保留无敌帧。注意这是"每个目标各自判定"，
 * 所以一次范围攻击里友好生物照样受无敌帧保护。
 */
public final class SephiriaDamage {
	/** 模组投掷物实现的标记：用它就能认出"这是赛菲利亚的伤害"。 */
	public interface Source {
	}

	/** 真实伤害（无视防御伤害）的伤害类型：数据包里带 bypasses_armor 等标签。 */
	public static final ResourceKey<DamageType> TRUE_DAMAGE = typeKey("true_damage");
	/** 神器自己造成的伤害（红色露水的暴击溅射等）：吃暴击，但吃不到「武器攻击的暴击几率」。 */
	public static final ResourceKey<DamageType> ARTIFACT_DAMAGE = typeKey("artifact_damage");
	/** 红色露水那类暴击溅射的范围（格）：以被暴击的目标为原点。 */
	private static final double SPLASH_RANGE = 3.0D;

	private SephiriaDamage() {
	}

	/** 伤害的种类：决定吃哪一套倍率与暴击几率。 */
	public enum Kind {
		/** 不是玩家造成的伤害。 */
		OTHER,
		/** 武器打出的伤害：普通攻击、横扫、弩矢、武器技能。 */
		WEAPON,
		/** 神器自己造成的伤害：吃暴击几率，但吃不到「武器攻击的暴击几率」。 */
		ARTIFACT,
		/** 无视防御伤害：不再暴击，也不会再触发一次真伤。 */
		TRUE
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

	/** 这发伤害属于哪一类。 */
	public static Kind kindOf(DamageSource source) {
		if (source.is(TRUE_DAMAGE)) {
			return Kind.TRUE;
		}

		if (source.is(ARTIFACT_DAMAGE)) {
			return Kind.ARTIFACT;
		}

		if (source.getDirectEntity() instanceof Source) {
			return Kind.WEAPON;
		}

		return source.getEntity() instanceof LivingEntity attacker
				&& (attacker.getMainHandItem().getItem() instanceof SephiriaWeapon
						|| attacker.getOffhandItem().getItem() instanceof SephiriaWeapon)
								? Kind.WEAPON
								: Kind.OTHER;
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

	// ------------------------------------------------------------------ 暴击

	/**
	 * 暴击判定：命中就按「暴击伤害」放大这一下的数值。
	 *
	 * <p>只有玩家造成的伤害会暴击，真实伤害不参与（它本来就是额外那一下）。
	 * 「武器攻击的暴击几率」只加在武器打出的伤害上——神器自己造成的伤害只用通用暴击几率。
	 * 武器攻击暴击时还会触发红色露水那类「暴击溅射」。
	 */
	public static float applyCrit(LivingEntity target, DamageSource source, float amount) {
		Kind kind = kindOf(source);

		if (kind == Kind.OTHER || kind == Kind.TRUE || !(source.getEntity() instanceof ServerPlayer player)) {
			return amount;
		}

		double chance = kind == Kind.WEAPON
				? PlayerStats.weaponCritChanceTotal(player)
				: PlayerStats.critChanceTotal(player);

		if (!PseudoRandom.crit(player, chance)) {
			return amount;
		}

		if (kind == Kind.WEAPON) {
			critSplash(target, player);
		}

		return (float) (amount * PlayerStats.critDamageTotal(player) / 100.0D);
	}

	/**
	 * 红色露水：暴击时对周围敌人造成「最高属性值 × 比例」的物理伤害，以被暴击的目标为原点。
	 *
	 * <p>它吃「物理伤害增幅」（和物理伤害一样），但吃不到普攻/技能那两套倍率——它是神器伤害，
	 * 神器伤害的加成还没做，所以这里只有增幅这一项。
	 */
	private static void critSplash(LivingEntity target, ServerPlayer player) {
		double percent = ArtifactEffects.critSplashPercent(player);

		if (percent <= 0.0D || !(target.level() instanceof ServerLevel level)) {
			return;
		}

		double highest = 0.0D;

		for (PlayerStats.Element element : PlayerStats.Element.values()) {
			highest = Math.max(highest, PlayerStats.elementTotal(player, element));
		}

		float damage = (float) (highest * percent / 100.0D
				* (1.0D + PlayerStats.physicalAmpPercent(player) / 100.0D));
		AABB area = target.getBoundingBox().inflate(SPLASH_RANGE, SPLASH_RANGE, SPLASH_RANGE);
		DamageSource splash = artifactDamage(level, player);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim == target || victim.isAlliedTo(player)) {
				continue;
			}

			victim.hurtServer(level, splash, damage);
		}
	}

	// ------------------------------------------------------------------ 无视防御伤害（真实伤害）

	/**
	 * 额外那一下真实伤害：由 {@code DamageReductionMixin} 在伤害结算<b>之后</b>调用。
	 *
	 * <p>单独打一次是因为它要绕开护甲、韧性、附魔保护与本模组的减伤——这些都作用在
	 * 「那一下」的数值上，只有换一种伤害类型重新打一次才能真正绕开。它会按该次攻击的倍率放大：
	 * 普通攻击吃普攻那一套，技能吃技能那一套（技能结算前用 {@link #markSkill} 挂上倍率）。
	 */
	public static void applyTrueDamage(LivingEntity target, DamageSource source, float amount) {
		Kind kind = kindOf(source);

		if ((kind != Kind.WEAPON && kind != Kind.ARTIFACT)
				|| !(source.getEntity() instanceof ServerPlayer player)
				|| !(target.level() instanceof ServerLevel level)) {
			return;
		}

		double flat = PlayerStats.ignoreDefenseTotal(player);

		if (flat <= 0.0D || target.isDeadOrDying()) {
			return;
		}

		double multiplier = kind == Kind.WEAPON ? attackMultiplier(player) : 1.0D;
		float extra = (float) (flat * multiplier);

		// 真实伤害不吃无敌帧，否则高速武器下它会经常被上一次的帧吞掉
		target.invulnerableTime = 0;
		target.hurtServer(level, trueDamage(level, player), extra);
	}

	// ------------------------------------------------------------------ 技能倍率的标记

	/** 技能在结算前挂上的倍率：只在挂上的那一 tick 内有效（一次技能打多个目标时都算数）。 */
	private static final Map<UUID, Mark> SKILL_MARK = new HashMap<>();

	private record Mark(long tick, double multiplier) {
	}

	/**
	 * 技能在调用 {@code hurtServer} 之前调一次，把这次技能的倍率告诉伤害入口。
	 *
	 * <p>技能与普通攻击用的是同一个伤害类型（{@code playerAttack}），光看伤害来源分不出两者，
	 * 而「无视防御伤害」要按攻击方式放大，所以由技能自己报一下。
	 */
	public static void markSkill(ServerPlayer player, double multiplier) {
		SKILL_MARK.put(player.getUUID(), new Mark(player.level().getGameTime(), multiplier));
	}

	/** 玩家退出时清掉标记。 */
	public static void forget(ServerPlayer player) {
		SKILL_MARK.remove(player.getUUID());
	}

	/** 本 tick 有技能标记就用技能倍率，否则按普通攻击倍率算。 */
	private static double attackMultiplier(ServerPlayer player) {
		Mark mark = SKILL_MARK.get(player.getUUID());

		if (mark == null) {
			return PlayerStats.normalAttackMultiplier(player);
		}

		if (mark.tick() != player.level().getGameTime()) {
			// 上一 tick 的标记已经过期，顺手清掉免得越积越多
			SKILL_MARK.remove(player.getUUID());
			return PlayerStats.normalAttackMultiplier(player);
		}

		return mark.multiplier();
	}

	// ------------------------------------------------------------------ 伤害类型

	/** 造一个真实伤害的伤害来源（数据包里的 sephiria:true_damage）。 */
	public static DamageSource trueDamage(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, TRUE_DAMAGE), player, player);
	}

	/** 造一个神器伤害的伤害来源（数据包里的 sephiria:artifact_damage）。 */
	public static DamageSource artifactDamage(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, ARTIFACT_DAMAGE), player, player);
	}

	private static Holder<DamageType> lookup(ServerLevel level, ResourceKey<DamageType> key) {
		return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
	}

	private static ResourceKey<DamageType> typeKey(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name));
	}
}
