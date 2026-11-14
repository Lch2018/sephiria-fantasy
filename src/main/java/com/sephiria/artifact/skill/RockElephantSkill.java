package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.RockElephantItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 岩象（守护 + 风之歌，羁绊）：发动型技能，冷却 15 秒；每消耗一次冲刺减 3 秒。
 *
 * <p>从玩家周围 3 格内随机位置生成 3 根岩石矛，朝释放瞬间的准星方向直线飞 36 格
 * （与魔弹同距离）、沿途贯穿敌人，每根造成防御力 × 14/19/25/32/40% 的物理伤害。
 * 每额外 15% 攻击速度多一根矛。岩石矛是铜矛的物品展示实体，无任何粒子。
 */
public final class RockElephantSkill implements ArtifactSkill {
	public static final RockElephantSkill INSTANCE = new RockElephantSkill();

	private static final int COOLDOWN_TICKS = 300;
	private static final int DASH_COOLDOWN_REDUCTION_TICKS = 60;
	private static final int BASE_SPEARS = 3;
	private static final double ATTACK_SPEED_PER_SPEAR = 15.0D;
	private static final double SPEED = 3.0D;
	private static final double DISTANCE = 36.0D;
	private static final double HIT_RADIUS = 1.5D;
	private static final double SPAWN_RANGE = 3.0D;
	private static final double SPAWN_MIN = 2.0D;
	private static final double SPAWN_HEIGHT = 2.0D;
	private static final float SPEAR_SCALE = 1.5F;
	private static final float SPEAR_IN_PLANE_DEGREES = 135.0F;
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	private static final int INTERPOLATION_TICKS = 2;
	private static final List<Spear> SPEARS = new ArrayList<>();

	private RockElephantSkill() {
	}

	@Override
	public String id() {
		return "rock_elephant";
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
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int extra = (int) Math.floor(Math.max(0.0D,
				PlayerStats.attackSpeedTotal(player) - PlayerStats.DEFAULT_ATTACK_SPEED) / ATTACK_SPEED_PER_SPEAR);
		int count = BASE_SPEARS + extra;
		double damage = PlayerStats.defenseTotal(player) * RockElephantItem.damagePercentAt(level) / 100.0D;
		Vec3 look = player.getLookAngle().normalize();

		if (look.lengthSqr() < 1.0E-8D) {
			return;
		}

		for (int index = 0; index < count; index++) {
			Vec3 from = randomSpawn(serverLevel, player);
			Display.ItemDisplay body = spawnSpear(serverLevel, from);
			orientSpear(body, from, from.add(look));
			SPEARS.add(new Spear(serverLevel, player.getUUID(), from, look, (float) damage,
					0.0D, new HashSet<>(), body));
		}

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.65F, 1.0F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RockElephantSkill::tick);
	}

	/** 冲刺钩子：消耗一次冲刺就减 3 秒冷却（只减装着岩象的栏位）。 */
	public static void onDash(ServerPlayer player) {
		if (ArtifactEffects.highestLevelOf(player, com.sephiria.registry.ModItems.ROCK_ELEPHANT) < 0) {
			return;
		}

		ArtifactSkills.reduceCooldown(player, INSTANCE, DASH_COOLDOWN_REDUCTION_TICKS);
	}

	private static Vec3 randomSpawn(ServerLevel level, ServerPlayer player) {
		double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
		// 半径 2..3，避免直接生成在玩家脸上
		double radius = SPAWN_MIN + level.getRandom().nextDouble() * (SPAWN_RANGE - SPAWN_MIN);
		double height = (level.getRandom().nextDouble() - 0.5D) * 2.0D * SPAWN_HEIGHT;
		return new Vec3(player.getX() + Math.cos(angle) * radius,
				player.getY() + 1.0D + height,
				player.getZ() + Math.sin(angle) * radius);
	}

	private static Display.ItemDisplay spawnSpear(ServerLevel level, Vec3 at) {
		Display.ItemDisplay body = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		data.set(Display.ItemDisplay.DATA_ITEM_STACK_ID, new ItemStack(Items.COPPER_SPEAR));
		data.set(Display.ItemDisplay.DATA_ITEM_DISPLAY_ID, (byte) ItemDisplayContext.FIXED.getId());
		data.set(Display.DATA_SCALE_ID, new Vector3f(SPEAR_SCALE));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, INTERPOLATION_TICKS);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, INTERPOLATION_TICKS);
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		level.addFreshEntity(body);
		return body;
	}

	/** 与冰长矛同一套朝向：矛尖朝飞行方向；平直飞行不翻滚。 */
	private static void orientSpear(Display.ItemDisplay body, Vec3 from, Vec3 to) {
		if (body == null) {
			return;
		}

		Vec3 direction = to.subtract(from);

		if (direction.lengthSqr() < 1.0E-8D) {
			return;
		}

		Vec3 dir = direction.normalize();
		double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		float yaw = (float) Math.atan2(dir.x, dir.z);
		float pitch = (float) Math.atan2(dir.y, horizontal);
		Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(-pitch)
				.rotateY((float) (Math.PI / 2.0D))
				.rotateZ((float) Math.toRadians(SPEAR_IN_PLANE_DEGREES));
		body.getEntityData().set(Display.DATA_LEFT_ROTATION_ID, rotation);
	}

	private static void tick(MinecraftServer server) {
		if (SPEARS.isEmpty()) {
			return;
		}

		List<Spear> inFlight = new ArrayList<>(SPEARS);
		SPEARS.clear();

		for (Spear spear : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(spear.ownerId());

			if (owner == null || !owner.isAlive() || owner.level() != spear.level()) {
				removeBody(spear.body());
				continue;
			}

			double traveled = spear.traveled() + SPEED;
			Vec3 at = spear.from().add(spear.direction().scale(traveled));

			if (spear.body() != null) {
				spear.body().setPos(at.x, at.y, at.z);
			}

			AABB area = new AABB(at, at).inflate(HIT_RADIUS);

			for (LivingEntity victim : spear.level().getEntitiesOfClass(LivingEntity.class, area)) {
				if (!SephiriaDamage.strikeable(victim, owner) || !spear.hit().add(victim.getId())) {
					continue;
				}

				SephiriaDamage.clearDamageCooldown(victim);
				victim.hurtServer(spear.level(), SephiriaDamage.artifactDamage(spear.level(), owner),
						spear.damage());
			}

			if (traveled >= DISTANCE) {
				removeBody(spear.body());
				continue;
			}

			SPEARS.add(new Spear(spear.level(), spear.ownerId(), spear.from(), spear.direction(),
					spear.damage(), traveled, spear.hit(), spear.body()));
		}
	}

	private static void removeBody(Display.ItemDisplay body) {
		if (body != null) {
			body.discard();
		}
	}

	private record Spear(ServerLevel level, UUID ownerId, Vec3 from, Vec3 direction, float damage,
			double traveled, Set<Integer> hit, Display.ItemDisplay body) {
	}
}
