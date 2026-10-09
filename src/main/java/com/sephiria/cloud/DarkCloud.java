package com.sephiria.cloud;

import com.sephiria.artifact.ArtifactCombo;
import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.network.CloudSyncPayload;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 「乌云」连击：玩家头顶飘一朵灰黑色的烟雾云，战斗中每隔一段时间对附近<b>有威胁的生物</b>打一记雷击。
 *
 * <p>容量（堆栈）是这套系统的资源：每次攻击消耗 1 点（10 档「2 点射」连打两下、消耗 2 点），
 * 战斗中每 5 秒回 5%、脱战每 0.1 秒回 5%（都按容量上限的百分比算，见 {@link CombatState}）。
 * 容量不够就不打，等回复。战斗状态的口径是「挨打或主动打出伤害都算」——乌云只看这个开关，
 * 自己开火也会刷新它（云在打，就是在战斗；等云打完了、8 秒没人挨打没人出手，容量才回得快）。
 *
 * <p>攻击间隔由「消耗速度」决定：基础 100% = 每秒一次（20 刻），乐谱《台风》按<b>攻击速度的
 * 百分之多少</b>往上加（攻速 100% + 台风 25% → 消耗速度 125% → 每 0.8 秒一次），
 * 桅杆模型再叠一个固定点数（+20/40/60%）。
 *
 * <p>神器对雷击本身的改动：雷石按百分比放大每记伤害、云种箭头有概率把一记强化成双倍
 * （落点炸得更开）、雷文泥板有概率让这一下不扣容量（每一下单独掷）。
 *
 * <p>视觉是<b>篝火那种大团烟</b>在头顶不断鼓出翻腾（CAMPFIRE_COSY_SMOKE 的大团类圆烟形，
 * 间插 LARGE_SMOKE 压暗成灰黑），云团大小随连击档位放大（2 档 1 倍 → 10 档 3 倍）；
 * 打雷时从云心向目标画一条蓝白色火花线，音效用原版雷声的一半音量。
 */
public final class DarkCloud {
	/** 基础容量（2 档点亮时就有）。 */
	public static final int BASE_CAPACITY = 15;
	/** 基础攻击间隔（tick）：消耗速度 100% = 每 1 秒一次。 */
	private static final int BASE_INTERVAL_TICKS = 20;
	/** 单次雷击的伤害：电元素强度面板值 × 这个比例（%）。 */
	private static final double DAMAGE_PERCENT = 20.0D;
	/** 攻击范围（格）：找最近的有威胁生物。 */
	private static final double RANGE = 8.0D;
	/** 战斗中回容量的间隔与比例：每 5 秒回上限的 5%。 */
	private static final int COMBAT_REGEN_INTERVAL = 100;
	private static final double COMBAT_REGEN_PERCENT = 5.0D;
	/** 脱战回容量的间隔与比例：每 0.1 秒回上限的 5%。 */
	private static final int OUT_OF_COMBAT_REGEN_INTERVAL = 2;
	private static final double OUT_OF_COMBAT_REGEN_PERCENT = 5.0D;
	/** 乌云悬停高度（格）：比贴图时代再高两格（头顶上方不挡视线），与绕圈半径。 */
	private static final double HOVER_HEIGHT = 4.6D;
	private static final double ORBIT_RADIUS = 0.9D;
	/** 烟雾云的补烟间隔（tick）：篝火那种大团烟 spawn 频率要低，靠烟自己慢慢长大叠成云。 */
	private static final int SMOKE_INTERVAL_TICKS = 5;
	/** 雷击音效的音量：比原版雷声小 50%。 */
	private static final float STRIKE_VOLUME = 0.5F;
	/** 云体大小的最大倍率：10 档时是基础体型的 3 倍（2 档 = 1 倍，中间档线性）。 */
	private static final double BODY_SCALE_MAX = 3.0D;
	/** 体型从 1 倍长到最大倍率跨过的连击等级数（2 档 → 10 档）。 */
	private static final int BODY_SCALE_LEVELS = 8;

	/** 一名玩家的乌云状态（视觉改成了纯粒子，不再持有实体）。 */
	private static final class Cloud {
		private final UUID playerId;
		private double capacity = BASE_CAPACITY;
		private int attackCooldown;
		private int regenTimer;
		/** 上一次推给客户端的容量 / 上限（NaN = 还没推过，首刻必定同步一次给 HUD）。 */
		private double syncedCapacity = Double.NaN;
		private double syncedMax = Double.NaN;

		Cloud(UUID playerId) {
			this.playerId = playerId;
		}
	}

	private static final Map<UUID, Cloud> CLOUDS = new HashMap<>();

	private DarkCloud() {
	}

