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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 敌人身上的「减益效果」（负面 buff）：魔法科技连击的「触电」、余烬连击的「灼伤」，
 * 以及等离子头盔把两者合并后的「等离子体」。
 *
 * <p>与玩家侧的限时增益（TimedAttributes）不同，减益挂在<b>目标实体</b>上，各存一份
 * 运行时附件，互不干扰——同一只怪可以同时触电又灼伤，脚下会排成两行标签。
 *
 * <p><b>触电</b>整段共享一份时间轴：持续 2 秒，期间每 2 秒造成一次周期伤害（第 0 秒不触发，
 * 默认只在 t=2s 结算一跳）；时间走完层数清空，没有终结跳。
 * 层数（上限 2）不刷新持续时间，只作为乘数放大每跳伤害：每层 = 触发者电元素强度 × 40%。
 *
 * <p><b>灼伤</b>是另一套规矩：持续 4 秒，每 0.5 秒跳一次，层数（上限 2）既当乘数、叠加时还
 * 把持续时间刷回满 4 秒（触电不刷新），时间走完直接消失、没有终结跳。
 * 每跳伤害 = 触发者火元素强度 × 8%（余烬 10 档换成 18%）× 层数。
 *
 * <p><b>等离子体</b>（等离子头盔）：灼伤与触电的层数上限相加、持续时间相加（4+2 秒），
 * 但灼伤 / 触电两条倍率与周期仍分别结算，系数取「火 + 电」（蓝色灼伤再 +25 冰）。
 * 结束像触电一样直接清空层数。
 *
 * <p>两份减益都按「一份记账」存进目标实体的运行时附件——不值得进存档，目标死亡或重登时
 * 附件跟着实体一起消失（「减益效果若消失，则伤害增加效果也消失」）。
 */
public final class Debuffs {
	/** 触电默认持续时间（tick）：2 秒，结束时层数直接清空（没有终结跳）。 */
	private static final int SHOCK_DURATION_TICKS = 40;
	/** 触电默认叠层上限（「默认」二字留给以后改上限的机制）。 */
	private static final int SHOCK_MAX_STACKS = 2;
	/** 周期伤害倍率：触发者电属性强度 × 40% × 层数。 */
	private static final double SHOCK_DAMAGE_RATIO = 0.40D;
	/** 默认激活间隔（tick）：每 2 秒造成一次周期伤害；第 0 秒不触发。 */
	private static final int SHOCK_INTERVAL_TICKS = 40;
	/** 电击之触的冷却（tick）：2 秒。 */
	private static final int ELECTRIC_TOUCH_COOLDOWN_TICKS = 40;
	/** 持续电流特效：存续中每刻在目标身上撒几颗电流火花；结算的爆点在 {@link #activate} 里另放。 */
	private static final int SHOCK_AURA_PARTICLES = 2;
	/** 周期结算时的电流火花数量。 */
	private static final int SHOCK_BURST_PARTICLES = 8;

	/** 灼伤持续时间（tick）：4 秒；每次叠加都刷回这个值。 */
	private static final int BURN_DURATION_TICKS = 80;	/** 灼伤的跳伤害间隔（tick）：每 0.5 秒一跳，4 秒一共八跳。 */
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

	/** 等离子体持续时间（tick）：灼伤 4 秒 + 触电 2 秒。 */
	private static final int PLASMA_DURATION_TICKS = BURN_DURATION_TICKS + SHOCK_DURATION_TICKS;
	/** 蓝色灼伤并进等离子体时额外附加的冰属性伤害（点）：用户口径里的「25」。 */
	private static final double PLASMA_BLUE_ICE_FLAT = 25.0D;
	/** 等离子体每刻的电流 / 火焰粒子数。 */
	private static final int PLASMA_AURA_PARTICLES = 2;
	/** 等离子体结算时的粒子数。 */
	private static final int PLASMA_BURST_PARTICLES = 10;

	/** 冻伤持续时间（tick）：10 秒；每次叠加都刷回满（与灼伤同一套）。 */
	private static final int FROSTBITE_DURATION_TICKS = 200;
	/** 冻伤叠层上限，也就是「冰冻所需的冻伤层数」的基准（冰川 10 档的 -1 加在它上面）。 */
	private static final int FROSTBITE_MAX_STACKS = 5;
	/** 冻伤的减速（%）：固定值——层数只用来倒数冰冻，不把减速越叠越深。 */
	private static final double FROSTBITE_SLOW_PERCENT = 25.0D;
	/** 霜之触的冷却（tick）：2 秒（6 档「冷却加速 150%」把它压到 0.8 秒）。 */
	private static final int FROST_TOUCH_COOLDOWN_TICKS = 40;
	/** 冻伤特效：存续中每刻撒几颗雪。 */
	private static final int FROSTBITE_AURA_PARTICLES = 2;
	/** 挂上冻伤那一下的雪量。 */
	private static final int FROSTBITE_APPLY_PARTICLES = 6;
	/** 冰冻的持续时间（tick）：2 秒——用户没给时长，取这个；改这一处即可。 */
	private static final int FREEZE_DURATION_TICKS = 40;
	/** 冰冻那一下的伤害倍率（%）：触发者冰元素强度 × 它。 */
	private static final double FREEZE_DAMAGE_PERCENT = 200.0D;
	/** 冰冻期间每刻的雪量（比冻伤浓）。 */
	private static final int FROZEN_AURA_PARTICLES = 5;
	/** 冻住的爆点雪量。 */
	private static final int FROZEN_BURST_PARTICLES = 18;

