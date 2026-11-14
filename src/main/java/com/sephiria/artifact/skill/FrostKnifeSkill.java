package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.FrostKnifeItem;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 霜刀（高级魔法书）：在玩家周围召唤 4 把霜刃，延迟 1 秒后各自锁定 28 格内最近的敌人并飞过去，
 * 命中 / 飞满 36 格时爆炸，半径 3 的球形范围结算冰属性伤害。
 *
 * <p>4 把霜刃是<b>独立的四次伤害</b>（与火焰箭相反）：每把爆炸都会把范围内的所有有威胁生物打一次，
 * 所以同一只敌人被 4 把同时炸到就吃 4 份伤害。没有可锁定的敌人时，每把按玩家当时的准星方向
 * 直飞 36 格后爆炸。
 */
public final class FrostKnifeSkill implements ArtifactSkill {
	public static final FrostKnifeSkill INSTANCE = new FrostKnifeSkill();

	private static final int COOLDOWN_TICKS = 200;
	private static final int COUNT = 4;
	private static final int DELAY_TICKS = 20;
	private static final double SPEED = 1.0D;
	private static final double FALLBACK_DISTANCE = 36.0D;
	private static final double LOCK_RANGE = 28.0D;
	private static final double AOE_RADIUS = 3.0D;

	private static final List<Knife> KNIVES = new ArrayList<>();

	private FrostKnifeSkill() {
	}

	@Override
	public String id() {
		return "frost_knife";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return FrostKnifeItem.mpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		double damage = (FrostKnifeItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.ICE) * FrostKnifeItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		Vec3 base = player.position();
		RandomSource random = serverLevel.getRandom();

		for (int index = 0; index < COUNT; index++) {
			double angle = Math.PI * 2.0D * index / COUNT + (random.nextDouble() - 0.5D) * 0.5D;
			double radius = 1.5D + random.nextDouble() * 1.5D;
			Vec3 at = base.add(Math.cos(angle) * radius, 1.0D + random.nextDouble() * 0.5D,
					Math.sin(angle) * radius);
			KNIVES.add(new Knife(serverLevel, player.getUUID(), at, player.getLookAngle().normalize(),
					DELAY_TICKS, 0.0D, false, null, (float) damage));
		}

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.2F);
		serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(),
				20, 1.8D, 0.8D, 1.8D, 0.02D);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FrostKnifeSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (KNIVES.isEmpty()) {
			return;
		}

		List<Knife> inFlight = new ArrayList<>(KNIVES);
		KNIVES.clear();

		for (Knife knife : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(knife.ownerId);

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			if (knife.delay > 0) {
				knife.delay--;
				swirl(knife.level, knife.pos);
				KNIVES.add(knife);
				continue;
			}

			if (!knife.launched) {
				knife.launched = true;
				knife.fallback = owner.getLookAngle().normalize();
				knife.target = nearestTarget(knife, owner);
			}

			if (knife.target != null && (!knife.target.isAlive() || knife.target.level() != knife.level)) {
				knife.target = null;
			}

			double step = SPEED;
			Vec3 move;

			if (knife.target != null) {
				Vec3 target = knife.target.getBoundingBox().getCenter();
				Vec3 delta = target.subtract(knife.pos);

				if (delta.lengthSqr() <= 1.44D) {
					explode(knife, owner, target);
					continue;
				}

				move = delta.normalize().scale(step);
			} else {
				move = knife.fallback.scale(step);
			}

			knife.pos = knife.pos.add(move);
			knife.traveled += step;
			trail(knife.level, knife.pos);

			if (knife.traveled >= FALLBACK_DISTANCE) {
				explode(knife, owner, knife.pos);
				continue;
			}

			KNIVES.add(knife);
		}
	}

	private static LivingEntity nearestTarget(Knife knife, ServerPlayer owner) {
		AABB area = new AABB(knife.pos, knife.pos).inflate(LOCK_RANGE);
		return knife.level.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> SephiriaDamage.autoTargetable(victim, owner))
				.filter(victim -> victim.position().distanceToSqr(knife.pos) <= LOCK_RANGE * LOCK_RANGE)
				.min(Comparator.comparingDouble(victim -> victim.position().distanceToSqr(knife.pos)))
				.orElse(null);
	}

	private static void explode(Knife knife, ServerPlayer owner, Vec3 center) {
		AABB area = new AABB(center, center).inflate(AOE_RADIUS);

		for (LivingEntity victim : knife.level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(center) > AOE_RADIUS * AOE_RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			victim.hurtServer(knife.level, SephiriaDamage.magicIceAttack(knife.level, owner), knife.damage);
		}

		knife.level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
				28, 1.2D, 1.2D, 1.2D, 0.05D);
		knife.level.playSound(null, center.x, center.y, center.z,
				SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);
	}

	private static void swirl(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 2, 0.1D, 0.1D, 0.1D, 0.01D);
	}

	private static void trail(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 3, 0.08D, 0.08D, 0.08D, 0.01D);
	}

	private static final class Knife {
		private final ServerLevel level;
		private final UUID ownerId;
		private Vec3 pos;
		private Vec3 fallback;
		private int delay;
		private double traveled;
		private boolean launched;
		private LivingEntity target;
		private final float damage;

		private Knife(ServerLevel level, UUID ownerId, Vec3 pos, Vec3 fallback, int delay, double traveled,
				boolean launched, LivingEntity target, float damage) {
			this.level = level;
			this.ownerId = ownerId;
			this.pos = pos;
			this.fallback = fallback;
			this.delay = delay;
			this.traveled = traveled;
			this.launched = launched;
			this.target = target;
			this.damage = damage;
		}
	}
}
