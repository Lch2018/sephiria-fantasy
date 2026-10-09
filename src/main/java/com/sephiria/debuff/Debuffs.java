package com.sephiria.debuff;

import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.network.DebuffSyncPayload;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.registry.ModSounds;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 敌人身上的「减益效果」（负面 buff）：魔法科技连击的「触电」与余烬连击的「灼伤」。
 *
 * <p>与玩家侧的限时增益（TimedAttributes）不同，减益挂在<b>目标实体</b>上，两套各存一份
 * 运行时附件，互不干扰——同一只怪可以同时触电又灼伤，脚下会排成两行标签。
 *
 * <p><b>触电</b>整段共享一份时间轴：持续 3 秒，期间每 1.5 秒造成一次周期伤害（第 0 秒不触发，
 * 默认两跳周期）；时间走完时消耗所有层数再补一记终结跳——默认整段一共三跳。
 * 层数（上限 2）不刷新持续时间，只作为乘数放大周期与终结伤害；伤害式里的 x
 * 随这段触电的存续时间成长（每 0.5 秒 +0.5，第 0 秒为 1）。
 *
 * <p><b>灼伤</b>是另一套规矩：持续 4 秒，每 0.5 秒跳一次，层数（上限 2）既当乘数、叠加时还
 * 把持续时间刷回满 4 秒（触电不刷新），时间走完直接消失、没有终结跳。
 * 每跳伤害 = 触发者火元素强度 × 8%（余烬 10 档换成 18%）× 层数。
 *
 * <p>两份减益都按「一份记账」存进目标实体的运行时附件——不值得进存档，目标死亡或重登时
 * 附件跟着实体一起消失（「减益效果若消失，则伤害增加效果也消失」）。
 */
public final class Debuffs {
	/** 触电默认持续时间（tick）：3 秒，结束时消耗所有层数打终结一跳。 */
	private static final int SHOCK_DURATION_TICKS = 60;
	/** 触电默认叠层上限（「默认」二字留给以后改上限的机制）。 */
	private static final int SHOCK_MAX_STACKS = 2;
	/** 周期伤害倍率：触发者电属性强度 × 5% × 层数 × x。 */
	private static final double SHOCK_DAMAGE_RATIO = 0.05D;
	/** 终结伤害倍率：触发者电属性强度 × 7.5% × 层数 × x。 */
	private static final double SHOCK_END_RATIO = 0.075D;
	/** 默认激活间隔（tick）：每 1.5 秒造成一次周期伤害；第 0 秒不触发。 */
	private static final int SHOCK_INTERVAL_TICKS = 30;
	/** x 的成长：触电每存续 0.5 秒 x +0.5，第 0 秒时 x = 1（10 tick = 0.5 秒）。 */
	private static final double SHOCK_X_PER_HALF_SECOND = 0.5D;
	/** 电击之触的冷却（tick）：2 秒。 */
	private static final int ELECTRIC_TOUCH_COOLDOWN_TICKS = 40;
	/** 持续电流特效：存续中每刻在目标身上撒几颗电流火花；结算的爆点在 {@link #activate} 里另放。 */
	private static final int SHOCK_AURA_PARTICLES = 2;
	/** 周期结算时的电流火花数量。 */
	private static final int SHOCK_BURST_PARTICLES = 8;
	/** 终结结算时的电流火花数量。 */
	private static final int SHOCK_END_PARTICLES = 15;

