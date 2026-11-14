package com.sephiria.artifact;

import com.sephiria.stats.PlayerStats;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.frost.FrostArmament;
import com.sephiria.registry.ModItems;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Unit;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 普利特维采的水滴（冰霜武具 + 湖泊，羁绊，【唯一】）：
 * 冰霜武具激活时，每次命中消耗 4 MP 召唤一把旋转冰剑（最多 5 把，每把持续 8 秒）。
 *
 * <p>冰剑以玩家为圆心、半径 2 旋转，1 秒一圈；每把剑每圈对同一敌人最多造成一次伤害。
 * 消失时模型会射向 8 格内最近的敌人，飞抵后才结算伤害；没有目标就原地消散。
 *
 * <p>永恒蚀在赛菲莉娅背包左半边时，整把剑跟着冰霜武具一起改成火属性：伤害按火元素强度结算、
 * 类型换成 {@code fire_attack}，箭模型与命中粒子也从冰蓝换成焰黄。
 */
public class PlitviceWaterDropItem extends ArtifactItem {
	private static final int MAX_SWORDS = 5;
	private static final int DURATION_TICKS = 160;
	private static final int REVOLUTION_TICKS = 20;
	private static final double ORBIT_RADIUS = 2.0D;
	private static final double HIT_RADIUS = 1.2D;
	private static final double FINAL_RANGE = 8.0D;
	private static final int FINAL_FLIGHT_TICKS = 4;
	private static final double MP_COST = 4.0D;
	private static final float ARROW_SCALE = 0.6F;
	private static final float ARROW_IN_PLANE_DEGREES = 135.0F;
	private static final int BRIGHTNESS_OVERRIDE = 0xF000F0;
	private static final int INTERPOLATION_TICKS = 2;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private static final Map<UUID, List<Sword>> SWORDS = new HashMap<>();
	/** 已经离轨、正在飞向目标的剑。飞完才结算伤害，保证模型真的“射”出去。 */
	private static final List<Shot> SHOTS = new ArrayList<>();

