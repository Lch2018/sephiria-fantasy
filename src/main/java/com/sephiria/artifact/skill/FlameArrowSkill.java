package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.FlameArrowItem;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 火焰箭（高级魔法书）：朝准星方向齐射 4 枚火焰箭，夹角 8°，飞 36 格。
 *
 * <p>四枚箭共享同一个命中集合：敌人被任意一枚的范围扫到一次后，本次技能不再吃第二次伤害，
 * 所以「4 枚」只是技能的表现与覆盖范围，不是四份独立伤害。每枚箭沿路径结算半径 3 的球形 AOE，
 * 穿透敌人、不撞方块；首次命中时额外挂 2 层灼伤。
 */
public final class FlameArrowSkill implements ArtifactSkill {
	public static final FlameArrowSkill INSTANCE = new FlameArrowSkill();

	private static final int COOLDOWN_TICKS = 180;
	private static final double SPEED = 1.0D;
	private static final double DISTANCE = 36.0D;
	private static final double AOE_RADIUS = 3.0D;
	private static final float[] ANGLES = { -12.0F, -4.0F, 4.0F, 12.0F };

	private static final List<Arrow> ARROWS = new ArrayList<>();

	private FlameArrowSkill() {
	}

	@Override
	public String id() {
		return "flame_arrow";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return FlameArrowItem.mpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		double damage = (FlameArrowItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.FIRE) * FlameArrowItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		Set<Integer> hit = new HashSet<>();

		for (float angle : ANGLES) {
			Vec3 direction = look.yRot((float) Math.toRadians(angle)).normalize();
			Vec3 from = player.getEyePosition().add(direction.scale(0.5D));
			ARROWS.add(new Arrow(serverLevel, player.getUUID(), from, direction, 0.0D, (float) damage, hit));
		}

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
		serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
				16, 0.3D, 0.3D, 0.3D, 0.08D);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FlameArrowSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (ARROWS.isEmpty()) {
			return;
		}

		List<Arrow> inFlight = new ArrayList<>(ARROWS);
		ARROWS.clear();

		for (Arrow arrow : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(arrow.ownerId());

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			double next = arrow.traveled() + SPEED;
			Vec3 at = arrow.from().add(arrow.direction().scale(next));
			AABB area = new AABB(at, at).inflate(AOE_RADIUS);

			for (LivingEntity victim : arrow.level().getEntitiesOfClass(LivingEntity.class, area)) {
				if (!SephiriaDamage.strikeable(victim, owner)
						|| victim.position().distanceToSqr(at) > AOE_RADIUS * AOE_RADIUS
						|| !arrow.hit().add(victim.getId())) {
					continue;
				}

				SephiriaDamage.clearDamageCooldown(victim);
				victim.hurtServer(arrow.level(), SephiriaDamage.magicFireAttack(arrow.level(), owner), arrow.damage());
				// 「灼伤：2 层」：按现有灼伤入口叠两次，层数上限 / 熔岩珠额外层数照旧管着
				Debuffs.applyBurn(victim, owner);
				Debuffs.applyBurn(victim, owner);
			}

			trail(arrow.level(), at);

			if (next >= DISTANCE) {
				hitEffect(arrow.level(), at);
				continue;
			}

			ARROWS.add(new Arrow(arrow.level(), arrow.ownerId(), arrow.from(), arrow.direction(), next,
					arrow.damage(), arrow.hit()));
		}
	}

	private static void trail(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.06D, 0.06D, 0.06D, 0.0D);
	}

	private static void hitEffect(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 10, 0.35D, 0.35D, 0.35D, 0.03D);
	}

	private record Arrow(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, double traveled,
			float damage, Set<Integer> hit) {
	}
}