	/** 注册推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(DarkCloud::tick);
	}

	/** 玩家退出时收掉状态（乌云本身只是粒子，没有东西要清）。 */
	public static void forget(ServerPlayer player) {
		CLOUDS.remove(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Cloud cloud = CLOUDS.get(player.getUUID());
			int comboLevel = ArtifactEffects.comboLevels(player)
					.getOrDefault(ArtifactCombo.DARK_CLOUD, 0);
			boolean active = player.isAlive() && ArtifactCombo.DARK_CLOUD.cloudActive(comboLevel);

			if (!active) {
				// 没点亮（或人没了）：把乌云收掉，下次点亮重新攒满容量；
				// 顺手推一份 0/0 让 HUD 的乌云 UI 跟着消失
				if (CLOUDS.remove(player.getUUID()) != null) {
					ServerPlayNetworking.send(player, new CloudSyncPayload(0.0D, 0.0D));
				}

				continue;
			}

			if (cloud == null) {
				cloud = new Cloud(player.getUUID());
				CLOUDS.put(player.getUUID(), cloud);
			}

			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			Vec3 pos = cloudPos(player, level.getGameTime());
			emitSmoke(level, pos, level.getGameTime(), bodyScale(comboLevel));

			regen(player, cloud, comboLevel);
			tryStrike(level, player, cloud, comboLevel, pos);
			syncCapacity(player, cloud, comboLevel);
		}
	}

	/**
	 * 容量 / 上限有变化才推给客户端（HUD 的乌云 UI 读它）。
	 *
	 * <p>变化频率不高：出手时 -1、回复是每 2 刻（脱战）或每 5 秒（战斗）一次、回满自然停；
	 * 上限变化（换神器 / 连击档位）也会被这里带出去。Cloud 里存的上一份初值是 NaN，
	 * 所以刚激活那一 tick 必定同步一次。
	 */
	private static void syncCapacity(ServerPlayer player, Cloud cloud, int comboLevel) {
		double max = maxCapacity(player, comboLevel);

		if (cloud.syncedCapacity == cloud.capacity && cloud.syncedMax == max) {
			return;
		}

		cloud.syncedCapacity = cloud.capacity;
		cloud.syncedMax = max;
		ServerPlayNetworking.send(player, new CloudSyncPayload(cloud.capacity, max));
	}

	/** 容量上限：基础 15 + 连击各档的加成 + 神器的「乌云容量」加成（沙波特果实 / 避雷针）。 */
	public static double maxCapacity(ServerPlayer player, int comboLevel) {
		return BASE_CAPACITY + ArtifactCombo.DARK_CLOUD.cloudCapacity(comboLevel)
				+ ArtifactEffects.cloudCapacityBonus(player);
	}

	/** 乌云此刻的位置：玩家头顶上方两格多，绕圈 + 上下轻飘。 */
	private static Vec3 cloudPos(ServerPlayer player, long now) {
		double angle = (now % 160L) / 160.0D * Math.PI * 2.0D;
		double x = player.getX() + Math.cos(angle) * ORBIT_RADIUS;
		double z = player.getZ() + Math.sin(angle) * ORBIT_RADIUS;
		double y = player.getY() + HOVER_HEIGHT + Math.sin(now / 14.0D) * 0.12D;
		return new Vec3(x, y, z);
	}