	public PlitviceWaterDropItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.FROST_ARMAMENT, ArtifactCombo.LAKE);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.plitvice_water_drop.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 0;
	}

	@Override
	public java.util.List<net.minecraft.network.chat.Component> affixLines(int level) {
		return java.util.List.of(
				net.minecraft.network.chat.Component.translatable(
						"artifact.sephiria_fantasy.affix.pritvice_water_drop"),
				net.minecraft.network.chat.Component.translatable(
						"artifact.sephiria_fantasy.affix.pritvice_water_drop_damage"));
	}

	/** 冰霜武具命中时调用：消耗 4 MP 加一把剑。 */
	public static void onFrostArmamentHit(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		if (ArtifactEffects.highestLevelOf(player, ModItems.PRITVICE_WATER_DROP) < 0) {
			return;
		}

		int combo = ArtifactEffects.comboLevels(player)
				.getOrDefault(ArtifactCombo.FROST_ARMAMENT, 0);

		if (combo < 2) {
			return;
		}

		List<Sword> swords = SWORDS.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>());

		if (swords.size() >= MAX_SWORDS) {
			return;
		}

		if (!PlayerStats.spendMp(player, MP_COST)) {
			return;
		}

		long now = level.getGameTime();
		double offset = level.getRandom().nextDouble() * Math.PI * 2.0D;
		double angle = now % REVOLUTION_TICKS / (double) REVOLUTION_TICKS * Math.PI * 2.0D + offset;
		Vec3 at = orbitPos(player, angle);
		boolean flame = FrostArmament.flameMode(player);
		Display.ItemDisplay body = spawnArrowBody(level, at, flame);
		orientArrow(body, at, angle);
		swords.add(new Sword(offset, now, now + DURATION_TICKS, new HashSet<>(), -1L, flame, body));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.6F, 1.5F);
	}

	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(PlitviceWaterDropItem::tick);
	}

	public static void forget(ServerPlayer player) {
		List<Sword> swords = SWORDS.remove(player.getUUID());

		if (swords != null) {
			for (Sword sword : swords) {
				removeBody(sword.body());
			}
		}

		SHOTS.removeIf(shot -> {
			if (!shot.ownerId().equals(player.getUUID())) {
				return false;
			}

			removeBody(shot.body());
			return true;
		});
	}

	private static void tick(MinecraftServer server) {
		tickShots(server);

		if (SWORDS.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			List<Sword> list = SWORDS.get(player.getUUID());

			if (list == null || list.isEmpty()) {
				continue;
			}

			if (!(player.level() instanceof ServerLevel level)) {
				continue;
			}

			long now = level.getGameTime();
			double base = (now % REVOLUTION_TICKS) / (double) REVOLUTION_TICKS * Math.PI * 2.0D;
			boolean flame = FrostArmament.flameMode(player);
			double damage = PlayerStats.elementTotal(player,
					flame ? PlayerStats.Element.FIRE : PlayerStats.Element.ICE)
					* 0.06D * PlayerStats.maxMpTotal(player) * 0.05D;
			net.minecraft.world.damagesource.DamageSource damageSource = flame
					? SephiriaDamage.fireAttack(level, player)
					: SephiriaDamage.iceAttack(level, player);
			List<Sword> kept = new ArrayList<>(list.size());

			for (Sword sword : list) {
				if (now >= sword.expireTick()) {
					startShot(level, player, sword, damage, flame);
					continue;
				}

				long revolution = (now - sword.spawnTick()) / REVOLUTION_TICKS;

				if (revolution != sword.lastRevolution()) {
					sword.hit().clear();
					sword = new Sword(sword.offset(), sword.spawnTick(), sword.expireTick(),
							sword.hit(), revolution, sword.flame(), sword.body());
				}

				// 永恒蚀换边时，已经在轨道上的剑也跟着换模型与属性
				if (flame != sword.flame()) {
					sword = new Sword(sword.offset(), sword.spawnTick(), sword.expireTick(),
							sword.hit(), sword.lastRevolution(), flame, sword.body());
					setArrowVariant(sword.body(), flame);
				}

				double angle = base + sword.offset();
				Vec3 swordPos = orbitPos(player, angle);

				if (sword.body() != null) {
					sword.body().setPos(swordPos.x, swordPos.y, swordPos.z);
					orientArrow(sword.body(), swordPos, angle);
				}

				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
						new net.minecraft.world.phys.AABB(swordPos.x - HIT_RADIUS, swordPos.y - HIT_RADIUS,
								swordPos.z - HIT_RADIUS, swordPos.x + HIT_RADIUS, swordPos.y + HIT_RADIUS,
								swordPos.z + HIT_RADIUS))) {
					if (!victim.getBoundingBox().inflate(HIT_RADIUS).contains(swordPos)
							|| !SephiriaDamage.strikeable(victim, player)
							|| !sword.hit().add(victim.getUUID())) {
						continue;
					}

					SephiriaDamage.clearDamageCooldown(victim);
					victim.hurtServer(level, damageSource, (float) damage);
				}

				kept.add(sword);
			}

			if (kept.isEmpty()) {
				SWORDS.remove(player.getUUID());
			} else {
				SWORDS.put(player.getUUID(), kept);
			}
		}
	}

	/**
	 * 冰剑到期：锁定 8 格内最近的敌人，把现有模型交给 {@link Shot} 飞过去。
	 * 没有目标就直接消散，不产生伤害。
	 */
	private static void startShot(ServerLevel level, ServerPlayer player, Sword sword, double damage,
			boolean flame) {
		setArrowVariant(sword.body(), flame);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				new net.minecraft.world.phys.AABB(player.position(), player.position()).inflate(FINAL_RANGE),
				victim -> SephiriaDamage.strikeable(victim, player)
						&& victim.distanceToSqr(player) <= FINAL_RANGE * FINAL_RANGE);

		LivingEntity target = targets.stream()
				.min(java.util.Comparator.comparingDouble(victim -> victim.distanceToSqr(player)))
				.orElse(null);

		if (target == null) {
			removeBody(sword.body());
			return;
		}

		Vec3 from = sword.body() != null ? sword.body().position() : orbitPos(player, 0.0D);
		Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
		SHOTS.add(new Shot(level, player.getUUID(), target.getUUID(), from, to, damage, flame, 0,
				sword.body()));
	}

	/** 每 tick 让飞行中的剑靠近目标一步；到达的那个 tick 才结算伤害并移除模型。 */
	private static void tickShots(MinecraftServer server) {
		if (SHOTS.isEmpty()) {
			return;
		}

		List<Shot> inFlight = new ArrayList<>(SHOTS);
		SHOTS.clear();

		for (Shot shot : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(shot.ownerId());

			if (owner == null || !owner.isAlive() || owner.level() != shot.level()) {
				removeBody(shot.body());
				continue;
			}

			// 目标可能中途死亡或失去可攻击状态，此时仍让模型飞完最后一段再消失
			var entity = shot.level().getEntity(shot.targetId());
			LivingEntity target = entity instanceof LivingEntity living
					&& living.isAlive() && SephiriaDamage.strikeable(living, owner) ? living : null;
			Vec3 to = target == null ? shot.to()
					: target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			int next = shot.tick() + 1;

			if (next >= FINAL_FLIGHT_TICKS) {
				if (shot.body() != null) {
					shot.body().setPos(to.x, to.y, to.z);
				}
				removeBody(shot.body());

				if (target != null) {
					SephiriaDamage.clearDamageCooldown(target);
					target.hurtServer(shot.level(), shot.flame()
							? SephiriaDamage.fireAttack(shot.level(), owner)
							: SephiriaDamage.iceAttack(shot.level(), owner),
							(float) shot.damage());
					shot.level().sendParticles(shot.flame() ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
							to.x, to.y, to.z, 18, 0.4D, 0.5D, 0.4D, 0.08D);
					shot.level().playSound(null, to.x, to.y, to.z,
							SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5F, 1.6F);
				}

				continue;
			}

			double progress = next / (double) FINAL_FLIGHT_TICKS;
			Vec3 pos = shot.from().lerp(to, progress);

			if (shot.body() != null) {
				shot.body().setPos(pos.x, pos.y, pos.z);
				orientFlight(shot.body(), to.subtract(shot.from()));
			}

			SHOTS.add(new Shot(shot.level(), shot.ownerId(), shot.targetId(), shot.from(), to,
					shot.damage(), shot.flame(), next, shot.body()));
		}
	}

	private record Sword(double offset, long spawnTick, long expireTick, Set<UUID> hit, long lastRevolution,
			boolean flame, Display.ItemDisplay body) {
	}

	private record Shot(ServerLevel level, UUID ownerId, UUID targetId, Vec3 from, Vec3 to, double damage,
			boolean flame, int tick, Display.ItemDisplay body) {
	}

	private static Vec3 orbitPos(ServerPlayer player, double angle) {
		return new Vec3(player.getX() + Math.cos(angle) * ORBIT_RADIUS, player.getY() + 1.2D,
				player.getZ() + Math.sin(angle) * ORBIT_RADIUS);
	}

	private static Display.ItemDisplay spawnArrowBody(ServerLevel level, Vec3 at, boolean flame) {
		Display.ItemDisplay body = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);

		if (body == null) {
			return null;
		}

		SynchedEntityData data = body.getEntityData();
		data.set(Display.ItemDisplay.DATA_ITEM_STACK_ID, arrowStack(flame));
		data.set(Display.ItemDisplay.DATA_ITEM_DISPLAY_ID, (byte) ItemDisplayContext.FIXED.getId());
		data.set(Display.DATA_SCALE_ID, new Vector3f(ARROW_SCALE));
		data.set(Display.DATA_BRIGHTNESS_OVERRIDE_ID, BRIGHTNESS_OVERRIDE);
		data.set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, INTERPOLATION_TICKS);
		data.set(Display.DATA_POS_ROT_INTERPOLATION_DURATION_ID, INTERPOLATION_TICKS);
		body.setPos(at.x, at.y, at.z);
		body.setNoGravity(true);
		body.setSilent(true);
		level.addFreshEntity(body);
		return body;
	}

	private static void orientArrow(Display.ItemDisplay body, Vec3 at, double angle) {
		if (body == null) {
			return;
		}

		// 单位圆的切线方向：冰剑沿圆周运动，箭杆朝切线
		Vec3 tangent = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle));
		float yaw = (float) Math.atan2(tangent.x, tangent.z);
		Quaternionf rotation = new Quaternionf().rotateY(yaw)
				.rotateY((float) (Math.PI / 2.0D))
				.rotateZ((float) Math.toRadians(ARROW_IN_PLANE_DEGREES));
		body.getEntityData().set(Display.DATA_LEFT_ROTATION_ID, rotation);
	}

	private static ItemStack arrowStack(boolean flame) {
		ItemStack stack = new ItemStack(ModItems.ICE_ARROW);

		if (flame) {
			// 与冰霜武具的飞行体同一套：模型定义按 FLAME_TOOL 挑焰黄贴图
			stack.set(FrostArmament.FLAME_TOOL, Unit.INSTANCE);
		}

		return stack;
	}

	private static void setArrowVariant(Display.ItemDisplay body, boolean flame) {
		if (body != null) {
			body.getEntityData().set(Display.ItemDisplay.DATA_ITEM_STACK_ID, arrowStack(flame));
		}
	}

	/** 朝水平飞行方向摆正箭杆；俯仰忽略，保持冰剑的平面造型。 */
	private static void orientFlight(Display.ItemDisplay body, Vec3 direction) {
		if (body == null || direction.x * direction.x + direction.z * direction.z < 1.0E-6D) {
			return;
		}

		double angle = Math.atan2(-direction.x, direction.z);
		orientArrow(body, body.position(), angle);
	}

	private static void removeBody(Display.ItemDisplay body) {
		if (body != null) {
			body.discard();
		}
	}
}
