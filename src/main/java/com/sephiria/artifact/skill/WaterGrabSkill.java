package com.sephiria.artifact.skill;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 水之精灵的 WaterGrab：准星 24 格内指到方块 / 实体后，消耗 30% 最大 MP，
 * 在命中点生成水球，1 秒内膨胀到 300% 后爆炸，对半径 8 的球形范围造成物理伤害。
 */
public final class WaterGrabSkill implements ArtifactSkill {
	public static final WaterGrabSkill INSTANCE = new WaterGrabSkill();

	private static final int COOLDOWN_TICKS = 160;
	private static final int CHARGE_TICKS = 20;
	private static final double MAX_RANGE = 24.0D;
	private static final double RADIUS = 8.0D;
	private static final double MP_PERCENT = 30.0D;
	private static final double[] DAMAGE_PERCENT_BY_LEVEL = { 32.0D, 48.0D, 64.0D, 80.0D, 100.0D };
	private static final double[] CRIT_BY_LEVEL = { 10.0D, 15.0D, 20.0D, 25.0D, 30.0D };

	private static final List<Grab> GRABS = new ArrayList<>();

	private WaterGrabSkill() {
	}

	@Override
	public String id() {
		return "water_grab";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return 0.0D;
	}

	@Override
	public double mpCost(ServerPlayer player, int level) {
		return PlayerStats.maxMpTotal(player) * MP_PERCENT / 100.0D;
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
			Entity entity = entityHit.getEntity();
			center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
		} else {
			center = hit.getLocation();
		}

		int clamped = Math.max(0, Math.min(DAMAGE_PERCENT_BY_LEVEL.length - 1, level));
		double damage = PlayerStats.maxMpTotal(player) * DAMAGE_PERCENT_BY_LEVEL[clamped] / 100.0D;
		GRABS.add(new Grab(serverLevel, player.getUUID(), center, (float) damage,
				CRIT_BY_LEVEL[clamped], 0));
		serverLevel.playSound(null, center.x, center.y, center.z,
				SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(WaterGrabSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (GRABS.isEmpty()) {
			return;
		}

		List<Grab> inFlight = new ArrayList<>(GRABS);
		GRABS.clear();

		for (Grab grab : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(grab.ownerId());

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			if (grab.tick() < CHARGE_TICKS) {
				visual(grab);
				GRABS.add(new Grab(grab.level(), grab.ownerId(), grab.center(), grab.damage(),
						grab.critBonus(), grab.tick() + 1));
				continue;
			}

			explode(grab, owner);
		}
	}

	private static void visual(Grab grab) {
		double progress = grab.tick() / (double) CHARGE_TICKS;
		double radius = 1.0D + progress * 2.0D;
		grab.level().sendParticles(ParticleTypes.SPLASH, grab.center().x, grab.center().y, grab.center().z,
				10 + (int) (progress * 18), radius * 0.6D, radius * 0.6D, radius * 0.6D, 0.02D);
		grab.level().sendParticles(ParticleTypes.BUBBLE, grab.center().x, grab.center().y, grab.center().z,
				6 + (int) (progress * 12), radius * 0.5D, radius * 0.5D, radius * 0.5D, 0.0D);
	}

	private static void explode(Grab grab, ServerPlayer owner) {
		AABB area = new AABB(grab.center(), grab.center()).inflate(RADIUS);

		for (LivingEntity victim : grab.level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(grab.center()) > RADIUS * RADIUS) {
				continue;
			}

			SephiriaDamage.clearDamageCooldown(victim);
			SephiriaDamage.withWaterGrabCrit(owner, grab.critBonus(),
					() -> victim.hurtServer(grab.level(), SephiriaDamage.waterGrab(grab.level(), owner),
							grab.damage()));
		}

		grab.level().sendParticles(ParticleTypes.SPLASH, grab.center().x, grab.center().y, grab.center().z,
				90, 3.2D, 3.2D, 3.2D, 0.25D);
		grab.level().sendParticles(ParticleTypes.BUBBLE, grab.center().x, grab.center().y, grab.center().z,
				50, 3.0D, 3.0D, 3.0D, 0.1D);
		grab.level().playSound(null, grab.center().x, grab.center().y, grab.center().z,
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.1F);
	}

	private record Grab(ServerLevel level, UUID ownerId, Vec3 center, float damage, double critBonus, int tick) {
	}
}
