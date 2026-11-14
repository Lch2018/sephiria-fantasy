package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.LightningBoomerangItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 雷电回旋镖（高级魔法书）：向准星方向飞出 36 格，然后以折返点的玩家位置为 A 点直线飞回，
 * 返程长度为「到 A 点的距离 + 28」。
 *
 * <p>伤害范围是以回旋镖为中心、水平半径 6、垂直半高 3 的圆柱；去程与返程各能对同一敌人结算一次。
 * 返程中玩家（发动者）只要处在同一个圆柱范围内就算接住，对应技能栏的冷却立刻减半。
 */
public final class LightningBoomerangSkill implements ArtifactSkill {
	public static final LightningBoomerangSkill INSTANCE = new LightningBoomerangSkill();

	private static final int COOLDOWN_TICKS = 200;
	private static final double SPEED = 1.0D;
	private static final double OUTBOUND_DISTANCE = 36.0D;
	private static final double RETURN_EXTRA = 28.0D;
	private static final double DAMAGE_RADIUS = 6.0D;
	private static final double DAMAGE_HEIGHT = 3.0D;

	private static final List<Boomerang> BOOMERANGS = new ArrayList<>();

	private LightningBoomerangSkill() {
	}

	@Override
	public String id() {
		return "lightning_boomerang";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return LightningBoomerangItem.mpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 from = player.getEyePosition().add(look.scale(0.5D));
		double damage = (LightningBoomerangItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)
				* LightningBoomerangItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);

		BOOMERANGS.add(new Boomerang(serverLevel, player.getUUID(), from, look, (float) damage,
				new HashSet<>()));
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(LightningBoomerangSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (BOOMERANGS.isEmpty()) {
			return;
		}

		List<Boomerang> inFlight = new ArrayList<>(BOOMERANGS);
		BOOMERANGS.clear();

		for (Boomerang boomerang : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(boomerang.ownerId);

			if (owner == null || !owner.isAlive() || owner.level() != boomerang.level) {
				continue;
			}

			if (!boomerang.returning) {
				boomerang.outbound += SPEED;
				boomerang.pos = boomerang.from.add(boomerang.direction.scale(boomerang.outbound));

				strike(boomerang, owner);
				renderField(boomerang.level, boomerang.pos);

				if (boomerang.outbound >= OUTBOUND_DISTANCE) {
					turnAround(boomerang, owner);
				}

				BOOMERANGS.add(boomerang);
				continue;
			}

			boomerang.returned += SPEED;
			boomerang.pos = boomerang.turnPos.add(boomerang.returnDirection.scale(boomerang.returned));

			strike(boomerang, owner);
			renderField(boomerang.level, boomerang.pos);

			// 接住：只认发动者本人；判定范围与伤害范围一致（水平半径 6、垂直半高 3）
			if (canCatch(owner, boomerang.pos)) {
				ArtifactSkills.reduceCooldown(owner, INSTANCE, COOLDOWN_TICKS / 2);
				boomerang.level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
						SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.5F);
				continue;
			}

			if (boomerang.returned < boomerang.returnDistance) {
				BOOMERANGS.add(boomerang);
			}
		}
	}

	private static void turnAround(Boomerang boomerang, ServerPlayer owner) {
		Vec3 a = owner.position();
		Vec3 direction = a.subtract(boomerang.pos);

		if (direction.lengthSqr() < 1.0E-6D) {
			direction = boomerang.direction.reverse();
		}

		boomerang.returning = true;
		boomerang.turnPos = boomerang.pos;
		boomerang.returnDirection = direction.normalize();
		boomerang.returnDistance = boomerang.turnPos.distanceTo(a) + RETURN_EXTRA;
		// 去程已经打过的敌人，返程可以再吃一次（回旋镖来回两趟；每个阶段对同一敌人仍只结算一次）
		boomerang.hit.clear();
	}

	private static void strike(Boomerang boomerang, ServerPlayer owner) {
		AABB area = new AABB(boomerang.pos, boomerang.pos)
				.inflate(DAMAGE_RADIUS, DAMAGE_HEIGHT, DAMAGE_RADIUS);

		for (LivingEntity victim : boomerang.level.getEntitiesOfClass(LivingEntity.class, area)) {
			double dx = victim.getX() - boomerang.pos.x;
			double dz = victim.getZ() - boomerang.pos.z;

			if (!SephiriaDamage.strikeable(victim, owner)
					|| dx * dx + dz * dz > DAMAGE_RADIUS * DAMAGE_RADIUS
					|| Math.abs(victim.getY() - boomerang.pos.y) > DAMAGE_HEIGHT
					|| !boomerang.hit.add(victim.getId())) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(boomerang.level, SephiriaDamage.magicElectricAttack(boomerang.level, owner),
					boomerang.damage);
		}
	}

	/**
	 * 接住判定：与伤害范围同口径——水平半径 6、垂直半高 3。
	 *
	 * <p>以前只看 1.5 格，视觉上回旋镖离玩家还很远就“接住”了；现在玩家只要站在伤害圆柱里，
	 * 返回的回旋镖就能被接住。
	 */
	private static boolean canCatch(ServerPlayer owner, Vec3 at) {
		Vec3 center = owner.position().add(0.0D, 0.9D, 0.0D);
		double dx = center.x - at.x;
		double dz = center.z - at.z;
		return dx * dx + dz * dz <= DAMAGE_RADIUS * DAMAGE_RADIUS
				&& Math.abs(center.y - at.y) <= DAMAGE_HEIGHT;
	}

	/** 回旋镖周围的电场：铺满伤害圆柱，避免“特效很小、判定很大”。 */
	private static void renderField(ServerLevel level, Vec3 at) {
		// 中心亮芯
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.4D, 0.4D, 0.4D, 0.03D);
		// 与伤害范围一致的电场体积：水平半径 6、垂直半高 3
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 36,
				DAMAGE_RADIUS * 0.9D, DAMAGE_HEIGHT * 0.9D, DAMAGE_RADIUS * 0.9D, 0.03D);
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12,
				DAMAGE_RADIUS * 0.8D, DAMAGE_HEIGHT * 0.8D, DAMAGE_RADIUS * 0.8D, 0.02D);
	}

	private static final class Boomerang {
		private final ServerLevel level;
		private final UUID ownerId;
		private final Vec3 from;
		private final Vec3 direction;
		private final float damage;
		private final Set<Integer> hit;
		private Vec3 pos;
		private double outbound;
		private boolean returning;
		private Vec3 turnPos;
		private Vec3 returnDirection;
		private double returned;
		private double returnDistance;

		private Boomerang(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, float damage,
				Set<Integer> hit) {
			this.level = level;
			this.ownerId = ownerId;
			this.from = from;
			this.direction = direction;
			this.pos = from;
			this.damage = damage;
			this.hit = hit;
		}
	}
}
