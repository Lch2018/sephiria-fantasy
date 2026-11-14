package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.FireCircusItem;
import com.sephiria.damage.SephiriaDamage;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 火焰马戏团（稀有魔法书）：在玩家周围 3 格随机召唤 8 个火焰球，延迟 1 秒后沿准星方向发射 36 格。
 *
 * <p>每个火球独立，命中第一个有威胁生物或飞满 36 格时爆炸，半径 3 的球形 AOE；
 * 8 个火球可以重复炸到同一只敌人，是独立的多段伤害。暴击率与暴击伤害额外乘
 * (1 + 神器等级的暴击加成)，由 {@link SephiriaDamage#withFireCircusCrit} 只包住这一发火球的结算。
 */
public final class FireCircusSkill implements ArtifactSkill {
	public static final FireCircusSkill INSTANCE = new FireCircusSkill();

	private static final int COOLDOWN_TICKS = 200;
	private static final int COUNT = 8;
	private static final int DELAY_TICKS = 20;
	private static final double SPEED = 1.0D;
	private static final double DISTANCE = 36.0D;
	private static final double AOE_RADIUS = 3.0D;

	private static final List<Fireball> FIREBALLS = new ArrayList<>();

	private FireCircusSkill() {
	}

	@Override
	public String id() {
		return "fire_circus";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return FireCircusItem.mpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		double damage = (FireCircusItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.FIRE) * FireCircusItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		double critAmp = FireCircusItem.critBonus(level);
		Vec3 base = player.position();
		RandomSource random = serverLevel.getRandom();

		for (int index = 0; index < COUNT; index++) {
			double angle = Math.PI * 2.0D * index / COUNT + (random.nextDouble() - 0.5D) * 0.5D;
			double radius = 1.5D + random.nextDouble() * 1.5D;
			Vec3 at = base.add(Math.cos(angle) * radius, 1.0D + random.nextDouble() * 0.5D,
					Math.sin(angle) * radius);
			FIREBALLS.add(new Fireball(serverLevel, player.getUUID(), at, player.getLookAngle().normalize(),
					DELAY_TICKS, 0.0D, (float) damage, critAmp));
		}

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.1F);
		serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
				28, 2.0D, 0.8D, 2.0D, 0.05D);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FireCircusSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (FIREBALLS.isEmpty()) {
			return;
		}

		List<Fireball> inFlight = new ArrayList<>(FIREBALLS);
		FIREBALLS.clear();

		for (Fireball fireball : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(fireball.ownerId);

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			if (fireball.delay > 0) {
				fireball.delay--;

				if (fireball.delay == 0) {
					// 延迟结束那一刻才锁定准星，玩家在这一秒里转身也能改方向
					fireball.direction = owner.getLookAngle().normalize();
				}

				swirl(fireball.level, fireball.pos);
				FIREBALLS.add(fireball);
				continue;
			}

			fireball.pos = fireball.pos.add(fireball.direction.scale(SPEED));
			fireball.traveled += SPEED;
			trail(fireball.level, fireball.pos);

			if (fireball.traveled >= DISTANCE) {
				explode(fireball, owner, fireball.pos);
				continue;
			}

			boolean hit = false;

			for (LivingEntity victim : fireball.level.getEntitiesOfClass(LivingEntity.class,
					new AABB(fireball.pos, fireball.pos).inflate(AOE_RADIUS))) {
				if (SephiriaDamage.strikeable(victim, owner)
						&& victim.position().distanceToSqr(fireball.pos) <= AOE_RADIUS * AOE_RADIUS) {
					explode(fireball, owner, fireball.pos);
					hit = true;
					break;
				}
			}

			if (!hit) {
				FIREBALLS.add(fireball);
			}
		}
	}

	private static void explode(Fireball fireball, ServerPlayer owner, Vec3 center) {
		AABB area = new AABB(center, center).inflate(AOE_RADIUS);

		for (LivingEntity victim : fireball.level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(center) > AOE_RADIUS * AOE_RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			SephiriaDamage.withFireCircusCrit(owner, fireball.critAmp,
					() -> victim.hurtServer(fireball.level,
							SephiriaDamage.magicFireAttack(fireball.level, owner), fireball.damage));
		}

		fireball.level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
				26, 1.0D, 1.0D, 1.0D, 0.04D);
		fireball.level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z,
				6, 0.6D, 0.6D, 0.6D, 0.0D);
		fireball.level.playSound(null, center.x, center.y, center.z,
				SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	private static void swirl(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 3, 0.12D, 0.12D, 0.12D, 0.01D);
	}

	private static void trail(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.1D, 0.1D, 0.1D, 0.01D);
	}

	private static final class Fireball {
		private final ServerLevel level;
		private final UUID ownerId;
		private Vec3 pos;
		private Vec3 direction;
		private int delay;
		private double traveled;
		private final float damage;
		private final double critAmp;

		private Fireball(ServerLevel level, UUID ownerId, Vec3 pos, Vec3 direction, int delay, double traveled,
				float damage, double critAmp) {
			this.level = level;
			this.ownerId = ownerId;
			this.pos = pos;
			this.direction = direction;
			this.delay = delay;
			this.traveled = traveled;
			this.damage = damage;
			this.critAmp = critAmp;
		}
	}
}
