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
	/** 触电减益自己结算的闪电属性伤害（数据包里 sephiria:electric_shock）：它是「减益伤害」，
	 * 不再触发电击之触（防链式自触），只由 {@code Debuffs#activate} 使用。 */
	public static final ResourceKey<DamageType> ELECTRIC_SHOCK = typeKey("electric_shock");
	/** 攻击打出的电属性伤害（乌云雷击 / 雷之裁决光束 / 桑德耳环 / 树枝与台风的附加闪电）：
	 * 与触电结算分开，事件守门才分得清「哪些电属性伤害能触发电击之触」——这些都能。 */
	public static final ResourceKey<DamageType> ELECTRIC_ATTACK = typeKey("electric_attack");
	/** 太阳剑投掷那一下的火属性伤害（数据包里 sephiria:sun_sword）：连击效果自己的伤害，
	 * 既不是武器攻击也不是魔法书，所以不触发别的「造成武器/神器伤害时」效果，也不会自我链式。 */
	public static final ResourceKey<DamageType> SUN_SWORD = typeKey("sun_sword");
	/** 灼伤减益自己结算的火属性伤害（数据包里 sephiria:burn）：它是「减益伤害」，
	 * 不再触发火焰之触（防链式自触），只由 {@code Debuffs#activateBurn} 使用。 */
	public static final ResourceKey<DamageType> BURN = typeKey("burn");
	/** 攻击打出的火属性伤害（数据包里 sephiria:fire_attack：红蛇之眼的陨石）：
	 * 与 sun_sword 分开，因为它照 Electric Attack 的口径算「攻击打出的火属性伤害」——
	 * 事件守门按它触发火焰之触。 */
	public static final ResourceKey<DamageType> FIRE_ATTACK = typeKey("fire_attack");
	/** 红色露水那类暴击溅射的范围（格）：以被暴击的目标为原点。 */
	private static final double SPLASH_RANGE = 3.0D;
	/** 黄金之手：每持有这么多叶子升一档。 */
	private static final double GOLDEN_HANDS_STEP_LEAVES = 200.0D;
	/** 黄金之手：每一档的增伤（%）。 */
	private static final double GOLDEN_HANDS_STEP_PERCENT = 1.0D;
	/** 黄金之手：增伤上限（%）。 */
	private static final double GOLDEN_HANDS_MAX_PERCENT = 20.0D;

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
		/**
		 * 无视防御伤害：绕开护甲与本模组减伤，但和别的伤害一样吃暴击与黄金之手（2026-10 起）；
		 * 它自己不会再触发一次真伤。
		 */
		TRUE,
		/** 电属性伤害（触电结算与攻击打出的都是）：数值各自定死，不吃倍率；
		 * 事件守门只把触电自己的结算（electric_shock）挡在电击之触之外（防链式自触），
		 * 攻击打出的（electric_attack）照常触发。 */
		ELECTRIC,
		/**
		 * 连击效果自己的伤害（太阳剑）：吃通用暴击与黄金之手，但既不算武器也不算神器攻击——
		 * 不触发电击之触 / 树枝 / 台风那类「造成武器或神器伤害时」的效果，也就不会链式自触。
		 */
		SUN,
		/**
		 * 灼伤减益自己结算的火属性伤害（sephiria:burn）：与触电一样属于「减益伤害」——
		 * 数值定死、不吃减伤与黄金之手、也不触发任何「造成伤害时」的效果；
		 * 暴击照吃（2026-10 起减益伤害一律走通用暴击几率，见 {@link #applyCrit}）。
		 */
		FIRE,
		/**
		 * 攻击打出的火属性伤害（sephiria:fire_attack：红蛇之眼的陨石）。
		 *
		 * <p>与电的 {@link #ELECTRIC} 对称：吃通用暴击、减伤与黄金之手（数值口径与太阳剑那一档相同），
		 * 但既不算武器也不算神器伤害——只用来触发火焰之触，不牵动树枝 / 台风 / 太阳剑那些口径。
		 */
		FIRE_ATTACK
	}

	public static boolean fromSephiria(DamageSource source) {
		// 「本模组造成的伤害」= kindOf 认得出来的那几类：模组的伤害类型（artifact_damage /
		// sun_sword / burn / electric_* / fire_attack / true_damage）、实现 Source 标记的投掷物，
		// 以及拿着本模组武器打人的那一下。其余一律 OTHER（原版武器、环境伤害……）。
		return kindOf(source) != Kind.OTHER;
	}

	/** 这发伤害属于哪一类。 */
	public static Kind kindOf(DamageSource source) {
		if (source.is(TRUE_DAMAGE)) {
			return Kind.TRUE;
		}

		if (source.is(ARTIFACT_DAMAGE)) {
			return Kind.ARTIFACT;
		}

		if (source.is(ELECTRIC_SHOCK) || source.is(ELECTRIC_ATTACK)) {
			return Kind.ELECTRIC;
		}

		if (source.is(SUN_SWORD)) {
			return Kind.SUN;
		}

		if (source.is(BURN)) {
			return Kind.FIRE;
		}

		if (source.is(FIRE_ATTACK)) {
			return Kind.FIRE_ATTACK;
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
	 * <p><b>本模组打出的伤害都吃通用暴击几率</b>（2026-10 起：减益伤害与真实伤害也算进来）。
	 * 在这条之上还有两笔专项加成：「武器攻击的暴击几率」只加在武器打出的那几路
	 * （普通攻击、横扫、弩矢、武器技能），「电属性攻击的暴击几率」（麒麟的角）只加在电属性那几路
	 * （触电结算、附加闪电、闪电攻击）——是<b>相加</b>，不是各掷一套；真实伤害与减益伤害只吃通用那一项。
	 * 唯一不掷暴击的是「不是玩家造成的伤害」。
	 * 武器攻击暴击时还会触发红色露水那类「暴击溅射」。
	 */
	public static float applyCrit(LivingEntity target, DamageSource source, float amount) {
		Kind kind = kindOf(source);

		if (kind == Kind.OTHER || !(source.getEntity() instanceof ServerPlayer player)) {
			return amount;
		}

		double chance = switch (kind) {
			case WEAPON -> PlayerStats.weaponCritChanceTotal(player);
			case ELECTRIC -> PlayerStats.critChanceTotal(player) + PlayerStats.electricCritChanceTotal(player);
			// 太阳剑那一下：通用之上再加「太阳剑暴击几率」（索利斯·德克里）
			case SUN -> PlayerStats.critChanceTotal(player) + ArtifactEffects.sunSwordCritChanceBonus(player);
			default -> PlayerStats.critChanceTotal(player);
		};

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
			// 被暴击的那个目标自己已经挨过本击，溅射只打周围；和平生物不误伤
			if (victim == target || !strikeable(victim, player)) {
				continue;
			}

			victim.hurtServer(level, splash, damage);
		}
	}

	/**
	 * 范围伤害能不能打这个目标：只打「有威胁的生物」（敌对生物、正在攻击玩家的中立生物），
	 * <b>玩家整个不在目标池里</b>。
	 *
	 * <p>神器的自动攻击（乌云雷击、桑德耳环闪电、雷之裁决光束、露水溅射）与武器技能的范围大，
	 * 扫到谁算谁——打玩家就是误伤，所以连击、神器、神器技能、武器技能都不以玩家为目标；
	 * 普通攻击与横扫是玩家自己瞄准的，不在此列（普攻 PVP 照旧）。
	 * 判定逐个目标进行，所以一次范围攻击裹进友好生物时，它们照样保留无敌帧。
	 */
	public static boolean strikeable(LivingEntity victim, ServerPlayer attacker) {
		// 玩家一律不打（这一条也涵盖 victim != attacker：攻击者自己就是玩家）
		return !(victim instanceof Player) && victim.isAlive() && !victim.isAlliedTo(attacker)
				&& ignoresInvulnerableFrames(victim);
	}

	// ------------------------------------------------------------------ 黄金之手

	/**
	 * 谈判连击的「黄金之手」：每持有 200 叶子，这次攻击的伤害 +1%，最多 +20%。
	 *
	 * <p>攻击方的增益，由 {@code DamageReductionMixin} 在暴击与减伤算完之后调用。
	 * 真实伤害也吃它（2026-10 起：它虽然绕开减伤，但仍是一次正常打出的伤害），
	 * 不参与的只有非玩家造成的伤害与减益伤害（触电 / 灼伤）——减益是「数值定死」的那一类。
	 */
	public static float applyGoldenHands(DamageSource source, float amount) {
		Kind kind = kindOf(source);

		if (kind == Kind.OTHER || kind == Kind.ELECTRIC || kind == Kind.FIRE
				|| !(source.getEntity() instanceof ServerPlayer player)) {
			return amount;
		}

		if (!ArtifactEffects.comboGoldenHands(player)) {
			return amount;
		}

		int steps = (int) Math.min(Math.floor(PlayerStats.leaves(player) / GOLDEN_HANDS_STEP_LEAVES),
				GOLDEN_HANDS_MAX_PERCENT / GOLDEN_HANDS_STEP_PERCENT);

		return (float) (amount * (1.0D + steps * GOLDEN_HANDS_STEP_PERCENT / 100.0D));
	}

	// ------------------------------------------------------------------ 无视防御伤害（真实伤害）

	/**
	 * 额外那一下真实伤害：由 {@code DamageReductionMixin} 在伤害结算<b>之后</b>调用。
	 *
	 * <p>单独打一次是因为它要绕开护甲、韧性、附魔保护与本模组的减伤——这些都作用在
	 * 「那一下」的数值上，只有换一种伤害类型重新打一次才能真正绕开。绕开的只有这些：
	 * <b>增伤照吃</b>——按该次攻击的倍率放大（普通攻击吃普攻那一套，技能吃技能那一套，
	 * 技能结算前用 {@link #markSkill} 挂上倍率），并在打出时照掷通用暴击、照吃黄金之手
	 * （2026-10 起；它是独立的一下，所以自己掷，不跟着主伤害那次的结果）。
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
		target.setInvulnerableTime(0);
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

	/**
	 * 造一个触电结算的闪电属性伤害来源（数据包里的 sephiria:electric_shock）。
	 *
	 * <p>只给触电减益自己的周期/终结跳用：它是减益伤害，事件守门按类型把它挡在
	 * 电击之触之外（防链式自触）。攻击打出的电属性伤害走 {@link #electricAttack}。
	 *
	 * <p>触发者掉线后 {@code player} 传 null：伤害照样结算，只是没有归属（不触发吸血、不计击杀功劳）。
	 */
	public static DamageSource electricShock(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, ELECTRIC_SHOCK), player, player);
	}

	/**
	 * 造一个「攻击打出的电属性伤害」来源（数据包里的 sephiria:electric_attack）。
	 *
	 * <p>乌云雷击、雷之裁决光束、桑德耳环、树枝 / 台风的附加闪电都用它——它们是电属性攻击，
	 * 与武器 / 神器伤害一样能触发电击之触（守门见 {@code Debuffs#register}）。
	 * 伤害数值口径与 {@link #electricShock} 一致：吃电属性暴击，不吃减伤与黄金之手。
	 */
	public static DamageSource electricAttack(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, ELECTRIC_ATTACK), player, player);
	}

	/**
	 * 造一个太阳剑的火属性伤害来源（数据包里的 sephiria:sun_sword）。
	 *
	 * <p>连击效果自己的伤害：吃通用暴击与黄金之手，但不触发「造成武器/神器伤害时」那一串
	 * （电击之触、树枝的附加闪电……）——见 {@link Kind#SUN}。
	 */
	public static DamageSource sunSword(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, SUN_SWORD), player, player);
	}

	/**
	 * 造一个灼伤结算的火属性伤害来源（数据包里的 sephiria:burn）。
	 *
	 * <p>只给灼伤减益的每跳用：它是减益伤害（{@link Kind#FIRE}），吃通用暴击（与神器伤害同一项），
	 * 但不吃减伤与黄金之手，也不会再触发火焰之触（防链式自触）。
	 *
	 * <p>触发者掉线后 {@code player} 传 null：伤害照样结算，只是没有归属（不计击杀功劳）。
	 */
	public static DamageSource burn(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, BURN), player, player);
	}

	/**
	 * 造一个「攻击打出的火属性伤害」来源（数据包里的 sephiria:fire_attack）。
	 *
	 * <p>红蛇之眼的陨石用它：与 {@link #electricAttack} 对称——照常触发火焰之触（守门见
	 * {@code Debuffs#register}），数值吃通用暴击、减伤与黄金之手。
	 */
	public static DamageSource fireAttack(ServerLevel level, ServerPlayer player) {
		return new DamageSource(lookup(level, FIRE_ATTACK), player, player);
	}

	private static Holder<DamageType> lookup(ServerLevel level, ResourceKey<DamageType> key) {
		return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
	}

	private static ResourceKey<DamageType> typeKey(String name) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, name));
	}
}