	/** 灼伤持续时间（tick）：4 秒；每次叠加都刷回这个值。 */
	private static final int BURN_DURATION_TICKS = 80;
	/** 灼伤的跳伤害间隔（tick）：每 0.5 秒一跳，4 秒一共八跳。 */
	private static final int BURN_INTERVAL_TICKS = 10;
	/** 灼伤叠层上限：基础 2 层，火焰虫与熔岩珠的「灼伤层」加成加在它上面。 */
	private static final int BURN_MAX_STACKS = 2;
	/** 灼伤每跳的基础倍率（%）：触发者火元素强度 × 它 × 层数；余烬 10 档换成 18（见 {@link #burnPercent}）。 */
	private static final double BURN_BASE_PERCENT = 8.0D;
	/** 火焰之触的冷却（tick）：2 秒。 */
	private static final int FLAME_TOUCH_COOLDOWN_TICKS = 40;
	/** 持续燃烧特效：存续中每刻撒几颗火苗；结算的爆点在 {@link #activateBurn} 里另放。 */
	private static final int BURN_AURA_PARTICLES = 2;
	/** 每跳结算时的火苗数量。 */
	private static final int BURN_BURST_PARTICLES = 8;
	/** 每跳结算时的烟粒数量（烟比火苗少，免得糊住目标）。 */
	private static final int BURN_SMOKE_PARTICLES = 5;
	/** 灼伤粒子浓度按层数线性缩放：1 层 = 上面三个基准数量的 40%，6 层 = 100%（原来的手感）。 */
	private static final double BURN_PARTICLE_LOW_FACTOR = 0.4D;
	/** 浓度 100% 对应的层数：基准数量按这一层数写；再往上按同一斜率继续变浓。 */
	private static final int BURN_PARTICLE_REFERENCE_STACKS = 6;

	/**
	 * 目标身上的触电：一段共享的持续时间 + 一个周期计时器，层数是伤害乘数。
	 *
	 * @param remainingTicks       距持续时间结束的倒计时，归零时打终结一跳并整段消失
	 * @param intervalTicks        周期伤害间隔（tick）
	 * @param ticksUntilActivation 距下一次周期伤害的倒计时
	 * @param stacks               当前层数（1..上限），周期与终结伤害都乘它
	 * @param elapsedTicks         已存续刻数——伤害式里的 x 随它成长
	 * @param attackerId           触发者（最近一次附加的人）：结算时按他当前的电属性强度取数
	 */
	private record Shock(int remainingTicks, int intervalTicks, int ticksUntilActivation,
			int stacks, int elapsedTicks, UUID attackerId) {
	}

	/**
	 * 目标身上的灼伤：持续时间 + 周期计时器 + 层数。
	 *
	 * <p>与 {@link Shock} 不同，这里没有「已存续刻数」——灼伤伤害式里没有 x 那一项，
	 * 时间轴在叠加时会被刷满，所以只有剩余时间一个量。
	 *
	 * @param remainingTicks       距持续时间结束的倒计时（每次叠加都刷回 4 秒）
	 * @param intervalTicks        跳伤害间隔（tick）：基础 10 刻，橡木炭的「灼伤的攻击速度」把它压短；
	 *                             与触电一样在施加那一刻定下来，之后换装不改这一段
	 * @param ticksUntilActivation 距下一跳的倒计时（叠加时保留原节奏，不跟着刷新）
	 * @param stacks               当前层数（1..上限），每跳伤害乘它
	 * @param attackerId           触发者（最近一次附加的人）：结算时按他当前的火元素强度与档位取数
	 */
	private record Burn(int remainingTicks, int intervalTicks, int ticksUntilActivation,
			int stacks, UUID attackerId) {
	}

