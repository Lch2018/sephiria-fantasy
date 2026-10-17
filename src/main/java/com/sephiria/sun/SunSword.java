package com.sephiria.sun;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.EternalEclipseItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.network.SunSwordSyncPayload;
import com.sephiria.registry.ModItems;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
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
 * 而是直接在被命中的敌人身上划一道<b>笔直的发光剑痕</b>（贴图粒子，见 {@link #slash}）
 * 并结算一次火属性伤害
 * （火元素强度面板值 × 10%，再乘连击档位的「太阳剑伤害」加成）。没有攻击间隔，
 * 每一发符合条件的伤害都会投一支（有剑可投时）。
 *
 * <p>数量（支）是这套系统的资源（见 {@link #BASE_CAPACITY}）：投一支扣一支，
 * 投出去的那支<b>会掉在被命中者脚下</b>——走过去就能捡回来（每支 +1，不超过上限），
 * 这是最主要的回剑方式；另一条是<b>脱战</b>时每 0.1 秒回上限的 10%。
 * <b>数量一到上限，地上剩下的剑就消失</b>（2026-10-14 用户口径）：满容量之后收不进数量，
 * 留着只会一直躺着（见 {@link #clearFullDrops}）。
 *
 * <p>掉落的太阳剑不是物品：它是一个真正的物品实体（贴图就是太阳剑）但
 * {@code setNeverPickUp()} + {@code setUnlimitedLifetime()}，原版拾取与自然消失都关掉了，
 * 由这里的邻近判定收进数量里；主人记在物品栈的 {@link #OWNER} 组件上，
 * 所以重登、跨维度回来照样能捡（组件跟着存档走）。
 *
 * <p><b>永恒蚀在背包右半边时整套换成冰属性</b>（见 {@link #iceMode}）：伤害按<b>冰元素强度</b>
 * 结算、伤害类型换成 {@code sephiria:ice_sun_sword}（于是触发的是霜之触而不是火焰之触），
 * 剑痕换成冰蓝贴图的 {@code sephiria_fantasy:ice_slash} 粒子，<b>掉落的太阳剑</b>也换成冰蓝贴图
 * ——掉落物还是同一个隐藏物品 {@code sun_sword}，只是多挂一个 {@link #ICE_DROP} 标记，
 * 物品模型定义按它挑贴图（Display 与物品图标都没法染色，只能走组件选模型这一手）。
 *
 * <p>冰蓝版的暴击 / 减伤 / 黄金之手口径与火的那版完全一样（{@code SephiriaDamage.Kind.SUN}），
 * 包括索利斯·德克里的「太阳剑暴击几率」。
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
	/**
	 * 容量满了以后清场「自己掉的太阳剑」的半径（格）：见 {@link #clearFullDrops}。
	 *
	 * <p>剑都掉在被命中者脚下、而人是玩家自己打的，正常战斗里离玩家不会超过几格；
	 * 32 格足够覆盖，走远到范围外的那些也没人会再看见。
	 */
	private static final double DROP_SWEEP_RADIUS = 32.0D;
	/** 掉落物落地高度（格，相对被命中者脚下）：稍微抬一点，让它自己落下去。 */
	private static final double DROP_HEIGHT = 0.5D;
	/** 剑痕的长度（格）：这就是那道划痕贴图的方片边长，颜色画在贴图里（橙边 → 黄 → 白芯）。 */
	private static final double SLASH_LENGTH = 1.7D;
	/** 剑痕往攻击者那一侧至少推出目标身体这么多格（够不到时按目标体宽算，见 {@link #slash}）。 */
	private static final double SLASH_FORWARD_OFFSET = 0.5D;
	/**
	 * 剑痕的三个方向（弧度，屏幕面内的倾角；剑痕那张方片是朝摄像机的，所以方向只能这么给）：
	 * 0 = 3 点 → 9 点（横的一道）、+30° = 3 点 → 7 点（右端抬起、缓一些）、
	 * −60° = 5 点 → 11 点（右端压低、陡一些）。正数 = 屏幕上的逆时针。
	 * 每一发随机挑一个，打出来的刀口就不会总是同一条横线。
	 */
	private static final float[] SLASH_ROLLS = {
			0.0F,
			(float) Math.toRadians(30.0D),
			(float) Math.toRadians(-60.0D),
	};
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

	/**
	 * 「这一支掉落物是冰蓝版」的标记组件（永恒蚀在背包右半边时挂上）。
	 *
	 * <p>拾取判定只看物品是不是 {@code sun_sword}（不看这个标记），所以两种颜色都能收进数量里；
	 * 模型定义按这个标记换贴图（见 {@code items/sun_sword.json}）。
	 */
	public static final DataComponentType<Unit> ICE_DROP = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Sephiria.MOD_ID, "sun_sword_ice"),
			DataComponentType.<Unit>builder()
					.persistent(Unit.CODEC)
					.networkSynchronized(Unit.STREAM_CODEC)
					.build());

	/** 一名玩家的太阳剑状态。 */
	private static final class State {
		private double current = BASE_CAPACITY;
		private int regenTimer;
		/** 永恒熔炉的计时器：攒够 sunSwordCapacityEverySeconds × 20 刻就 +1 支。 */
		private int generatorTimer;
		/** 上一刻数量是不是已经满了：用它认出「刚满」那一帧，好在那一刻清掉地上剩下的剑。 */
		private boolean wasFull;
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

	/**
	 * <b>冰霜武具的攻击也触发太阳剑</b>（永恒蚀的词条）：{@code frost.FrostArmament} 的四处结算
	 * （剑气 / 锤击 / 冰长矛 / 冰弓箭矢）每打中一个目标都调一次这里。
	 *
	 * <p>与武器攻击那条路同一个投掷逻辑（有剑可投就投一支、没激活 / 没剑就什么都不做），
	 * 区别只在次数固定为 1——正午磨刀石的「造成武器伤害时额外触发」讲的是武器伤害，
	 * 冰霜武具这四件不算武器伤害。
	 */
	public static void onFrostArmamentHit(ServerPlayer player, LivingEntity target) {
		// 这一下已经把人打死了就别再投了：剑痕与掉落物照样会出现、剑也照扣，纯浪费一支
		if (target instanceof Player || !target.isAlive()) {
			return;
		}

		throwSword(player, target);
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
			clearFullDrops(player, state, level, max);
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

	/**
	 * 走到自己掉的太阳剑跟前就收掉它：数量没满时每支 +1（陨铁镜再多收），
	 * <b>已经满了就直接清掉这一支</b>——满容量收不进数量，留着也捡不走（见 {@link #clearFullDrops}）。
	 */
	private static void pickUp(ServerPlayer player, State state, ServerLevel level, double max) {
		double perSword = 1.0D + ArtifactEffects.sunSwordPickupBonus(player);

		for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class,
				player.getBoundingBox().inflate(PICKUP_RADIUS))) {
			if (!isOurDrop(drop, player)) {
				continue;
			}

			drop.discard();

			if (state.current < max) {
				state.current = Math.min(max, state.current + perSword);
			}
		}
	}

	/**
	 * 数量<b>刚到上限</b>那一刻，把附近自己掉的太阳剑全清掉（2026-10-14 用户口径：
	 * 「掉落的太阳剑在太阳剑容量到达上限时应该消失」）。
	 *
	 * <p>为什么要清：满容量之后地上的剑<b>再也收不进数量</b>（走过去只会白扔一支），
	 * 而它们又刻意关掉了原版拾取与自然消失（见 {@link #drop}）——不清就一直在原地躺着。
	 *
	 * <p>为什么只在「刚满」那一帧扫：数量不满时地上的剑还有用（战斗里就靠走过去捡回来），
	 * 而且这一刻的判据是现算的——脱战回复、永恒熔炉、捡回一支都能把数量顶到上限，都会触发。
	 * 数量一直满着就不再扫了（{@code wasFull} 记着上一刻）：那时候不可能有新剑落地
	 * ——投一支必然把数量减到上限以下。
	 */
	private static void clearFullDrops(ServerPlayer player, State state, ServerLevel level, double max) {
		boolean full = state.current >= max;

		if (full && !state.wasFull) {
			for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class,
					player.getBoundingBox().inflate(DROP_SWEEP_RADIUS))) {
				if (isOurDrop(drop, player)) {
					drop.discard();
				}
			}
		}

		state.wasFull = full;
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
	 * 投一支太阳剑：直接在被命中者身上结算一次属性伤害 + 一道笔直的划痕，然后把那支剑
	 * 掉在他脚下（带陨铁耳环时掉在自己附近）。没有剑、或者元素强度为 0（打不出伤害）时这一发不投。
	 *
	 * <p>元素与颜色看 {@link #iceMode}：冰属性那版按冰元素强度结算、走 {@code ice_sun_sword}
	 * 伤害类型、剑痕与掉落物都是冰蓝的。
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

		boolean ice = iceMode(player);
		double damage = PlayerStats.elementTotal(player, ice ? PlayerStats.Element.ICE : PlayerStats.Element.FIRE)
				* DAMAGE_PERCENT / 100.0D
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
		SephiriaDamage.clearDamageCooldown(target);
		target.hurtServer(level, ice ? SephiriaDamage.iceSunSword(level, player) : SephiriaDamage.sunSword(level, player),
				(float) damage);
		slash(level, target, player, enlarged, ice);
		drop(level, player, ArtifactEffects.sunSwordDropNearPlayer(player) ? player.position() : target.position(),
				ice);
		sync(player, state, max);
		return true;
	}

	/**
	 * 永恒蚀是不是在赛菲莉娅背包的<b>右半边</b>（在 = 太阳剑整套改成冰属性）。
	 *
	 * <p>现查背包（{@link EternalEclipseItem#sideOf}）而不是看物品组件：组件只管图标颜色。
	 */
	private static boolean iceMode(ServerPlayer player) {
		return EternalEclipseItem.sideOf(player) == EternalEclipseItem.Side.RIGHT;
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

	/**
	 * 剑痕：一道笔直的发光划痕——自定义粒子 {@code sephiria_fantasy:sun_slash}（贴图
	 * textures/particle/sun_slash_0.png，行为照原版「横扫之刃」的剑痕写，见 SunSlashParticle），
	 * 长度与方向都在这里定好、随粒子包发给客户端：长度在「增大」（火红的夕阳）那一发 ×1.6，
	 * 方向从 {@link #SLASH_ROLLS} 里随机挑一个（3→9 点 / 3→7 点 / 5→11 点）。
	 *
	 * <p>永恒蚀在右半边时换成冰蓝的那一版（{@code sephiria_fantasy:ice_slash}）：形状一样、
	 * 参数一样，只是贴图的配色不同。
	 *
	 * <p>落点要往<b>攻击者那一侧</b>推出目标身体一点：剑痕是朝向摄像机的一张方片，
	 * 正好切在目标身体正中的话，半个方片埋在模型里、会被目标自己挡住，看起来像一根断掉的虚线。
	 * 推出多少按目标体宽算（窄的僵尸半宽 0.3 格、宽的劫掠兽近 1 格），保证整道划痕都在身体外面。
	 */
	private static void slash(ServerLevel level, LivingEntity target, ServerPlayer player, boolean enlarged,
			boolean ice) {
		Vec3 center = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
		double length = SLASH_LENGTH * (enlarged ? ENLARGE_SLASH_SCALE : 1.0D);
		float roll = SLASH_ROLLS[level.getRandom().nextInt(SLASH_ROLLS.length)];
		Vec3 toPlayer = player.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);

		if (toPlayer.lengthSqr() > 1.0E-4D) {
			double offset = Math.max(SLASH_FORWARD_OFFSET, target.getBbWidth() / 2.0D + 0.15D);
			center = center.add(toPlayer.normalize().scale(offset));
		}

		if (ice) {
			level.sendParticles(new ModParticleTypes.IceSlashOptions((float) length, roll),
					center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		} else {
			level.sendParticles(new ModParticleTypes.SunSlashOptions((float) length, roll),
					center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/**
	 * 把那支太阳剑掉在被命中者脚下：普通物品实体（无重力关掉），但原版拾取与消失都关掉
	 * ——由这里的邻近判定收进数量里（见 {@link #pickUp}），数量满了则由 {@link #clearFullDrops} 清掉。
	 *
	 * <p>冰属性那版挂一个 {@link #ICE_DROP} 标记：掉落物还是同一个隐藏物品，模型定义按标记换贴图。
	 * 拾取只看物品与主人，两种颜色都能收（见 {@link #isOurDrop}）。
	 */
	private static void drop(ServerLevel level, ServerPlayer player, Vec3 pos, boolean ice) {
		ItemStack stack = new ItemStack(ModItems.SUN_SWORD);
		stack.set(OWNER, player.getUUID());

		if (ice) {
			stack.set(ICE_DROP, Unit.INSTANCE);
		}

		ItemEntity drop = new ItemEntity(level, pos.x, pos.y + DROP_HEIGHT, pos.z, stack);
		drop.setNeverPickUp();
		drop.setUnlimitedLifetime();
		// 稍微弹一下，落地更自然（水平随机、竖直向上一点点）
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
		drop.setDeltaMovement(Math.cos(angle) * 0.05D, 0.15D, Math.sin(angle) * 0.05D);
		level.addFreshEntity(drop);
	}
}