	/** 冻伤减速挂在移动速度上的修饰符名（要能在解除时按名字摘掉）。 */
	private static final Identifier FROSTBITE_SLOW_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frostbite_slow");
	/** 冰冻的「定身」修饰符名：移动速度乘 0，等于定在原地。 */
	private static final Identifier FROZEN_STUN_ID =
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frozen_stun");

	/**
	 * 目标身上的触电：一段共享的持续时间 + 一个周期计时器，层数是伤害乘数。
	 *
	 * @param remainingTicks       距持续时间结束的倒计时，归零时清空所有层数
	 * @param intervalTicks        周期伤害间隔（tick）：默认 40（2 秒）
	 * @param ticksUntilActivation 距下一次周期伤害的倒计时
	 * @param stacks               当前层数（1..上限），每跳伤害乘它
	 * @param attackerId           触发者（最近一次附加的人）：结算时按他当前的电属性强度取数
	 */
	private record Shock(int remainingTicks, int intervalTicks, int ticksUntilActivation,
			int stacks, UUID attackerId) {
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
			int stacks, UUID attackerId, boolean blue) {
	}

	/**
	 * 目标身上的等离子体：灼伤与触电合并后的共用层数 + 共用持续时间，但两条时间轴分开走。
	 *
	 * <p>层数上限 = 灼伤上限 + 触电上限；持续时间 = 灼伤 4 秒 + 触电 2 秒，结束像触电一样
	 * 直接清空层数（不刷新、不终结跳）。
	 *
	 * @param remainingTicks       距整段结束的倒计时
	 * @param burnInterval        灼伤那一路的跳伤害间隔（施加时按橡木炭定下）
	 * @param burnUntil           距下一跳灼伤伤害的倒计时
	 * @param shockInterval       触电那一路的激活间隔（施加时按魔法科技定下）
	 * @param shockUntil          距下一跳触电伤害的倒计时
	 * @param stacks              共用层数（1..灼伤上限 + 触电上限）
	 * @param attackerId          触发者：结算时按他当前的火 / 电 / 冰面板值取数
	 * @param blue                施加时是否带蓝色灼伤（等离子体的火那一路额外加冰）
	 */
	private record Plasma(int remainingTicks, int burnInterval, int burnUntil, int shockInterval,
			int shockUntil, int stacks, UUID attackerId, boolean blue) {
	}

	/**
	 * 目标身上的冻伤：持续时间 + 层数（层数只用来倒数冰冻，见类注释）。
	 *
	 * @param remainingTicks 距持续时间结束的倒计时（每次叠加都刷回 10 秒）
	 * @param stacks         当前层数（1..阈值），够了就变成冰冻
	 * @param attackerId     触发者（最近一次附加的人）：变成冰冻时按他的冰元素强度结算
	 */
	private record Frostbite(int remainingTicks, int stacks, UUID attackerId) {
	}

	/**
	 * 目标身上的冰冻（眩晕）：持续时间走完自动解冻。
	 *
	 * @param remainingTicks 距解冻的倒计时
	 * @param attackerId     触发者：解冻时不需要再算什么，留着是为了以后可能的收尾效果
	 */
	private record Frozen(int remainingTicks, UUID attackerId) {
	}