	/** 触电的减益 id：附件名与脚下层数标签的同步 id 共用（客户端按它找标签文案）。 */
	public static final Identifier SHOCK_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "electric_shocks");

	/** 灼伤的减益 id：与触电同一套用法（标签文案见 {@code ui.sephiria.debuff.burns}）。 */
	public static final Identifier BURN_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "burns");

	/** 触电挂在目标实体上的运行时附件（一份记账：整段共享时间轴与层数）。 */
	private static final AttachmentType<Shock> SHOCKS = AttachmentRegistry.<Shock>builder()
			.buildAndRegister(SHOCK_ID);

	/** 灼伤挂在目标实体上的运行时附件（独立一份，与触电互不影响）。 */
	private static final AttachmentType<Burn> BURNS = AttachmentRegistry.<Burn>builder()
			.buildAndRegister(BURN_ID);

	/** 电击之触的冷却按攻击者记：多久能给下一个目标附加新一层。 */
	private static final Map<UUID, Long> LAST_TOUCH = new HashMap<>();

	/** 火焰之触的冷却，同样按攻击者记（与电击之触各算各的）。 */
	private static final Map<UUID, Long> LAST_FLAME_TOUCH = new HashMap<>();

	/** 脚下层数标签的广播距离（格）：与客户端那边的显示距离配套，远处就不发了。 */
	private static final double LABEL_RANGE = 64.0D;

	/** 上次推给客户端的层数（减益 id → 实体 id → 层数）：变了才推，别每刻刷包。 */
	private static final Map<Identifier, Map<Integer, Integer>> SENT_STACKS = new HashMap<>();

	private Debuffs() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			// 挂在 AFTER_DAMAGE 上与吸血同理：只有扣完护甲、减伤之后真正打掉的那一下才算触发
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

			// 触发口径：武器 / 神器伤害，以及**攻击打出的元素伤害**——电的那条是乌云雷击、雷之裁决光束、
			// 桑德耳环、树枝与台风的附加闪电（electric_attack 类型），火的那条是太阳剑（sun_sword）
			// 与红蛇之眼的陨石（fire_attack 类型）。
			// 两种减益自己的结算（electric_shock / burn）都是「减益伤害」，天然进不来，不会一层套一层。
			boolean weaponOrArtifact = kind == SephiriaDamage.Kind.WEAPON || kind == SephiriaDamage.Kind.ARTIFACT;
			boolean electricAttack = kind == SephiriaDamage.Kind.ELECTRIC
					&& !source.is(SephiriaDamage.ELECTRIC_SHOCK);
			boolean fireAttack = kind == SephiriaDamage.Kind.SUN
					|| kind == SephiriaDamage.Kind.FIRE_ATTACK;

			if (weaponOrArtifact || electricAttack) {
				tryApplyShock(entity, player);
			}

			if (weaponOrArtifact || fireAttack) {
				tryApplyBurn(entity, player);
			}
		});

		// 死亡即结算完毕：两份减益都不再跟着尸体跳（也就不会再出声、出粒子），脚下的层数标签同时收掉
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			entity.removeAttached(SHOCKS);
			entity.removeAttached(BURNS);
			syncStacks(SHOCK_ID, entity, 0);
			syncStacks(BURN_ID, entity, 0);
		});
	}

	/**
	 * 把「这个实体身上某个减益几层」推给附近的玩家：敌人脚下的层数标签读它（0 = 收掉标签）。
	 *
	 * <p>层数只存在服务端的附件里，而标签是给所有人看的，所以按距离广播、而不是只发给攻击者。
	 * 只有层数真的变了才发（叠加 +1、整段结束归 0），平时每刻调用只是一次表查询。
	 */
	private static void syncStacks(Identifier debuff, LivingEntity entity, int stacks) {
		Map<Integer, Integer> perEntity = SENT_STACKS.computeIfAbsent(debuff, key -> new HashMap<>());
		Integer sent = perEntity.get(entity.getId());

		if (sent == null && stacks <= 0) {
			// 从没推过、现在也没有：不用发（没这个减益的实体每刻都会走到这里）
			return;
		}

		if (sent != null && sent == stacks) {
			return;
		}

		if (stacks <= 0) {
			perEntity.remove(entity.getId());

			if (perEntity.isEmpty()) {
				SENT_STACKS.remove(debuff);
			}
		}
		else {
			perEntity.put(entity.getId(), stacks);
		}

		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(entity) <= LABEL_RANGE * LABEL_RANGE) {
				ServerPlayNetworking.send(player, new DebuffSyncPayload(debuff, entity.getId(), stacks));
			}
		}
	}

	/** 玩家退出时清冷却表（与 PlayerStats.forget 同一批调用）。 */
	public static void forget(ServerPlayer player) {
		LAST_TOUCH.remove(player.getUUID());
		LAST_FLAME_TOUCH.remove(player.getUUID());
	}

	/** 实体每刻把身上的减益往前推一格（mixin 从 LivingEntity.tick 调进来）；两套各推各的。 */
	public static void tick(LivingEntity entity, ServerLevel level) {
		// 目标一死就整段作废。死在结算那一跳里时，附件已经在 AFTER_DEATH 里被清掉，但那一跳
		// 会带着「重新挂回去」的写法继续往下走——于是尸体上又长出减益，接着按秒冒粒子、响音效。
		// 这里在入口再挡一道：尸体不吃减益，也不再继续结算。
		if (!entity.isAlive()) {
			entity.removeAttached(SHOCKS);
			entity.removeAttached(BURNS);
			syncStacks(SHOCK_ID, entity, 0);
			syncStacks(BURN_ID, entity, 0);
			return;
		}

		tickShock(entity, level);
		tickBurn(entity, level);
	}

	/** 触电：周期到点结算、时间走完补终结跳。 */
	private static void tickShock(LivingEntity entity, ServerLevel level) {
		Shock shock = entity.getAttached(SHOCKS);

		if (shock == null) {
			// 没触电（或刚被清掉）：兜底把脚下的层数标签收掉（实体死亡/卸载时也走这里）
			syncStacks(SHOCK_ID, entity, 0);
			return;
		}

		// 存续刻数每刻都在涨：x 与两跳结算都按「已存续的秒数」算
		int elapsed = shock.elapsedTicks() + 1;
		int lifetime = shock.remainingTicks() - 1;
		int until = shock.ticksUntilActivation() - 1;

		if (lifetime <= 0) {
			// 时间走完：最后一跳周期照发，再消耗所有层数补终结跳，整段触电消失
			if (until <= 0) {
				activate(level, entity, shock.attackerId(), shock.stacks(), elapsed, false);
			}

			activate(level, entity, shock.attackerId(), shock.stacks(), elapsed, true);
			entity.removeAttached(SHOCKS);
			syncStacks(SHOCK_ID, entity, 0);
			return;
		}

		if (until <= 0) {
			activate(level, entity, shock.attackerId(), shock.stacks(), elapsed, false);
			until = shock.intervalTicks();
		}

		entity.setAttached(SHOCKS, new Shock(lifetime, shock.intervalTicks(), until,
				shock.stacks(), elapsed, shock.attackerId()));
		syncStacks(SHOCK_ID, entity, shock.stacks());

		// 电流特效：身上挂着触电的目标每刻都有蓝白色电流火花；音效只在结算那一下放，避免一直响
		level.sendParticles(ModParticleTypes.BLUE_SPARK, entity.getX(), entity.getY(0.5D), entity.getZ(),
				SHOCK_AURA_PARTICLES, 0.35D, 0.55D, 0.35D, 0.02D);
	}

	/**
	 * 灼伤粒子的层数浓度倍率：1 层 40%，到 {@link #BURN_PARTICLE_REFERENCE_STACKS} 层满 100%
	 * （= 上面那三个基准数量），再往上按同一斜率继续涨——烧得越旺粒子越密。
	 */
	private static double burnParticleFactor(int stacks) {
		int level = Math.max(1, stacks);
		return BURN_PARTICLE_LOW_FACTOR + (1.0D - BURN_PARTICLE_LOW_FACTOR)
				* (level - 1) / (BURN_PARTICLE_REFERENCE_STACKS - 1);
	}

	/**
	 * 按浓度倍率折算粒子数：整数部分照发，小数部分按概率补一颗。
	 * 直接取整会把低层数抹平——1 层时 2 颗 × 40% = 0.8，取整成 0 就一点火苗都没有了。
	 */
	private static int burnParticleCount(int base, double factor, RandomSource random) {
		double exact = base * factor;
		int count = (int) exact;
		return count + (random.nextDouble() < exact - count ? 1 : 0);
	}

	/** 灼伤：每 0.5 秒跳一跳，时间走完直接消失（没有终结跳）。 */
	private static void tickBurn(LivingEntity entity, ServerLevel level) {
		Burn burn = entity.getAttached(BURNS);

		if (burn == null) {
			syncStacks(BURN_ID, entity, 0);
			return;
		}

		int lifetime = burn.remainingTicks() - 1;
		int until = burn.ticksUntilActivation() - 1;

		if (until <= 0) {
			activateBurn(level, entity, burn.attackerId(), burn.stacks());
			until = burn.intervalTicks();
		}

		if (lifetime <= 0) {
			entity.removeAttached(BURNS);
			syncStacks(BURN_ID, entity, 0);
			return;
		}

		entity.setAttached(BURNS, new Burn(lifetime, burn.intervalTicks(), until, burn.stacks(),
				burn.attackerId()));
		syncStacks(BURN_ID, entity, burn.stacks());

		// 燃烧特效：火苗贴着目标往上冒，浓度随层数（1 层 40% → 6 层 100%）；
		// 音效只在结算那一下放，避免一直响
		level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY(0.5D), entity.getZ(),
				burnParticleCount(BURN_AURA_PARTICLES, burnParticleFactor(burn.stacks()), level.getRandom()),
				0.35D, 0.55D, 0.35D, 0.01D);
	}

	/**
	 * 电击之触的公共入口：造成伤害后按规则给目标附加一层触电——
	 * 守门（魔法科技 2 档才有）、冷却（2 秒，按攻击者记）与上限都在这里。
	 *
	 * <p>由 AFTER_DAMAGE 事件调用（武器 / 神器 / 电属性攻击伤害都算，见 {@code register}）；
	 * 桑德耳环的「直接上触电」不走这里——那是耳环自己的词条，与电击之触无关。
	 */
	public static void tryApplyShock(LivingEntity target, ServerPlayer player) {
		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.MAGIC_TECH, 0);

		if (!ArtifactCombo.MAGIC_TECH.electricTouch(comboLevel)) {
			return;
		}

		// 冷却加速沿用冲刺恢复速度的算法：间隔 ÷ (1 + 加成)，向上取整免得小加成被取整吃掉
		long interval = (long) Math.ceil(ELECTRIC_TOUCH_COOLDOWN_TICKS
				/ (1.0D + ArtifactCombo.MAGIC_TECH.touchHastePercent(comboLevel) / 100.0D));
		long now = player.level().getGameTime();
		Long last = LAST_TOUCH.get(player.getUUID());

		// 冷却表不能拿 Long.MIN_VALUE 当「从没触发过」的初值：now - MIN_VALUE 会溢出成负数，
		// 第一次判定就被当成「还在冷却」直接返回，LAST_TOUCH 于是永远写不进去——电击之触全废。
		// 用 null 判「没触发过」。
		if (last != null && now - last < interval) {
			return;
		}

		if (!applyShock(target, player)) {
			return;
		}

		LAST_TOUCH.put(player.getUUID(), now);
	}

	/**
	 * 火焰之触的公共入口：与 {@link #tryApplyShock} 同型——余烬 2 档才有、冷却 2 秒按攻击者记
	 * （8 档的「冷却加速 150%」把间隔压到 0.8 秒），层数满了就不加、也不吃掉这次冷却。
	 */
	public static void tryApplyBurn(LivingEntity target, ServerPlayer player) {
		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.EMBER, 0);

		if (!ArtifactCombo.EMBER.flameTouch(comboLevel)) {
			return;
		}

		long interval = (long) Math.ceil(FLAME_TOUCH_COOLDOWN_TICKS
				/ (1.0D + ArtifactCombo.EMBER.touchHastePercent(comboLevel) / 100.0D));
		long now = player.level().getGameTime();
		Long last = LAST_FLAME_TOUCH.get(player.getUUID());

		if (last != null && now - last < interval) {
			return;
		}

		if (!applyBurn(target, player)) {
			return;
		}

		LAST_FLAME_TOUCH.put(player.getUUID(), now);
	}

	/**
	 * 给目标附加一层触电（电击之触与桑德耳环共用）；层满就不加，返回是否加上了。
	 *
	 * <p>叠加只 +1 层，时间轴（剩余时间 / 周期计时 / 存续刻数）原样保留——「叠加时不会刷新持续时间」；
	 * 上限 = 默认 2 层 + 电击虫的「触电层」加成。加层之后掷一次「强化触电」（雷云追踪指南针）：
	 * 命中就立即额外结算一次触电伤害。
	 */
	public static boolean applyShock(LivingEntity target, ServerPlayer player) {
		// 触电是连击（电击之触）与神器（桑德耳环）的效果，不上玩家身上——这类效果不做 PVP；
		// 返回 false 让调用方当「没加上」处理（冷却也不会被白白消耗）
		if (target instanceof net.minecraft.world.entity.player.Player) {
			return false;
		}

		Shock shock = target.getAttached(SHOCKS);
		int cap = SHOCK_MAX_STACKS + ArtifactEffects.shockStackBonus(player);

		if (shock != null && shock.stacks() >= cap) {
			return false;
		}

		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.MAGIC_TECH, 0);
		int shockInterval = shockIntervalTicks(comboLevel);
		Shock updated = shock == null
				? new Shock(SHOCK_DURATION_TICKS, shockInterval, shockInterval, 1, 0, player.getUUID())
				: new Shock(shock.remainingTicks(), shock.intervalTicks(), shock.ticksUntilActivation(),
						shock.stacks() + 1, shock.elapsedTicks(), player.getUUID());
		target.setAttached(SHOCKS, updated);
		// 层数刚变：立刻推给附近玩家，脚下的层数标签当场跟着变（不必等下一刻的 tick）
		syncStacks(SHOCK_ID, target, updated.stacks());

		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ModParticleTypes.BLUE_SPARK, target.getX(), target.getY(0.5D), target.getZ(),
					5, 0.3D, 0.4D, 0.3D, 0.02D);

			// 强化触电：用刚加上去的层数与存续刻数立即结算一次（与周期跳同式，不是终结跳）
			double enhancedChance = ArtifactEffects.enhancedShockChance(player);

			if (enhancedChance > 0.0D && level.getRandom().nextDouble() * 100.0D < enhancedChance) {
				activate(level, target, player.getUUID(), updated.stacks(), updated.elapsedTicks(), false);
			}
		}

		return true;
	}

	/**
	 * 给目标附加一层灼伤（火焰之触与红蛇之眼的陨石共用）；层满就不加，返回是否加上了。
	 *
	 * <p>与触电相反，灼伤叠加时<b>把持续时间刷回满 4 秒</b>（用户口径：一层剩 2 秒时再来一层，
	 * 变成 2 层、时间重新从 4 秒开始）；只保留原来的跳伤害节奏，免得叠加把两跳挤到一起。
	 *
	 * <p>层数上限 = 默认 2 层 + 火焰虫 / 熔岩珠的「灼伤层」加成；熔岩珠另外还会让这一下
	 * 多叠「额外给予」的层数（上限照旧管着）。跳伤害的间隔在施加那一刻按橡木炭定下来。
	 */
	public static boolean applyBurn(LivingEntity target, ServerPlayer player) {
		if (target instanceof net.minecraft.world.entity.player.Player) {
			return false;
		}

		Burn burn = target.getAttached(BURNS);
		int cap = BURN_MAX_STACKS + ArtifactEffects.burnStackBonus(player);

		if (burn != null && burn.stacks() >= cap) {
			return false;
		}

		// 熔岩珠的「赋予灼伤时额外给予 N 次」：这一下多叠几层（触发的 1 层 + 额外的 N 层）
		int added = 1 + ArtifactEffects.burnExtraApplications(player);
		int stacks = Math.min(cap, (burn == null ? 0 : burn.stacks()) + added);
		int interval = burn == null ? burnIntervalTicks(player) : burn.intervalTicks();
		Burn updated = new Burn(BURN_DURATION_TICKS, interval,
				burn == null ? interval : burn.ticksUntilActivation(), stacks, player.getUUID());
		target.setAttached(BURNS, updated);
		syncStacks(BURN_ID, target, updated.stacks());

		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5D), target.getZ(),
					6, 0.3D, 0.4D, 0.3D, 0.02D);
		}

		return true;
	}

	/** 灼伤每跳的间隔（tick）：基础 10 刻（0.5 秒），橡木炭的「灼伤的攻击速度」把它压短。 */
	private static int burnIntervalTicks(ServerPlayer player) {
		double speed = ArtifactEffects.burnTickSpeedPercent(player);

		return Math.max(1, (int) Math.ceil(BURN_INTERVAL_TICKS / (1.0D + speed / 100.0D)));
	}

	/** 触电的激活间隔（tick）：默认 1.5 秒一跳，10 档「触电激活时间-1秒」缩到 0.5 秒一跳。 */
	private static int shockIntervalTicks(int comboLevel) {
		return Math.max(1, SHOCK_INTERVAL_TICKS
				+ (int) (ArtifactCombo.MAGIC_TECH.shockDurationDelta(comboLevel) * 20.0D));
	}

	/**
	 * 一次结算：周期跳 = 触发者当前电属性强度 × 5% × 层数 × x；
	 * 终结跳（持续时间走完那一下）= × 7.5%，随后整段触电消失。
	 */
	private static void activate(ServerLevel level, LivingEntity target, UUID attackerId,
			int stacks, int elapsedTicks, boolean finishing) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(attackerId);
		// 电属性强度在结算那一刻按触发者现查（面板值口径，与面板上显示的一致）——换装后跟着新的走
		double element = player == null ? 0.0D
				: PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
		// x 第 0 秒为 1，每存续 0.5 秒 +0.5（10 tick = 0.5 秒）
		double x = 1.0D + SHOCK_X_PER_HALF_SECOND * (elapsedTicks / 10);
		double damage = element * (finishing ? SHOCK_END_RATIO : SHOCK_DAMAGE_RATIO) * stacks * x;

		if (damage <= 0.0D) {
			return;
		}

		// 与真实伤害同理：额外的那一下不清无敌帧，就会被目标上一刻吃到的伤害顶掉
		target.setInvulnerableTime(0);
		target.hurtServer(level, SephiriaDamage.electricShock(level, player), (float) damage);
		level.sendParticles(ModParticleTypes.BLUE_SPARK, target.getX(), target.getY(0.5D), target.getZ(),
				SHOCK_BURST_PARTICLES, 0.4D, 0.6D, 0.4D, 0.08D);
		// 短促的电流火花声，只在造成伤害的这一下响：音长约 0.3~0.4 秒，不会叠成持续嗡嗡
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				ModSounds.ELECTRIC_SPARK, SoundSource.PLAYERS, 0.6F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/**
	 * 灼伤的一跳：触发者当前火元素强度 × 倍率（默认 8%，10 档 18%）× 层数 × (1 + 红色线球的额外伤害)。
	 *
	 * <p>倍率与强度都在结算那一刻现查，与触电同口径（换装、连击等级变了就跟着走）。
	 */
	private static void activateBurn(ServerLevel level, LivingEntity target, UUID attackerId, int stacks) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(attackerId);
		double element = player == null ? 0.0D
				: PlayerStats.elementTotal(player, PlayerStats.Element.FIRE);
		double bonus = player == null ? 1.0D
				: 1.0D + ArtifactEffects.burnDamagePercent(player) / 100.0D;
		double damage = element * burnPercent(player) / 100.0D * stacks * bonus;

		if (damage <= 0.0D) {
			return;
		}

		target.setInvulnerableTime(0);
		target.hurtServer(level, SephiriaDamage.burn(level, player), (float) damage);
		// 这一跳的爆点浓度与身上那圈火苗同一个倍率，层数越高烧得越旺
		double factor = burnParticleFactor(stacks);
		RandomSource random = level.getRandom();
		level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5D), target.getZ(),
				burnParticleCount(BURN_BURST_PARTICLES, factor, random), 0.4D, 0.6D, 0.4D, 0.05D);
		level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(0.6D), target.getZ(),
				burnParticleCount(BURN_SMOKE_PARTICLES, factor, random), 0.35D, 0.5D, 0.35D, 0.03D);
		// 灶火那种短促的噼啪声（原版熔炉燃烧用的那个），只在造成伤害的这一下响
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 0.5F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/** 灼伤每跳的倍率（%）：默认 8，余烬 10 档换成 18（触发者掉线时用默认值）。 */
	private static double burnPercent(ServerPlayer player) {
		if (player == null) {
			return BURN_BASE_PERCENT;
		}

		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.EMBER, 0);
		double percent = ArtifactCombo.EMBER.burnBasePercent(comboLevel);

		return percent > 0.0D ? percent : BURN_BASE_PERCENT;
	}
}
