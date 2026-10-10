package com.sephiria.sun;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.network.SunSwordSyncPayload;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「太阳剑」连击：用武器攻击或魔法书造成伤害时投掷一支太阳剑——不是从玩家身上飞出去的实体，
 * 而是直接在被命中的敌人身上划一道<b>笔直的划痕</b>并结算一次火属性伤害
 * （火元素强度面板值 × 10%，再乘连击档位的「太阳剑伤害」加成）。没有攻击间隔，
 * 每一发符合条件的伤害都会投一支（有剑可投时）。
 *
 * <p>数量（支）是这套系统的资源（见 {@link #BASE_CAPACITY}）：投一支扣一支，
 * 投出去的那支<b>会掉在被命中者脚下</b>——走过去就能捡回来（每支 +1，不超过上限），
 * 这是最主要的回剑方式；另一条是<b>脱战</b>时每 0.1 秒回上限的 10%。
 *
 * <p>掉落的太阳剑不是物品：它是一个真正的物品实体（贴图就是太阳剑）但
 * {@code setNeverPickUp()} + {@code setUnlimitedLifetime()}，原版拾取与自然消失都关掉了，
 * 由这里的邻近判定收进数量里；主人记在物品栈的 {@link #OWNER} 组件上，
 * 所以重登、跨维度回来照样能捡（组件跟着存档走）。
 */
public final class SunSword {
	/** 基础数量上限（2 档激活时就有）。 */
	public static final int BASE_CAPACITY = 5;
	/** 单次投掷的伤害：火元素强度面板值 × 这个比例（%）。 */
	private static final double DAMAGE_PERCENT = 10.0D;
	/** 脱战回数量的间隔与比例：每 0.1 秒回上限的 10%（从 0 到满约 1 秒）。 */
	private static final int OUT_OF_COMBAT_REGEN_INTERVAL = 2;
	private static final double OUT_OF_COMBAT_REGEN_PERCENT = 10.0D;
	/** 拾取距离（格）：以玩家包围盒外扩这么多就算走到跟前了。 */
	private static final double PICKUP_RADIUS = 1.5D;
	/** 掉落物落地高度（格，相对被命中者脚下）：稍微抬一点，让它自己落下去。 */
	private static final double DROP_HEIGHT = 0.5D;
	/** 划痕的长度（格，以被命中者为中心）与采样间距。 */
	private static final double SLASH_LENGTH = 1.7D;
	private static final double SLASH_STEP = 0.09D;
	/** 划痕三色（用户定）：边缘橙、近中黄、正中白。 */
	private static final DustParticleOptions SLASH_EDGE = new DustParticleOptions(0xFDA542, 0.8F);
	private static final DustParticleOptions SLASH_MIDDLE = new DustParticleOptions(0xFDFD64, 0.8F);
	private static final DustParticleOptions SLASH_CENTER = new DustParticleOptions(0xFDFDFB, 0.8F);
	/** 火红的夕阳「增大」那一发：伤害 ×1.33、划痕长度 ×1.6。 */
	private static final double ENLARGE_DAMAGE_MULTIPLIER = 1.33D;
	private static final double ENLARGE_SLASH_SCALE = 1.6D;

	/** 掉落的太阳剑记着主人：只有本人走过去才会被拾取（也用它在重登后认领地上的剑）。 */
	public static final DataComponentType<UUID> OWNER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sun_sword_owner"),
			DataComponentType.<UUID>builder()
					.persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
					.networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString))
					.build());

	/** 一名玩家的太阳剑状态。 */
	private static final class State {
		private double current = BASE_CAPACITY;
		private int regenTimer;
		/** 永恒熔炉的计时器：攒够 sunSwordCapacityEverySeconds × 20 刻就 +1 支。 */
		private int generatorTimer;
		/** 上一次推给客户端的数量 / 上限（NaN = 还没推过，首刻必定同步一次给 HUD）。 */
		private double syncedCurrent = Double.NaN;
		private double syncedMax = Double.NaN;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private SunSword() {
	}

	/** 注册触发器与推进器（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		// 触发口与「被雷击中的树枝」同一套口径：武器 / 魔法书打出的伤害（Kind.WEAPON / ARTIFACT）。
		// 没有冷却——符合条件就投一支（有剑时）；打玩家不算（连击效果不做 PVP）
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

			if (kind != SephiriaDamage.Kind.WEAPON && kind != SephiriaDamage.Kind.ARTIFACT) {
				return;
			}

			if (entity instanceof Player) {
				return;
			}

			// 正午磨刀石：「造成武器伤害时太阳剑额外触发 N 次」——这一下连着投 N+1 支，
			// 每一支照常扣一支、没剑就停（throwSword 返回 false 即结束）
			int times = 1 + ArtifactEffects.sunSwordExtraTriggers(player);

			for (int index = 0; index < times; index++) {
				if (!throwSword(player, entity)) {
					break;
				}
			}
		});
	}

	/** 注册推进器：数量回复、地面拾取、HUD 同步（由 {@link Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(SunSword::tick);
	}

	/** 玩家退出时收掉状态（地上的剑留着，重登回来还能捡）。 */
	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	/** 数量上限：基础 5 支 + 连击各档 + 神器的「太阳剑数量上限」（索利斯·帕尔沃）。 */
	public static double maxCapacity(ServerPlayer player, int comboLevel) {
		return BASE_CAPACITY + ArtifactCombo.SUN_SWORD.sunSwordCapacity(comboLevel)
				+ ArtifactEffects.sunSwordCapacityBonus(player);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int comboLevel = ArtifactEffects.comboLevels(player)
					.getOrDefault(ArtifactCombo.SUN_SWORD, 0);
			boolean active = player.isAlive() && ArtifactCombo.SUN_SWORD.sunActive(comboLevel);

			if (!active) {
				// 没点亮（或人没了）：收掉状态、HUD 也收掉；地上的剑不动，重新点亮后还能捡
				if (STATES.remove(player.getUUID()) != null) {
					ServerPlayNetworking.send(player, new SunSwordSyncPayload(0.0D, 0.0D));
				}

				continue;
			}

			State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());

			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			double max = maxCapacity(player, comboLevel);
			state.current = Math.min(max, state.current);
			regen(player, state, max);
			generate(player, state, max);
			pickUp(player, state, level, max);
			sync(player, state, max);
		}
	}

	/** 脱战回数量：每 0.1 秒回上限的 10%（战斗中不回，靠捡地上的剑）。 */
	private static void regen(ServerPlayer player, State state, double max) {
		if (CombatState.isInCombat(player) || state.current >= max) {
			return;
		}

		state.regenTimer++;

		if (state.regenTimer >= OUT_OF_COMBAT_REGEN_INTERVAL) {
			state.regenTimer = 0;
			state.current = Math.min(max, state.current + max * OUT_OF_COMBAT_REGEN_PERCENT / 100.0D);
		}
	}

	/** 走到自己掉的太阳剑跟前就捡起来：每支 +1（陨铁镜再多收），满了就不捡（留在地上等以后）。 */
	private static void pickUp(ServerPlayer player, State state, ServerLevel level, double max) {
		double perSword = 1.0D + ArtifactEffects.sunSwordPickupBonus(player);

		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class,
				player.getBoundingBox().inflate(PICKUP_RADIUS))) {
			if (state.current >= max) {
				return;
			}

			if (isOurDrop(drop, player)) {
				drop.discard();
				state.current = Math.min(max, state.current + perSword);
			}
		}
	}

	/**
	 * 永恒熔炉：每隔 sunSwordCapacityEverySeconds 秒 +1 支上限内的数量（「生成太阳剑」= 容量增加）。
	 * 只在太阳剑激活时走表（整段 tick 本来就在激活分支里），到上限就停、计时器归零。
	 */
	private static void generate(ServerPlayer player, State state, double max) {
		int everySeconds = ArtifactEffects.sunSwordCapacityEverySeconds(player);

		if (everySeconds <= 0 || state.current >= max) {
			state.generatorTimer = 0;
			return;
		}

		state.generatorTimer++;

		if (state.generatorTimer >= everySeconds * 20) {
			state.generatorTimer = 0;
			state.current = Math.min(max, state.current + 1.0D);
		}
	}

	/** 这支掉落的剑是不是这个玩家丢的（主人记在物品栈的组件上）。 */
	private static boolean isOurDrop(ItemEntity drop, ServerPlayer player) {
		ItemStack stack = drop.getItem();
		return stack.is(ModItems.SUN_SWORD) && player.getUUID().equals(stack.get(OWNER));
	}

	/** 数量 / 上限有变化才推给客户端（HUD 的太阳剑 UI 读它）。 */
	private static void sync(ServerPlayer player, State state, double max) {
		if (state.syncedCurrent == state.current && state.syncedMax == max) {
			return;
		}

		state.syncedCurrent = state.current;
		state.syncedMax = max;
		ServerPlayNetworking.send(player, new SunSwordSyncPayload(state.current, max));
	}

	/**
	 * 投一支太阳剑：直接在被命中者身上结算一次火属性伤害 + 一道笔直的划痕，然后把那支剑
	 * 掉在他脚下（带陨铁耳环时掉在自己附近）。没有剑、或者火元素强度为 0（打不出伤害）时这一发不投。
	 *
	 * @return 这一发有没有真的投出去（正午磨刀石的连投靠它决定要不要继续）
	 */
	private static boolean throwSword(ServerPlayer player, LivingEntity target) {
		State state = STATES.get(player.getUUID());

		// 没激活就没有状态：连击 2 档才有太阳剑
		if (state == null || !(target.level() instanceof ServerLevel level)) {
			return false;
		}

		int comboLevel = ArtifactEffects.comboLevels(player).getOrDefault(ArtifactCombo.SUN_SWORD, 0);
		double max = maxCapacity(player, comboLevel);

		if (state.current < 1.0D) {
			return false;
		}

		double damage = PlayerStats.elementTotal(player, PlayerStats.Element.FIRE) * DAMAGE_PERCENT / 100.0D
				* (1.0D + ArtifactCombo.SUN_SWORD.sunSwordDamagePercent(comboLevel) / 100.0D)
				* (1.0D + ArtifactEffects.sunSwordDamagePercentBonus(player) / 100.0D);

		if (damage <= 0.0D) {
			return false;
		}

		// 火红的夕阳：「概率增大」——伤害 ×1.33，划痕也画得更长
		boolean enlarged = level.getRandom().nextDouble() * 100.0D
				< ArtifactEffects.sunSwordEnlargeChance(player);

		if (enlarged) {
			damage *= ENLARGE_DAMAGE_MULTIPLIER;
		}

		// 索利斯·德克里：无视目标防御力 X%（结算时目标还会再吃一次减伤，这里先补回去）
		damage = withDefenseIgnored(target, player, damage);

		state.current -= 1.0D;

		// 直接打在敌人身上（不是从玩家那儿飞过去的实体），所以先清无敌帧，保证这一下落地
		target.setInvulnerableTime(0);
		target.hurtServer(level, SephiriaDamage.sunSword(level, player), (float) damage);
		slash(level, target, player, enlarged);
		drop(level, player, ArtifactEffects.sunSwordDropNearPlayer(player) ? player.position() : target.position());
		sync(player, state, max);
		return true;
	}

	/**
	 * 「太阳剑无视目标防御力 X%」：目标原本减伤 (1 − m)，现在只吃 (1 − m) × (1 − x)。
	 *
	 * <p>减伤是在伤害结算里乘的（这里够不着），所以先把伤害除以 m、再乘上等效减伤，
	 * 等结算乘完 m 之后就正好是「只吃那么多减伤」。完全免疫或本来不减伤时原样返回。
	 */
	private static double withDefenseIgnored(LivingEntity target, ServerPlayer player, double damage) {
		double ignore = ArtifactEffects.sunSwordDefenseIgnorePercent(player) / 100.0D;

		if (ignore <= 0.0D) {
			return damage;
		}

		float reduction = SephiriaDamage.damageMultiplier(target);

		if (reduction <= 0.0F || reduction >= 1.0F) {
			return damage;
		}

		double effective = 1.0D - (1.0D - reduction) * (1.0D - ignore);
		return damage * effective / reduction;
	}

	/** 笔直的划痕：一条横贯被命中者身体的直线，边缘橙 → 近中黄 → 正中白；增大那一发更长。 */
	private static void slash(ServerLevel level, LivingEntity target, ServerPlayer player, boolean enlarged) {
		Vec3 center = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
		Vec3 look = target.position().subtract(player.position());
		double length = SLASH_LENGTH * (enlarged ? ENLARGE_SLASH_SCALE : 1.0D);

		// 划痕与「玩家 → 目标」的水平方向垂直，看起来就是横着划过去的一刀
		double angle = Math.atan2(look.z, look.x) + Math.PI / 2.0D;
		double stepX = Math.cos(angle) * SLASH_STEP;
		double stepZ = Math.sin(angle) * SLASH_STEP;
		int steps = (int) Math.round(length / SLASH_STEP / 2.0D);

		for (int index = -steps; index <= steps; index++) {
			double offset = index * SLASH_STEP;
			// 中间那两三颗是白的，往外是黄，最外圈是橙
			double edge = Math.abs(offset) / (length / 2.0D);
			DustParticleOptions colour = edge < 0.15D ? SLASH_CENTER : edge < 0.6D ? SLASH_MIDDLE : SLASH_EDGE;
			level.sendParticles(colour, center.x + stepX * index, center.y, center.z + stepZ * index,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/** 把那支太阳剑掉在被命中者脚下：普通物品实体（无重力关掉），但原版拾取与消失都关掉。 */
	private static void drop(ServerLevel level, ServerPlayer player, Vec3 pos) {
		ItemStack stack = new ItemStack(ModItems.SUN_SWORD);
		stack.set(OWNER, player.getUUID());
		ItemEntity drop = new ItemEntity(level, pos.x, pos.y + DROP_HEIGHT, pos.z, stack);
		drop.setNeverPickUp();
		drop.setUnlimitedLifetime();
		// 稍微弹一下，落地更自然（水平随机、竖直向上一点点）
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
		drop.setDeltaMovement(Math.cos(angle) * 0.05D, 0.15D, Math.sin(angle) * 0.05D);
		level.addFreshEntity(drop);
	}
}
