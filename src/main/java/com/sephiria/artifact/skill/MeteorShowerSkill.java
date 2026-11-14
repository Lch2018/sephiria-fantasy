package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.MeteorShowerItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 陨石雨（传说魔法书）：准星在 48 格内指到方块 / 实体后，以命中点为中心在半径 18 的圆形区域内
 * 持续降下 30/35/40/45 颗陨石。
 *
 * <p>每颗陨石从高处落下，下落判定半径 4，落地 / 命中后半径 6 的火焰 AOE；每颗独立结算。
 * 落点平面上方 1 格之前可以穿墙，进入这 1 格以内恢复普通方块碰撞。
 * 准星没指到东西时 {@link #canCast} 返回 false，不扣蓝也不进冷却。
 */
public final class MeteorShowerSkill implements ArtifactSkill {
	public static final MeteorShowerSkill INSTANCE = new MeteorShowerSkill();

	private static final int COOLDOWN_TICKS = 520;
	private static final int SPAWN_INTERVAL = 2;
	private static final double FALL_SPEED = 2.0D;
	private static final double RAIN_RADIUS = 18.0D;
	private static final double HIT_RADIUS = 4.0D;
	private static final double AOE_RADIUS = 6.0D;
	private static final double MAX_RANGE = 48.0D;
	private static final double SPAWN_HEIGHT = 30.0D;
	/** 落到落点平面 1 格以内才开始检测墙体；更上面属于“可穿墙”的下落段。 */
	private static final double WALL_CHECK_HEIGHT = 1.0D;
	/** 陨石本体：红蛇之眼的岩浆块显示实体，尺寸取其 80%。 */
	private static final float BODY_SCALE = 1.15F * 0.8F;
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	private static final int BODY_INTERPOLATION_TICKS = 2;

	private static final List<Rain> RAINS = new ArrayList<>();
	private static final List<Meteor> METEORS = new ArrayList<>();

	private MeteorShowerSkill() {
	}

	@Override
	public String id() {
		return "meteor_shower";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return MeteorShowerItem.mpCost(level);
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

		double damage = (MeteorShowerItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.FIRE) * MeteorShowerItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		RAINS.add(new Rain(serverLevel, player.getUUID(), center, (float) damage, MeteorShowerItem.countAt(level)));

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MeteorShowerSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		tickRains(server);
		tickMeteors(server);
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

			if (rain.spawned >= rain.count) {
				return true;
			}

			if (++rain.timer >= SPAWN_INTERVAL) {
				rain.timer = 0;
				spawnMeteor(rain);
			}

			return false;
		});
	}

	private static void spawnMeteor(Rain rain) {
		RandomSource random = rain.level.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0D;
		double radius = Math.sqrt(random.nextDouble()) * RAIN_RADIUS;
		double x = rain.center.x + Math.cos(angle) * radius;
		double z = rain.center.z + Math.sin(angle) * radius;
		// 落点平面取命中点本身的 Y，而不是地表高度：洞穴里也能从上方穿过天花板落下来
		double groundY = rain.center.y;
		rain.spawned++;
		Vec3 from = new Vec3(x, groundY + SPAWN_HEIGHT, z);
		METEORS.add(new Meteor(rain.level, rain.ownerId, from, groundY, rain.damage,
				spawnBody(rain.level, from)));
	}

	private static void tickMeteors(MinecraftServer server) {
		if (METEORS.isEmpty()) {
			return;
		}

		List<Meteor> inFlight = new ArrayList<>(METEORS);
		METEORS.clear();

		for (Meteor meteor : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(meteor.ownerId);

			if (owner == null || !owner.isAlive()) {
				removeBody(meteor.body());
				continue;
			}

			Vec3 at = meteor.pos.add(0.0D, -FALL_SPEED, 0.0D);

			if (meteor.body() != null) {
				meteor.body().setPos(at.x, at.y, at.z);
			}

			// 落点平面上方 1 格之前可以穿墙；进入这 1 格以内恢复普通方块碰撞。
			// 只在跨过“平面 + 1”之后的这一段检测，避免把上方已经允许穿过的天花板算成碰撞。
			if (at.y <= meteor.groundY + WALL_CHECK_HEIGHT) {
				Vec3 checkFrom = wallCheckStart(meteor.pos, at, meteor.groundY + WALL_CHECK_HEIGHT);
				BlockHitResult block = meteor.level.clip(new ClipContext(checkFrom, at,
						ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));

				if (block.getType() != HitResult.Type.MISS) {
					removeBody(meteor.body());
					explode(meteor.level, owner, block.getLocation(), meteor.damage);
					continue;
				}
			}

			AABB hitBox = new AABB(at, at).inflate(HIT_RADIUS);
			boolean hit = false;

			for (LivingEntity victim : meteor.level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
				if (SephiriaDamage.strikeable(victim, owner)
						&& victim.position().distanceToSqr(at) <= HIT_RADIUS * HIT_RADIUS) {
					removeBody(meteor.body());
					explode(meteor.level, owner, at, meteor.damage);
					hit = true;
					break;
				}
			}

			if (hit) {
				continue;
			}

			if (at.y <= meteor.groundY + 0.2D) {
				removeBody(meteor.body());
				explode(meteor.level, owner, new Vec3(at.x, meteor.groundY + 0.1D, at.z), meteor.damage);
				continue;
			}

			trail(meteor.level, at);
			METEORS.add(new Meteor(meteor.level, meteor.ownerId, at, meteor.groundY, meteor.damage,
					meteor.body()));
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
			victim.hurtServer(level, SephiriaDamage.magicFireAttack(level, owner), damage);
		}

		level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
				30, 1.8D, 1.2D, 1.8D, 0.06D);
		level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z,
				10, 1.0D, 0.6D, 1.0D, 0.0D);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z,
				1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.playSound(null, center.x, center.y, center.z,
				SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 0.8F);
	}

	private static void trail(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.15D, 0.15D, 0.15D, 0.02D);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
		level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 2, 0.1D, 0.1D, 0.1D, 0.01D);
	}

	private static final class Rain {
		private final ServerLevel level;
		private final UUID ownerId;
		private final Vec3 center;
		private final float damage;
		private final int count;
		private int spawned;
		private int timer;

		private Rain(ServerLevel level, UUID ownerId, Vec3 center, float damage, int count) {
			this.level = level;
			this.ownerId = ownerId;
			this.center = center;
			this.damage = damage;
			this.count = count;
		}
	}

	private record Meteor(ServerLevel level, UUID ownerId, Vec3 pos, double groundY, float damage,
			Display.BlockDisplay body) {
	}

	/** 造一颗与红蛇之眼同款、但只有 80% 大小的岩浆块本体。 */
	private static Display.BlockDisplay spawnBody(ServerLevel level, Vec3 at) {
		Display.BlockDisplay body = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		data.set(Display.DATA_TRANSLATION_ID, new Vector3f(-0.5F, -0.5F, -0.5F));
		data.set(Display.DATA_SCALE_ID, new Vector3f(BODY_SCALE));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, BODY_INTERPOLATION_TICKS);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, BODY_INTERPOLATION_TICKS);
		data.set(Display.BlockDisplay.DATA_BLOCK_STATE_ID, Blocks.MAGMA_BLOCK.defaultBlockState());
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		level.addFreshEntity(body);
		return body;
	}

	private static void removeBody(Display.BlockDisplay body) {
		if (body != null) {
			body.discard();
		}
	}
}