	/** 触电的减益 id：附件名与脚下层数标签的同步 id 共用（客户端按它找标签文案）。 */
	public static final Identifier SHOCK_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "electric_shocks");

	/** 灼伤的减益 id：与触电同一套用法（标签文案见 {@code ui.sephiria.debuff.burns}）。 */
	public static final Identifier BURN_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "burns");

	/** 等离子体的减益 id：灼伤与触电合并后的标签文案见 {@code ui.sephiria_fantasy.debuff.plasma}。 */
	public static final Identifier PLASMA_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "plasma");

	/** 冻伤的减益 id：标签文案见 {@code ui.sephiria_fantasy.debuff.frostbites}。 */
	public static final Identifier FROSTBITE_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frostbites");

	/** 冰冻（眩晕）的减益 id：客户端按它把被冻住的敌人染成冰蓝（见 FrozenTintMixin）。 */
	public static final Identifier FROZEN_ID = Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "frozen");

	/** 触电挂在目标实体上的运行时附件（一份记账：整段共享时间轴与层数）。 */
	private static final AttachmentType<Shock> SHOCKS = AttachmentRegistry.<Shock>builder()
			.buildAndRegister(SHOCK_ID);

	/** 灼伤挂在目标实体上的运行时附件（独立一份，与触电互不影响）。 */
	private static final AttachmentType<Burn> BURNS = AttachmentRegistry.<Burn>builder()
			.buildAndRegister(BURN_ID);

	/** 等离子体：灼伤与触电合并后的那一份附件（与两份原减益互斥，施加时会清掉它们）。 */
	private static final AttachmentType<Plasma> PLASMAS = AttachmentRegistry.<Plasma>builder()
			.buildAndRegister(PLASMA_ID);

	/** 冻伤（冰川）：独立一份，与触电 / 灼伤并行。 */
	private static final AttachmentType<Frostbite> FROSTBITES = AttachmentRegistry.<Frostbite>builder()
			.buildAndRegister(FROSTBITE_ID);

	/** 冰冻（眩晕）：冻伤叠满后换成它，走完自动解冻。 */
	private static final AttachmentType<Frozen> FROZEN = AttachmentRegistry.<Frozen>builder()
			.buildAndRegister(FROZEN_ID);

	/** 电击之触的冷却按攻击者记：多久能给下一个目标附加新一层。 */
	private static final Map<UUID, Long> LAST_TOUCH = new HashMap<>();

	/** 火焰之触的冷却，同样按攻击者记（与电击之触各算各的）。 */
	private static final Map<UUID, Long> LAST_FLAME_TOUCH = new HashMap<>();

	/** 霜之触的冷却，同样按攻击者记。 */
	private static final Map<UUID, Long> LAST_FROST_TOUCH = new HashMap<>();

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
			// 太阳剑分火 / 冰两版（永恒蚀在背包右半边时走冰的那版），两版的 Kind 都是 SUN，
			// 所以这一条按**伤害类型**认而不是按 Kind：元素不同，该触发的减益也不同
			boolean fireAttack = source.is(SephiriaDamage.SUN_SWORD)
					|| kind == SephiriaDamage.Kind.FIRE_ATTACK;
			// 冰的那条：攻击打出的冰属性伤害（ice_attack，以及太阳剑的冰版 ice_sun_sword）；
			// 冰冻自己的结算也是冰伤害，但它是减益伤害（Kind.ICE），进不来这里
			boolean iceAttack = kind == SephiriaDamage.Kind.ICE_ATTACK
					|| source.is(SephiriaDamage.ICE_SUN_SWORD);
			// 酷寒断头台：冰 + 电双属性，两条之触都算
			boolean cryoAttack = kind == SephiriaDamage.Kind.CRYO;

			if (weaponOrArtifact || electricAttack || cryoAttack) {
				tryApplyShock(entity, player);
			}

			if (weaponOrArtifact || fireAttack) {
				tryApplyBurn(entity, player);
			}

			if (weaponOrArtifact || iceAttack || cryoAttack) {
				tryApplyFrostbite(entity, player);
			}
		});

		// 死亡即结算完毕：三份减益都不再跟着尸体跳（也就不会再出声、出粒子），脚下的层数标签同时收掉
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			entity.removeAttached(SHOCKS);
			entity.removeAttached(BURNS);
			entity.removeAttached(PLASMAS);
			entity.removeAttached(FROSTBITES);
			entity.removeAttached(FROZEN);
			clearSlows(entity);
			syncStacks(SHOCK_ID, entity, 0);
			syncStacks(BURN_ID, entity, 0);
			syncStacks(PLASMA_ID, entity, 0);
			syncStacks(FROSTBITE_ID, entity, 0);
			syncStacks(FROZEN_ID, entity, 0);
		});
	}

	/**
	 * 摘掉冻伤 / 冰冻挂在移动速度上的修饰符。
	 *
	 * <p>属性修饰符是实体的一部分（不像附件那样随实体消失），所以每一处「减益没了」的出口
	 * （到期、变成冰冻、死亡、实体不再存活）都必须调它，否则怪会一直带着那个减速跑。
	 */
	private static void clearSlows(LivingEntity entity) {
		AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);

		if (instance != null) {
			instance.removeModifier(FROSTBITE_SLOW_ID);
			instance.removeModifier(FROZEN_STUN_ID);
		}
	}

	/**
	 * 给目标挂一条临时的移动速度修饰符（同名的会覆盖，不会叠）。
	 *
	 * <p>先查一遍同名的：{@code addOrUpdateTransientModifier} 是按<b>对象引用</b>判「是不是同一个」
	 * 的（原版源码里是 {@code if_acmpne}），每刻都塞一个新对象进去会把属性标脏、
	 * 每刻同步一次属性包；查过再决定要不要写，就只在真的变了那一次同步。
	 */
	private static void applySlow(LivingEntity entity, Identifier id, double amount) {
		AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);

		if (instance == null) {
			return;
		}

		AttributeModifier existing = instance.getModifier(id);

		if (existing != null && existing.amount() == amount) {
			return;
		}

		instance.addOrUpdateTransientModifier(
				new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
		LAST_FROST_TOUCH.remove(player.getUUID());
	}

	/** 实体每刻把身上的减益往前推一格（mixin 从 LivingEntity.tick 调进来）；几套各推各的。 */
	public static void tick(LivingEntity entity, ServerLevel level) {
		// 目标一死就整段作废。死在结算那一跳里时，附件已经在 AFTER_DEATH 里被清掉，但那一跳
		// 会带着「重新挂回去」的写法继续往下走——于是尸体上又长出减益，接着按秒冒粒子、响音效。
		// 这里在入口再挡一道：尸体不吃减益，也不再继续结算。
		if (!entity.isAlive()) {
			entity.removeAttached(SHOCKS);
			entity.removeAttached(BURNS);
			entity.removeAttached(PLASMAS);
			entity.removeAttached(FROSTBITES);
			entity.removeAttached(FROZEN);
			clearSlows(entity);
			syncStacks(SHOCK_ID, entity, 0);
			syncStacks(BURN_ID, entity, 0);
			syncStacks(PLASMA_ID, entity, 0);
			syncStacks(FROSTBITE_ID, entity, 0);
			syncStacks(FROZEN_ID, entity, 0);
			return;
		}

		tickShock(entity, level);
		tickBurn(entity, level);
		tickPlasma(entity, level);
		tickFrostbite(entity, level);
		tickFrozen(entity, level);
	}

	/** 目标是不是正被冻住（眩晕）——MobStunMixin 用它挡掉被冻住时的攻击。 */
	public static boolean isFrozen(LivingEntity entity) {
		return entity.getAttached(FROZEN) != null;
	}

	/** 冻伤：每刻冒雪，时间走完就恢复（减速与标签一起收掉）。 */
	private static void tickFrostbite(LivingEntity entity, ServerLevel level) {
		Frostbite frostbite = entity.getAttached(FROSTBITES);

		if (frostbite == null) {
			// 没冻伤（或刚被换成冰冻）：兜底把减速与层数标签收掉
			clearSlow(entity, FROSTBITE_SLOW_ID);
			syncStacks(FROSTBITE_ID, entity, 0);
			return;
		}

		int lifetime = frostbite.remainingTicks() - 1;

		if (lifetime <= 0) {
			entity.removeAttached(FROSTBITES);
			clearSlow(entity, FROSTBITE_SLOW_ID);
			syncStacks(FROSTBITE_ID, entity, 0);
			return;
		}

		entity.setAttached(FROSTBITES, new Frostbite(lifetime, frostbite.stacks(), frostbite.attackerId()));
		syncStacks(FROSTBITE_ID, entity, frostbite.stacks());
		applySlow(entity, FROSTBITE_SLOW_ID, -FROSTBITE_SLOW_PERCENT / 100.0D);

		// 冰霜特效：贴着目标撒雪（原版雪片粒子），浓度随层数
		level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.5D), entity.getZ(),
				burnParticleCount(FROSTBITE_AURA_PARTICLES, frostbiteParticleFactor(frostbite.stacks()),
						level.getRandom()),
				0.35D, 0.55D, 0.35D, 0.01D);
	}

	/**
	 * 冰冻（眩晕）：<b>完全静止</b>——不攻击、不寻路（{@code MobStunMixin} 取消 {@code Mob#serverAiStep}），
	 * 连被打飞都不动（{@code FrozenMotionMixin} 拦掉 {@code Entity#move}），四肢停在冰冻那一刻的姿势；
	 * 这里负责计时、层数标签、每刻按住动画与速度、以及冒雪特效，时间走完解冻。
	 */
	private static void tickFrozen(LivingEntity entity, ServerLevel level) {
		Frozen frozen = entity.getAttached(FROZEN);

		if (frozen == null) {
			clearSlow(entity, FROZEN_STUN_ID);
			syncStacks(FROZEN_ID, entity, 0);
			return;
		}

		int lifetime = frozen.remainingTicks() - 1;

		if (lifetime <= 0) {
			entity.removeAttached(FROZEN);
			clearSlow(entity, FROZEN_STUN_ID);
			syncStacks(FROZEN_ID, entity, 0);
			return;
		}

		entity.setAttached(FROZEN, new Frozen(lifetime, frozen.attackerId()));
		syncStacks(FROZEN_ID, entity, 1);
		// 定身：移动速度乘 0（原版属性下限也是 0）——只是保险，真正拦住位移的是 FrozenMotionMixin
		applySlow(entity, FROZEN_STUN_ID, -1.0D);
		// 「把那一刻的动画定格」：走路动画的速度按住不放，四肢就停在冰冻那一刻的姿势上
		// （不按住的话它会自己收回到站姿——那也是「动」）
		entity.walkAnimation.setSpeed(0.0F);
		// 速度每刻清零：位置本来就被 move 拦截挡住了，这里是别让速度攒起来（解冻那一刻不会窜出去）
		entity.setDeltaMovement(0.0D, 0.0D, 0.0D);

		level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.5D), entity.getZ(),
				FROZEN_AURA_PARTICLES, 0.4D, 0.6D, 0.4D, 0.02D);
	}

	/** 摘掉某一条减速修饰符（不碰另一条）。 */
	private static void clearSlow(LivingEntity entity, Identifier id) {
		AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);

		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	/** 冻伤粒子的层数浓度倍率：与灼伤同一套（1 层 40% → 5 层 100%）。 */
	private static double frostbiteParticleFactor(int stacks) {
		int level = Math.max(1, stacks);
		return BURN_PARTICLE_LOW_FACTOR + (1.0D - BURN_PARTICLE_LOW_FACTOR)
				* (level - 1) / (FROSTBITE_MAX_STACKS - 1);
	}

	/** 触电：周期到点结算；时间走完清空层数（默认最后一跳正好落在结束那一刻）。 */
	private static void tickShock(LivingEntity entity, ServerLevel level) {
		Shock shock = entity.getAttached(SHOCKS);

		if (shock == null) {
			// 没触电（或刚被清掉）：兜底把脚下的层数标签收掉（实体死亡/卸载时也走这里）
			syncStacks(SHOCK_ID, entity, 0);
			return;
		}

		int lifetime = shock.remainingTicks() - 1;
		int until = shock.ticksUntilActivation() - 1;

		// 结算用当前层数：时间轴不动，叠层只影响之后（以及这一跳）的伤害
		if (until <= 0) {
			activate(level, entity, shock.attackerId(), shock.stacks());
			until = shock.intervalTicks();
		}

		if (lifetime <= 0) {
			// 持续时间结束：不补终结跳，层数随整段触电一起清空
			entity.removeAttached(SHOCKS);
			syncStacks(SHOCK_ID, entity, 0);
			return;
		}

		entity.setAttached(SHOCKS, new Shock(lifetime, shock.intervalTicks(), until,
				shock.stacks(), shock.attackerId()));
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
			activateBurn(level, entity, burn.attackerId(), burn.stacks(), burn.blue());
			until = burn.intervalTicks();
		}

		if (lifetime <= 0) {
			entity.removeAttached(BURNS);
			syncStacks(BURN_ID, entity, 0);
			return;
		}

		entity.setAttached(BURNS, new Burn(lifetime, burn.intervalTicks(), until, burn.stacks(),
				burn.attackerId(), burn.blue()));
		syncStacks(BURN_ID, entity, burn.stacks());

		// 燃烧特效：火苗贴着目标往上冒（蓝色灼伤用蓝色的魂火），浓度随层数（1 层 40% → 6 层 100%）；
		// 音效只在结算那一下放，避免一直响
		level.sendParticles(burn.blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
				entity.getX(), entity.getY(0.5D), entity.getZ(),
				burnParticleCount(BURN_AURA_PARTICLES, burnParticleFactor(burn.stacks()), level.getRandom()),
				0.35D, 0.55D, 0.35D, 0.01D);
	}

	/**
	 * 等离子体：两条时间轴分别走——灼伤按灼伤间隔跳、触电按触电间隔跳，共用层数与剩余时间；
	 * 时间走完像触电一样直接清空层数（没有终结跳）。
	 */
	private static void tickPlasma(LivingEntity entity, ServerLevel level) {
		Plasma plasma = entity.getAttached(PLASMAS);

		if (plasma == null) {
			syncStacks(PLASMA_ID, entity, 0);
			return;
		}

		int lifetime = plasma.remainingTicks() - 1;
		int burnUntil = plasma.burnUntil() - 1;
		int shockUntil = plasma.shockUntil() - 1;

		if (burnUntil <= 0) {
			activatePlasmaBurn(level, entity, plasma);
			burnUntil = plasma.burnInterval();
		}

		if (shockUntil <= 0) {
			activatePlasmaShock(level, entity, plasma);
			shockUntil = plasma.shockInterval();
		}

		if (lifetime <= 0) {
			entity.removeAttached(PLASMAS);
			syncStacks(PLASMA_ID, entity, 0);
			return;
		}

		entity.setAttached(PLASMAS, new Plasma(lifetime, plasma.burnInterval(), burnUntil,
				plasma.shockInterval(), shockUntil, plasma.stacks(), plasma.attackerId(), plasma.blue()));
		syncStacks(PLASMA_ID, entity, plasma.stacks());

		// 等离子体的菱形粒子：粉 20% / 白 60% / 蓝 20%（客户端逐颗染色），层数越高越密
		double factor = burnParticleFactor(plasma.stacks());
		RandomSource random = level.getRandom();
		level.sendParticles(ModParticleTypes.PLASMA,
				entity.getX(), entity.getY(0.5D), entity.getZ(),
				burnParticleCount(PLASMA_AURA_PARTICLES * 2, factor, random),
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
	 * <p>叠加只 +1 层，时间轴（剩余时间 / 周期计时）原样保留——「叠加时不会刷新持续时间」；
	 * 上限 = 默认 2 层 + 电击虫的「触电层」加成。加层之后掷一次「强化触电」（雷云追踪指南针）：
	 * 命中就立即额外结算一次触电伤害。
	 */
	public static boolean applyShock(LivingEntity target, ServerPlayer player) {
		// 触电是连击（电击之触）与神器（桑德耳环）的效果，不上玩家身上——这类效果不做 PVP；
		// 返回 false 让调用方当「没加上」处理（冷却也不会被白白消耗）
		if (target instanceof net.minecraft.world.entity.player.Player) {
			return false;
		}

		// 等离子头盔：灼伤 / 触电都并进等离子体，层数与持续时间共用。
		// 目标身上已经有等离子体时，后来者（哪怕没戴头盔）也只能往这一份上加，避免三份减益共存。
		if (target.getAttached(PLASMAS) != null || ArtifactEffects.plasmaDebuff(player)) {
			if (!applyPlasma(target, player, 1, false)) {
				return false;
			}

			// 「强化触电」在等离子体上也生效：刚刚加上去的那一层立即补一次触电那一路的结算
			if (target.level() instanceof ServerLevel level) {
				double enhancedChance = ArtifactEffects.enhancedShockChance(player);

				if (enhancedChance > 0.0D && level.getRandom().nextDouble() * 100.0D < enhancedChance) {
					Plasma plasma = target.getAttached(PLASMAS);

					if (plasma != null) {
						activatePlasmaShock(level, target, plasma);
					}
				}
			}

			return true;
		}

		Shock shock = target.getAttached(SHOCKS);
		int cap = SHOCK_MAX_STACKS + ArtifactEffects.shockStackBonus(player);

		if (shock != null && shock.stacks() >= cap) {
			return false;
		}

		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.MAGIC_TECH, 0);
		int shockInterval = shockIntervalTicks(comboLevel);
		Shock updated = shock == null
				? new Shock(SHOCK_DURATION_TICKS, shockInterval, shockInterval, 1, player.getUUID())
				: new Shock(shock.remainingTicks(), shock.intervalTicks(), shock.ticksUntilActivation(),
						shock.stacks() + 1, player.getUUID());
		target.setAttached(SHOCKS, updated);
		// 层数刚变：立刻推给附近玩家，脚下的层数标签当场跟着变（不必等下一刻的 tick）
		syncStacks(SHOCK_ID, target, updated.stacks());

		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ModParticleTypes.BLUE_SPARK, target.getX(), target.getY(0.5D), target.getZ(),
					5, 0.3D, 0.4D, 0.3D, 0.02D);

			// 强化触电：用刚加上去的层数立即结算一次（与周期跳同式）
			double enhancedChance = ArtifactEffects.enhancedShockChance(player);

			if (enhancedChance > 0.0D && level.getRandom().nextDouble() * 100.0D < enhancedChance) {
				activate(level, target, player.getUUID(), updated.stacks());
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

		// 等离子头盔：灼伤也并进等离子体；熔岩珠的「额外给予 N 次」照样多叠 N 层。
		// 目标已经有等离子体时，没戴头盔的后来者也并进去。
		if (target.getAttached(PLASMAS) != null || ArtifactEffects.plasmaDebuff(player)) {
			return applyPlasma(target, player, 1 + ArtifactEffects.burnExtraApplications(player), true);
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
		// 蓝色灼伤（封印的特加斯）在每一次赋予时重新判定：中途换装摘掉它，下一次赋予就变回普通灼伤
		boolean blue = ArtifactEffects.blueBurn(player);
		Burn updated = new Burn(BURN_DURATION_TICKS, interval,
				burn == null ? interval : burn.ticksUntilActivation(), stacks, player.getUUID(), blue);
		target.setAttached(BURNS, updated);
		syncStacks(BURN_ID, target, updated.stacks());

		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(blue ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
					target.getX(), target.getY(0.5D), target.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.02D);
		}

		return true;
	}

	/**
	 * 等离子头盔的公共附加入口：把一次灼伤 / 触电并进等离子体。
	 *
	 * <p>层数上限 = 灼伤上限 + 触电上限；持续时间 = 4 秒 + 2 秒，只在新挂上时设定，
	 * 之后叠层不刷新（与触电同口径）；结束直接清空层数。
	 *
	 * <p>第一次挂等离子体时会把目标身上原有的灼伤 / 触电层数折算进来，并移除那两份附件，
	 * 免得同一只怪同时挂三份。灼伤、触电两条时间轴各自按原间隔继续走（新挂时从满间隔开始）。
	 *
	 * @param added           这次要加几层（灼伤带熔岩珠的额外次数）
	 * @param burnApplication 这次是不是灼伤入口（决定蓝色灼伤标记是否跟着更新）
	 */
	private static boolean applyPlasma(LivingEntity target, ServerPlayer player, int added,
			boolean burnApplication) {
		Burn burn = target.getAttached(BURNS);
		Shock shock = target.getAttached(SHOCKS);
		Plasma plasma = target.getAttached(PLASMAS);
		int burnCap = BURN_MAX_STACKS + ArtifactEffects.burnStackBonus(player);
		int shockCap = SHOCK_MAX_STACKS + ArtifactEffects.shockStackBonus(player);
		int cap = burnCap + shockCap;
		int base = plasma != null ? plasma.stacks()
				: (burn == null ? 0 : burn.stacks()) + (shock == null ? 0 : shock.stacks());

		if (base >= cap) {
			return false;
		}

		int stacks = Math.min(cap, base + added);
		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.MAGIC_TECH, 0);
		int burnInterval = plasma != null ? plasma.burnInterval()
				: burn != null ? burn.intervalTicks() : burnIntervalTicks(player);
		int shockInterval = plasma != null ? plasma.shockInterval() : shockIntervalTicks(comboLevel);
		int remaining = plasma != null ? plasma.remainingTicks() : PLASMA_DURATION_TICKS;
		int burnUntil = plasma != null ? plasma.burnUntil() : burnInterval;
		int shockUntil = plasma != null ? plasma.shockUntil() : shockInterval;
		// 灼伤入口每次赋予都重新判定蓝色灼伤（与普通灼伤同一套）；触电入口沿用当前状态
		boolean blue = burnApplication ? ArtifactEffects.blueBurn(player)
				: plasma != null ? plasma.blue() : burn != null && burn.blue();

		if (plasma == null) {
			target.removeAttached(BURNS);
			target.removeAttached(SHOCKS);
			syncStacks(BURN_ID, target, 0);
			syncStacks(SHOCK_ID, target, 0);
		}

		target.setAttached(PLASMAS, new Plasma(remaining, burnInterval, burnUntil, shockInterval,
				shockUntil, stacks, player.getUUID(), blue));
		syncStacks(PLASMA_ID, target, stacks);

		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ModParticleTypes.PLASMA,
					target.getX(), target.getY(0.5D), target.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.02D);
		}

		return true;
	}

	/**
	 * 霜之触的公共入口：与 {@link #tryApplyShock} / {@link #tryApplyBurn} 同型——冰川 2 档才有、
	 * 冷却 2 秒按攻击者记（6 档的「冷却加速 150%」把间隔压到 0.8 秒），冻伤满了就不加、
	 * 也不吃掉这次冷却。
	 */
	public static void tryApplyFrostbite(LivingEntity target, ServerPlayer player) {
		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.GLACIER, 0);

		if (!ArtifactCombo.GLACIER.frostTouch(comboLevel)) {
			return;
		}

		long interval = (long) Math.ceil(FROST_TOUCH_COOLDOWN_TICKS
				/ (1.0D + ArtifactCombo.GLACIER.touchHastePercent(comboLevel) / 100.0D));
		long now = player.level().getGameTime();
		Long last = LAST_FROST_TOUCH.get(player.getUUID());

		if (last != null && now - last < interval) {
			return;
		}

		if (!applyFrostbite(target, player)) {
			return;
		}

		LAST_FROST_TOUCH.put(player.getUUID(), now);
	}

	/**
	 * 给目标附加一层冻伤（霜之触）；冻伤满了不加、返回是否加上了。
	 *
	 * <p>规矩与灼伤同型：叠加把持续时间刷回满 10 秒。层数不放大任何伤害——它只是冰冻的倒计时，
	 * 叠到「冰冻所需的冻伤层数」（基础 5 层，冰川 10 档 -1，见 {@link #freezeThreshold}）就<b>整段换成冰冻</b>：
	 * 冻伤连同它的减速一起收掉，换成一次 200% 冰属性伤害 + 眩晕。
	 *
	 * <p>蓝爪的「冻伤赋予时额外附加一次」在这里掷：命中就再叠一层（可能正好把阈值凑满、当场冻住）。
	 */
	public static boolean applyFrostbite(LivingEntity target, ServerPlayer player) {
		if (target instanceof net.minecraft.world.entity.player.Player) {
			return false;
		}

		// 已经被冻住（眩晕）时不再挂冻伤：不然解冻那一刻手里攒的层数会立刻再冻一次，
		// 变成「一冻到底」的连环控，200% 的伤害也就跟着连环结算了
		if (target.getAttached(FROZEN) != null) {
			return false;
		}

		if (!(target.level() instanceof ServerLevel level)) {
			return false;
		}

		int threshold = freezeThreshold(player);
		Frostbite frostbite = target.getAttached(FROSTBITES);
		int stacks = frostbite == null ? 0 : frostbite.stacks();

		// 层数已经够（阈值被中途换上 10 档的冰川降下来时会这样）：这一下直接冻住，不再往上叠
		if (stacks >= threshold) {
			applyFreeze(level, target, player);
			return true;
		}

		stacks++;

		// 蓝爪：概率额外附加一次（多一层）
		double extraChance = ArtifactEffects.frostbiteExtraChance(player);

		if (extraChance > 0.0D && target.level().getRandom().nextDouble() * 100.0D < extraChance) {
			stacks = Math.min(threshold, stacks + 1);
		}

		level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5D), target.getZ(),
				FROSTBITE_APPLY_PARTICLES, 0.3D, 0.4D, 0.3D, 0.02D);

		// 满层：这一下不再挂冻伤，直接换成冰冻
		if (stacks >= threshold) {
			applyFreeze(level, target, player);
			return true;
		}

		target.setAttached(FROSTBITES, new Frostbite(FROSTBITE_DURATION_TICKS, stacks, player.getUUID()));
		// 层数刚变：立刻推给附近玩家，脚下的层数标签当场跟着变
		syncStacks(FROSTBITE_ID, target, stacks);
		applySlow(target, FROSTBITE_SLOW_ID, -FROSTBITE_SLOW_PERCENT / 100.0D);
		return true;
	}

	/**
	 * 冻伤叠满 → 冰冻：一次冰属性伤害（触发者冰元素强度 × 200%）＋ 眩晕 {@link #FREEZE_DURATION_TICKS} 刻。
	 *
	 * <p>冻伤连同它的减速一起收掉（换成冰冻的定身）。伤害是「减益伤害」（{@code sephiria:freeze}），
	 * 不吃减伤与黄金之手、也不会再触发霜之触。触发者掉线时只有归属没了，伤害照打。
	 */
	private static void applyFreeze(ServerLevel level, LivingEntity target, ServerPlayer player) {
		target.removeAttached(FROSTBITES);
		clearSlow(target, FROSTBITE_SLOW_ID);
		syncStacks(FROSTBITE_ID, target, 0);

		target.setAttached(FROZEN, new Frozen(FREEZE_DURATION_TICKS, player.getUUID()));
		syncStacks(FROZEN_ID, target, 1);

		// 冻住的一瞬清掉全部速度：要的是「一动不动」——连竖直也停，悬空被冻住的怪就定格在半空
		// （解冻后继续掉；要改成照常下落就放行竖直分量，见 FrozenMotionMixin 的注释）
		target.setDeltaMovement(0.0D, 0.0D, 0.0D);

		double element = PlayerStats.elementTotal(player, PlayerStats.Element.ICE);
		double damage = element * FREEZE_DAMAGE_PERCENT / 100.0D;

		if (damage > 0.0D) {
			// 与其它额外结算同理：先清无敌帧，免得这一下被目标上一刻吃到的伤害顶掉
			SephiriaDamage.clearDamageCooldown(target);
			target.hurtServer(level, SephiriaDamage.freeze(level, player), (float) damage);
		}

		level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5D), target.getZ(),
				FROZEN_BURST_PARTICLES, 0.5D, 0.7D, 0.5D, 0.08D);
		// 结冰的一瞬：粉雪的碎裂声 + 一记冰裂（音量都压着，别盖过战斗音效）
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.35F, 1.4F);
	}

	/** 冰冻所需的冻伤层数：基础 {@link #FROSTBITE_MAX_STACKS} 层 + 冰川 10 档的 -1（不低于 1）。 */
	private static int freezeThreshold(ServerPlayer player) {
		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.GLACIER, 0);
		int delta = (int) ArtifactCombo.GLACIER.freezeThresholdDelta(comboLevel);

		return Math.max(1, FROSTBITE_MAX_STACKS + delta);
	}

	/** 灼伤每跳的间隔（tick）：基础 10 刻（0.5 秒），橡木炭的「灼伤的攻击速度」把它压短。 */
	private static int burnIntervalTicks(ServerPlayer player) {
		double speed = ArtifactEffects.burnTickSpeedPercent(player);

		return Math.max(1, (int) Math.ceil(BURN_INTERVAL_TICKS / (1.0D + speed / 100.0D)));
	}

	/** 触电的激活间隔（tick）：默认 2 秒一跳，10 档「触电激活时间-1秒」缩到 1 秒一跳。 */
	private static int shockIntervalTicks(int comboLevel) {
		return Math.max(1, SHOCK_INTERVAL_TICKS
				+ (int) (ArtifactCombo.MAGIC_TECH.shockDurationDelta(comboLevel) * 20.0D));
	}

	/**
	 * 一次周期结算：触发者当前电属性强度 × 40% × 层数。
	 *
	 * <p>触电没有终结跳——持续时间走完只是清空层数（见 {@link #tickShock}）。
	 */
	private static void activate(ServerLevel level, LivingEntity target, UUID attackerId, int stacks) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(attackerId);
		// 电属性强度在结算那一刻按触发者现查（面板值口径，与面板上显示的一致）——换装后跟着新的走
		double element = player == null ? 0.0D
				: PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
		double damage = element * SHOCK_DAMAGE_RATIO * stacks;

		if (damage <= 0.0D) {
			return;
		}

		// 与真实伤害同理：额外的那一下不清无敌帧，就会被目标上一刻吃到的伤害顶掉
		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, SephiriaDamage.electricShock(level, player), (float) damage);

		// 酷寒断头台：对敌人造成触电伤害时，它的冷却缩短 1 秒
		if (player != null) {
			com.sephiria.artifact.CryoGuillotineItem.onShockDamage(player);
		}

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
	 *
	 * <p><b>蓝色灼伤</b>（封印的特加斯）在这一跳上多两件事：强度换成「火元素强度 + 冰元素强度」，
	 * 伤害的元素类型按两者里更高的那个定——火 ≥ 冰走火（{@code sephiria:burn}，{@code Kind.FIRE}），
	 * 冰更高走冰（{@code sephiria:blue_burn}，{@code Kind.ICE}）。两者都是减益伤害，
	 * 所以减伤 / 黄金之手 / 「造成伤害时」那几套口径完全一样，只有元素类型不同。
	 */
	private static void activateBurn(ServerLevel level, LivingEntity target, UUID attackerId, int stacks,
			boolean blue) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(attackerId);
		double fire = player == null ? 0.0D
				: PlayerStats.elementTotal(player, PlayerStats.Element.FIRE);
		double ice = blue && player != null
				? PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
				: 0.0D;
		double bonus = player == null ? 1.0D
				: 1.0D + ArtifactEffects.burnDamagePercent(player) / 100.0D;
		double damage = (fire + ice) * burnPercent(player) / 100.0D * stacks * bonus;

		if (damage <= 0.0D) {
			return;
		}

		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, ice > fire ? SephiriaDamage.blueBurn(level, player)
				: SephiriaDamage.burn(level, player), (float) damage);
		// 这一跳的爆点浓度与身上那圈火苗同一个倍率，层数越高烧得越旺
		double factor = burnParticleFactor(stacks);
		RandomSource random = level.getRandom();
		level.sendParticles(blue ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
				target.getX(), target.getY(0.5D), target.getZ(),
				burnParticleCount(BURN_BURST_PARTICLES, factor, random), 0.4D, 0.6D, 0.4D, 0.05D);
		level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(0.6D), target.getZ(),
				burnParticleCount(BURN_SMOKE_PARTICLES, factor, random), 0.35D, 0.5D, 0.35D, 0.03D);
		// 灶火那种短促的噼啪声（原版熔炉燃烧用的那个），只在造成伤害的这一下响
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 0.5F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/**
	 * 等离子体的灼伤那一路：火 + 电（蓝色时再加 25 点冰）之和 × 灼伤倍率 × 共用层数。
	 *
	 * <p>倍率仍按余烬档位现查（8%，10 档 18%），再乘灼伤的额外伤害与等离子体伤害量。
	 */
	private static void activatePlasmaBurn(ServerLevel level, LivingEntity target, Plasma plasma) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(plasma.attackerId());
		double element = plasmaElement(player, plasma.blue());
		double bonus = player == null ? 1.0D
				: 1.0D + ArtifactEffects.burnDamagePercent(player) / 100.0D;
		double plasmaBonus = player == null ? 1.0D
				: 1.0D + ArtifactEffects.plasmaDamagePercent(player) / 100.0D;
		double damage = element * burnPercent(player) / 100.0D * plasma.stacks() * bonus * plasmaBonus;

		if (damage <= 0.0D) {
			return;
		}

		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, SephiriaDamage.plasma(level, player), (float) damage);
		double factor = burnParticleFactor(plasma.stacks());
		RandomSource random = level.getRandom();
		level.sendParticles(ModParticleTypes.PLASMA,
				target.getX(), target.getY(0.5D), target.getZ(),
				burnParticleCount(PLASMA_BURST_PARTICLES * 2, factor, random), 0.4D, 0.6D, 0.4D, 0.05D);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.PLAYERS, 0.5F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/**
	 * 等离子体的触电那一路：同一个「火 + 电（+25 冰）」系数 × 40% × 共用层数。
	 *
	 * <p>这一跳也吃「强化触电」的立即补结算，并算作触电伤害——酷寒断头台的冷却会因此缩短 1 秒。
	 */
	private static void activatePlasmaShock(ServerLevel level, LivingEntity target, Plasma plasma) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(plasma.attackerId());
		double element = plasmaElement(player, plasma.blue());
		double plasmaBonus = player == null ? 1.0D
				: 1.0D + ArtifactEffects.plasmaDamagePercent(player) / 100.0D;
		double damage = element * SHOCK_DAMAGE_RATIO * plasma.stacks() * plasmaBonus;

		if (damage <= 0.0D) {
			return;
		}

		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, SephiriaDamage.plasma(level, player), (float) damage);

		if (player != null) {
			com.sephiria.artifact.CryoGuillotineItem.onShockDamage(player);
		}

		level.sendParticles(ModParticleTypes.PLASMA, target.getX(), target.getY(0.5D), target.getZ(),
				PLASMA_BURST_PARTICLES, 0.4D, 0.6D, 0.4D, 0.08D);
		level.playSound(null, target.getX(), target.getY(), target.getZ(),
				ModSounds.ELECTRIC_SPARK, SoundSource.PLAYERS, 0.6F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/** 等离子体的共用系数：火 + 电，蓝色灼伤再额外加 25 点冰。 */
	private static double plasmaElement(ServerPlayer player, boolean blue) {
		if (player == null) {
			return 0.0D;
		}

		return PlayerStats.elementTotal(player, PlayerStats.Element.FIRE)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)
				+ (blue ? PLASMA_BLUE_ICE_FLAT : 0.0D);
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