	/**
	 * 灰黑色的乌云：主体是<b>篝火那种大团烟（CAMPFIRE_COSY_SMOKE）</b>——类圆、粒子大、
	 * 慢悠悠往上飘，靠低频率不断补烟让大团一个个鼓出来再散掉；间插一点大团黑烟
	 * （LARGE_SMOKE，小一号的暗斑）把整体压向灰黑——原版篝火烟粒本身是浅色的，
	 * 单用它到不了「灰黑」，两条一起就是「篝火的形状 + 灰黑色」。
	 *
	 * <p>云团大小随连击档位放大（见 {@link #bodyScale}）：铺开的范围按倍率放大，
	 * 烟粒数量按面积（倍率的平方）跟着涨——云团变大但不会变稀。
	 */
	private static void emitSmoke(ServerLevel level, Vec3 pos, long now, double scale) {
		if (now % SMOKE_INTERVAL_TICKS != 0L) {
			return;
		}

		int count = (int) Math.round(scale * scale);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z,
				count, 0.35D * scale, 0.1D * scale, 0.35D * scale, 0.0D);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z,
				count, 0.5D * scale, 0.15D * scale, 0.5D * scale, 0.0D);
	}

	/** 云体大小倍率：连击档越高云团越大（2 档 1 倍、10 档 {@link #BODY_SCALE_MAX} 倍，中间线性）。 */
	private static double bodyScale(int comboLevel) {
		int steps = Math.clamp(comboLevel - 2, 0, BODY_SCALE_LEVELS);
		return 1.0D + steps * (BODY_SCALE_MAX - 1.0D) / BODY_SCALE_LEVELS;
	}

	/** 回复容量：战斗中每 5 秒 5%（可被避雷针放大）、脱战每 0.1 秒 5%（都按上限算，回满为止）。 */
	private static void regen(ServerPlayer player, Cloud cloud, int comboLevel) {
		double max = maxCapacity(player, comboLevel);
		boolean inCombat = CombatState.isInCombat(player);
		int interval = inCombat ? COMBAT_REGEN_INTERVAL : OUT_OF_COMBAT_REGEN_INTERVAL;
		double percent = inCombat
				? COMBAT_REGEN_PERCENT * (1.0D + ArtifactEffects.cloudCombatRegenPercent(player) / 100.0D)
				: OUT_OF_COMBAT_REGEN_PERCENT;

		cloud.capacity = Math.min(max, cloud.capacity);
		cloud.regenTimer++;

		if (cloud.regenTimer >= interval) {
			cloud.regenTimer = 0;
			cloud.capacity = Math.min(max, cloud.capacity + max * percent / 100.0D);
		}
	}

	/** 攻击：只要处于战斗状态就打（挨打或主动打出伤害都算战斗），容量够、附近有威胁生物时出手。 */
	private static void tryStrike(ServerLevel level, ServerPlayer player, Cloud cloud, int comboLevel, Vec3 from) {
		if (cloud.attackCooldown > 0) {
			cloud.attackCooldown--;
			return;
		}

		if (!CombatState.isInCombat(player)) {
			return;
		}

		boolean doubleShot = ArtifactCombo.DARK_CLOUD.cloudDoubleShot(comboLevel);
		int shots = doubleShot ? 2 : 1;

		if (cloud.capacity < shots) {
			return;
		}

		List<LivingEntity> targets = threateningNear(level, player);

		if (targets.isEmpty()) {
			return;
		}

		// 消耗速度：基础 100%，再按攻击速度的百分之多少往上加（乐谱《台风》），
		// 最后加上固定点数（桅杆模型）——两者相加后一起算攻击间隔
		double speed = 100.0D + PlayerStats.attackSpeedTotal(player)
				* ArtifactEffects.cloudSpeedPercent(player) / 100.0D
				+ ArtifactEffects.cloudSpeedBonus(player);
		cloud.attackCooldown = (int) Math.max(1.0D, Math.round(BASE_INTERVAL_TICKS * 100.0D / speed));

		for (int shot = 0; shot < shots; shot++) {
			// 每一下都挑当前最近的目标：第一下打死的话第二下自然换人
			LivingEntity target = threateningNear(level, player).stream().findFirst().orElse(null);

			if (target == null) {
				break;
			}

			// 雷文泥板：这一下有可能不扣容量（每一下单独掷；「打不打得起」在上面按容量判过了）
			if (!(level.getRandom().nextDouble() * 100.0D < ArtifactEffects.cloudFreeShotChance(player))) {
				cloud.capacity -= 1.0D;
			}

			strike(level, player, cloud, target, from);
		}
	}

	/** 附近有威胁的生物：只打玩家 / 敌对 / 正在打玩家的生物，和平生物不误伤（SephiriaDamage.strikeable）。 */
	private static List<LivingEntity> threateningNear(ServerLevel level, ServerPlayer player) {
		return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
				candidate -> SephiriaDamage.strikeable(candidate, player)).stream()
				.sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player)))
				.toList();
	}

	/** 一记雷击：电元素强度 × {@link #DAMAGE_PERCENT}% 的电属性伤害 + 蓝白色闪电 + 雷声（半音量）。 */
	private static void strike(ServerLevel level, ServerPlayer player, Cloud cloud, LivingEntity target, Vec3 from) {
		double damage = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)
				* DAMAGE_PERCENT / 100.0D
				* (1.0D + ArtifactEffects.cloudDamagePercent(player) / 100.0D);

		if (damage <= 0.0D) {
			return;
		}

		// 云种箭头：这一记有概率被强化——伤害翻倍（落点也会炸得更开，一眼能看出是双倍）
		boolean enhanced = level.getRandom().nextDouble() * 100.0D
				< ArtifactEffects.cloudEnhancedChance(player);

		if (enhanced) {
			damage *= 2.0D;
		}

		// 电属性攻击伤害：与武器 / 神器伤害一样能触发电击之触（守门见 Debuffs.register，
		// 那里拦下的只有触电自己的结算）
		DamageSource source = SephiriaDamage.electricAttack(level, player);
		// 每记雷击都要落地：清掉无敌帧（与触电结算同一套手法）
		target.setInvulnerableTime(0);
		target.hurtServer(level, source, (float) damage);

		Vec3 to = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
		int steps = (int) Math.ceil(from.distanceTo(to) * 2.0D);

		for (int step = 0; step <= steps; step++) {
			Vec3 point = from.lerp(to, step / (double) Math.max(1, steps));
			level.sendParticles(ModParticleTypes.BLUE_SPARK, point.x, point.y, point.z,
					1, 0.05D, 0.05D, 0.05D, 0.0D);
		}

		if (enhanced) {
			level.sendParticles(ModParticleTypes.BLUE_SPARK, to.x, to.y, to.z, 24, 0.6D, 0.6D, 0.6D, 0.12D);
		}
		else {
			level.sendParticles(ModParticleTypes.BLUE_SPARK, to.x, to.y, to.z, 12, 0.3D, 0.4D, 0.3D, 0.08D);
		}

		level.playSound(null, from.x, from.y, from.z,
				SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, STRIKE_VOLUME, 1.4F);
	}
}
