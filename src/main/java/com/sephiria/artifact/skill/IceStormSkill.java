package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.IceStormItem;
import com.sephiria.cloud.DarkCloud;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModParticleTypes;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 冰风暴（传说魔法书）：准星在 48 格内指到方块 / 实体后，以命中点为中心召唤持续 6 秒的球形冰风暴。
 *
 * <p>风暴半径 12，中心以每秒 2 格（0.1 格/刻）追踪最近的敌人；每 0.25 秒（5 刻）对球形范围内的
 * 所有有威胁生物结算一次冰属性伤害，持续 8 秒。准星没指到东西时 {@link #canCast} 返回 false，
 * 不扣蓝也不进冷却。
 */
public final class IceStormSkill implements ArtifactSkill {
	public static final IceStormSkill INSTANCE = new IceStormSkill();

	private static final int COOLDOWN_TICKS = 440;
	private static final int DURATION_TICKS = 160;
	private static final int INTERVAL_TICKS = 5;
	private static final double MAX_RANGE = 48.0D;
	private static final double RADIUS = 12.0D;
	private static final double TRACK_RANGE = 48.0D;
	private static final double MOVE_SPEED = 0.1D;
	/** 绕 Y 轴转一圈的时间（40 刻 = 2 秒）。 */
	private static final int ROTATION_TICKS = 40;
	/** 初版特效的粒子数量：大范围雪片 + 少量冰晶高光。 */
	private static final int SNOWFLAKE_COUNT = 36;
	/** 36 片雪里一半参与旋转，另一半每刻重新随机散布（初版表现）。 */
	private static final int ROTATING_SNOWFLAKES = SNOWFLAKE_COUNT / 2;
	private static final int END_ROD_COUNT = 8;
	private static final double SNOWFLAKE_SPREAD = RADIUS * 0.7D;
	private static final double END_ROD_SPREAD = RADIUS * 0.5D;
	/** 暴风雪地面改造间隔：与雷霜蝶的暴风雪云一致，5 秒一次。 */
	private static final int BLIZZARD_GROUND_INTERVAL = 100;
	private static final double TWO_PI = Math.PI * 2.0D;

	private static final Map<UUID, Storm> STORMS = new HashMap<>();

	private IceStormSkill() {
	}

	@Override
	public String id() {
		return "ice_storm";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return IceStormItem.mpCost(level);
	}

	@Override
	public boolean canCast(ServerPlayer player, int level) {
		return player.pick(MAX_RANGE, 0.0F, false).getType() != HitResult.Type.MISS;
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		HitResult hit = player.pick(MAX_RANGE, 0.0F, false);

		if (hit.getType() == HitResult.Type.MISS) {
			return;
		}

		Vec3 center;

		if (hit instanceof EntityHitResult entityHit) {
			net.minecraft.world.entity.Entity entity = entityHit.getEntity();
			center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
		} else {
			center = hit.getLocation();
		}

		double damage = (IceStormItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.ICE) * IceStormItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		long now = serverLevel.getGameTime();
		STORMS.put(player.getUUID(), new Storm(serverLevel, player, center, (float) damage, level,
				now + DURATION_TICKS, now));

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(IceStormSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (STORMS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();

		STORMS.entrySet().removeIf(entry -> {
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());

			if (owner == null || !owner.isAlive() || now >= entry.getValue().endTick) {
				return true;
			}

			Storm storm = entry.getValue();

			if (owner.level() != storm.level) {
				// 玩家换维度：旧风暴留在原维度没有意义
				return true;
			}

			track(storm);
			visual(storm);

			if (now >= storm.nextGround) {
				// 与雷霜蝶的暴风雪云同一套环境改造：冻水 / 铺雪；附近有红石或容器就跳过
				if (!DarkCloud.isBlizzardBlocked(storm.level, storm.center)) {
					DarkCloud.applyBlizzardGround(storm.level, storm.center);
				}

				storm.nextGround = now + BLIZZARD_GROUND_INTERVAL;
			}

			if (now >= storm.nextAttack) {
				strike(storm, owner);
				storm.nextAttack = now + INTERVAL_TICKS;
			}

			return false;
		});
	}

	private static void track(Storm storm) {
		LivingEntity target = storm.level.getEntitiesOfClass(LivingEntity.class,
						new AABB(storm.center, storm.center).inflate(TRACK_RANGE)).stream()
				.filter(victim -> SephiriaDamage.autoTargetable(victim, storm.owner))
				.min(Comparator.comparingDouble(victim -> victim.position().distanceToSqr(storm.center)))
				.orElse(null);

		if (target == null) {
			return;
		}

		Vec3 delta = target.position().subtract(storm.center);
		double distance = delta.length();

		if (distance <= MOVE_SPEED) {
			return;
		}

		storm.center = storm.center.add(delta.scale(MOVE_SPEED / distance));
	}

	private static void strike(Storm storm, ServerPlayer owner) {
		AABB area = new AABB(storm.center, storm.center).inflate(RADIUS);

		for (LivingEntity victim : storm.level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(storm.center) > RADIUS * RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(storm.level, SephiriaDamage.magicIceAttack(storm.level, owner), storm.damage);
		}

		storm.level.sendParticles(ParticleTypes.SNOWFLAKE, storm.center.x, storm.center.y, storm.center.z,
				20, 1.0D, 1.0D, 1.0D, 0.02D);
		storm.level.playSound(null, storm.center.x, storm.center.y, storm.center.z,
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.35F, 1.4F);
	}

	private static void visual(Storm storm) {
		long elapsed = storm.level.getGameTime() - storm.startTick;
		double turn = TWO_PI * ((elapsed % ROTATION_TICKS) / (double) ROTATION_TICKS);

		// 一半雪片绕 Y 轴旋转（顺 / 逆各半）
		for (Orbit orbit : storm.orbits) {
			double angle = orbit.direction() * turn;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			double x = storm.center.x + orbit.x() * cos - orbit.z() * sin;
			double z = storm.center.z + orbit.x() * sin + orbit.z() * cos;
			double y = storm.center.y + orbit.y();
			storm.level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
		}

		// 另一半雪片保持初版：每刻在中心附近重新随机散布，不再用固定偏移随风暴中心平移
		storm.level.sendParticles(ParticleTypes.SNOWFLAKE, storm.center.x, storm.center.y, storm.center.z,
				SNOWFLAKE_COUNT - ROTATING_SNOWFLAKES, SNOWFLAKE_SPREAD, SNOWFLAKE_SPREAD,
				SNOWFLAKE_SPREAD, 0.0D);

		// 冰晶高光保持初版：每刻在中心 ±0.5R 的盒子里随机撒 8 个 glitter 高光。
		// 使用自定义 ICE_GLINT（0 重力），不会像原版 END_ROD 那样持续向下飘。
		storm.level.sendParticles(ModParticleTypes.ICE_GLINT, storm.center.x, storm.center.y, storm.center.z,
				END_ROD_COUNT, END_ROD_SPREAD, END_ROD_SPREAD, END_ROD_SPREAD, 0.0D);

		// 与雷霜蝶的暴风雪云同一套「云下飘雪」粒子；浓度随技能等级 100% → 300%
		int snowfall = (int) Math.round(32.0D * snowfallDensity(storm.skillLevel));
		storm.level.sendParticles(ModParticleTypes.SNOWFALL,
				storm.center.x, storm.center.y - 1.0D, storm.center.z,
				snowfall, RADIUS * 0.5D, 2.0D, RADIUS * 0.5D, 0.01D);
	}

	/** 初版特效里的一颗粒子：相对中心的固定偏移 + 旋转方向（0 = 不旋转）。 */
	private record Orbit(double x, double y, double z, int direction) {
	}

	/** 下雪浓度：0 级 1.0 倍，之后每级 +0.5，最高 3.0 倍。 */
	private static double snowfallDensity(int level) {
		return 1.0D + Math.min(4, Math.max(0, level)) * 0.5D;
	}

	private static final class Storm {
		private final ServerLevel level;
		private final ServerPlayer owner;
		private final float damage;
		private final int skillLevel;
		private final long endTick;
		private final long startTick;
		private final List<Orbit> orbits = new ArrayList<>();
		private long nextAttack;
		private long nextGround;
		private Vec3 center;

		private Storm(ServerLevel level, ServerPlayer owner, Vec3 center, float damage, int skillLevel,
				long endTick, long nextAttack) {
			this.level = level;
			this.owner = owner;
			this.damage = damage;
			this.skillLevel = skillLevel;
			this.endTick = endTick;
			this.nextAttack = nextAttack;
			this.center = center;
			this.startTick = level.getGameTime();
			this.nextGround = level.getGameTime();
			RandomSource random = level.getRandom();

			// 初版的雪片随机分布；冰晶高光不预生成，维持每刻随机撒的初版表现
			for (int index = 0; index < ROTATING_SNOWFLAKES; index++) {
				addOrbit(random, SNOWFLAKE_SPREAD, index);
			}
		}

		private void addOrbit(RandomSource random, double spread, int index) {
			double x = (random.nextDouble() * 2.0D - 1.0D) * spread;
			double y = (random.nextDouble() * 2.0D - 1.0D) * spread;
			double z = (random.nextDouble() * 2.0D - 1.0D) * spread;
			// 顺时针 / 逆时针交替
			int direction = index % 2 == 0 ? 1 : -1;
			this.orbits.add(new Orbit(x, y, z, direction));
		}
	}
}
