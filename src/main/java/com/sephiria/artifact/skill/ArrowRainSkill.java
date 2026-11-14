package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.ArrowRainItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.debuff.Debuffs;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 箭雨（稀有魔法书）：以准星指向的方块 / 实体为中心，在半径 8 的圆形区域内持续降下 45 支冰箭。
 *
 * <p>每支冰箭从目标点上方落下：下落时碰到半径 2 内的敌人、或落地时爆炸，半径 3 的冰属性 AOE；
 * 落点平面上方 1 格之前可以穿墙，进入这 1 格以内恢复普通方块碰撞。
 * 每支箭独立结算（多段伤害），命中敌人有 66% 概率附加冻伤。准星在 48 格内没有命中方块 / 实体时
 * {@link #canCast} 返回 false，技能不会扣蓝也不会进冷却。
 */
public final class ArrowRainSkill implements ArtifactSkill {
	public static final ArrowRainSkill INSTANCE = new ArrowRainSkill();

	private static final int COOLDOWN_TICKS = 320;
	private static final int ARROW_COUNT = 45;
	private static final int SPAWN_INTERVAL = 2;
	private static final double FALL_SPEED = 1.5D;
	private static final double RAIN_RADIUS = 8.0D;
	private static final double HIT_RADIUS = 2.0D;
	private static final double AOE_RADIUS = 3.0D;
	private static final double MAX_RANGE = 48.0D;
	private static final double SPAWN_HEIGHT = 20.0D;
	/** 落到落点平面 1 格以内才开始检测墙体；更上面属于“可穿墙”的下落段。 */
	private static final double WALL_CHECK_HEIGHT = 1.0D;
	private static final double FROSTBITE_CHANCE = 66.0D;

	private static final List<Rain> RAINS = new ArrayList<>();
	private static final List<Arrow> ARROWS = new ArrayList<>();

	private ArrowRainSkill() {
	}

	@Override
	public String id() {
		return "arrow_rain";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return ArrowRainItem.mpCost(level);
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

		double damage = (ArrowRainItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.ICE) * ArrowRainItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		RAINS.add(new Rain(serverLevel, player.getUUID(), center, (float) damage));

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9F, 1.0F);
		serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + SPAWN_HEIGHT, center.z,
				40, RAIN_RADIUS, 1.0D, RAIN_RADIUS, 0.02D);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ArrowRainSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		tickRains(server);
		tickArrows(server);
	}

	private static void tickRains(MinecraftServer server) {
		if (RAINS.isEmpty()) {
			return;
		}

		RAINS.removeIf(rain -> {
			ServerPlayer owner = server.getPlayerList().getPlayer(rain.ownerId);

			if (owner == null || !owner.isAlive()) {
				return true;
			}

			if (rain.spawned >= ARROW_COUNT) {
				return true;
			}

			if (++rain.timer >= SPAWN_INTERVAL) {
				rain.timer = 0;
				spawnArrow(rain);
			}

			return false;
		});
	}

	private static void spawnArrow(Rain rain) {
		RandomSource random = rain.level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = Math.sqrt(random.nextDouble()) * RAIN_RADIUS;
		double x = rain.center.x + Math.cos(angle) * radius;
		double z = rain.center.z + Math.sin(angle) * radius;
		// 落点平面取命中点本身的 Y，而不是地表高度：洞穴里也能从上方穿过天花板落下来
		double groundY = rain.center.y;
		rain.spawned++;
		ARROWS.add(new Arrow(rain.level, rain.ownerId, new Vec3(x, groundY + SPAWN_HEIGHT, z),
				groundY, rain.damage));
	}

	private static void tickArrows(MinecraftServer server) {
		if (ARROWS.isEmpty()) {
			return;
		}

		List<Arrow> inFlight = new ArrayList<>(ARROWS);
		ARROWS.clear();

		for (Arrow arrow : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(arrow.ownerId);

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			Vec3 at = arrow.pos.add(0.0D, -FALL_SPEED, 0.0D);

			// 落点平面上方 1 格之前可以穿墙；进入这 1 格以内恢复普通方块碰撞。
			// 只在跨过“平面 + 1”之后的这一段检测，避免把上方已经允许穿过的天花板算成碰撞。
			if (at.y <= arrow.groundY + WALL_CHECK_HEIGHT) {
				Vec3 checkFrom = wallCheckStart(arrow.pos, at, arrow.groundY + WALL_CHECK_HEIGHT);
				BlockHitResult block = arrow.level.clip(new ClipContext(checkFrom, at,
						ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));

				if (block.getType() != HitResult.Type.MISS) {
					explode(arrow.level, owner, block.getLocation(), arrow.damage);
					continue;
				}
			}

			AABB hitBox = new AABB(at.x - HIT_RADIUS, at.y - HIT_RADIUS, at.z - HIT_RADIUS,
					at.x + HIT_RADIUS, at.y + HIT_RADIUS, at.z + HIT_RADIUS);
			boolean hit = false;

			for (LivingEntity victim : arrow.level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
				if (SephiriaDamage.strikeable(victim, owner)
						&& victim.position().distanceToSqr(at) <= HIT_RADIUS * HIT_RADIUS) {
					explode(arrow.level, owner, at, arrow.damage);
					hit = true;
					break;
				}
			}

			if (hit) {
				continue;
			}

			if (at.y <= arrow.groundY + 0.2D) {
				explode(arrow.level, owner, new Vec3(at.x, arrow.groundY + 0.1D, at.z), arrow.damage);
				continue;
			}

			trail(arrow.level, at);
			ARROWS.add(new Arrow(arrow.level, arrow.ownerId, at, arrow.groundY, arrow.damage));
		}
	}

	/** 下落段与「平面 + 1」的交点；之前的部分允许穿墙，不参与方块碰撞。 */
	private static Vec3 wallCheckStart(Vec3 from, Vec3 to, double plane) {
		if (from.y <= plane) {
			return from;
		}

		double t = (plane - from.y) / (to.y - from.y);
		return new Vec3(from.x + (to.x - from.x) * t, plane, from.z + (to.z - from.z) * t);
	}

	private static void explode(ServerLevel level, ServerPlayer owner, Vec3 center, float damage) {
		AABB area = new AABB(center, center).inflate(AOE_RADIUS);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(center) > AOE_RADIUS * AOE_RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(level, SephiriaDamage.magicIceAttack(level, owner), damage);

			if (level.getRandom().nextDouble() * 100.0D < FROSTBITE_CHANCE) {
				Debuffs.applyFrostbite(victim, owner);
			}
		}

		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
				18, 1.1D, 0.8D, 1.1D, 0.04D);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
	}

	private static void trail(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 2, 0.08D, 0.1D, 0.08D, 0.01D);
	}

	private static final class Rain {
		private final ServerLevel level;
		private final UUID ownerId;
		private final Vec3 center;
		private final float damage;
		private int spawned;
		private int timer;

		private Rain(ServerLevel level, UUID ownerId, Vec3 center, float damage) {
			this.level = level;
			this.ownerId = ownerId;
			this.center = center;
			this.damage = damage;
		}
	}

	private record Arrow(ServerLevel level, UUID ownerId, Vec3 pos, double groundY, float damage) {
	}
}
