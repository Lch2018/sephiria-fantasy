package com.sephiria.artifact.skill;

import com.sephiria.artifact.ThunderVerdictItem;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModSounds;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 雷之裁决（魔法书「获得技能」）：向准星方向释放 8×8×30 的电击光束，攻击 10 次。
 *
 * <p>光束在释放那一刻定方向（跟着准星指的方向走，不追人），持续 20 刻、每 2 刻结算一击；
 * 每击对光束走廊里的每个敌人各打一次，单次伤害 = 6 + 触发者当前电元素强度面板值 ×
 * 50/70/80/90/90%（按神器等级）。
 *
 * <p>光束是<b>电属性伤害</b>（{@code Kind.ELECTRIC}）：吃麒麟的角的「电属性攻击的暴击几率」，
 * 也吃以后要做的魔法书加成；与其它电属性伤害一样，它不再触发电击之触、不吃减伤与黄金之手。
 * 同一目标要连吃 10 击，所以每击前清一次无敌帧，否则会被上一次的无敌帧吞掉。
 */
public final class ThunderVerdictSkill implements ArtifactSkill {
	public static final ThunderVerdictSkill INSTANCE = new ThunderVerdictSkill();

	/** 冷却 25 秒。 */
	private static final int COOLDOWN_TICKS = 500;
	/** 光束的结算次数。 */
	private static final int HITS = 10;
	/** 每几刻结算一击（10 击 × 2 刻 = 光束持续 20 刻）。 */
	private static final int TICKS_PER_HIT = 2;
	/** 光束长度（格）。 */
	private static final double LENGTH = 30.0D;
	/** 光束半宽（格）：宽高 8 格 → 以射线为轴 ±4 格。 */
	private static final double HALF_SIZE = 4.0D;
	/** 单次伤害的固定部分（点）。 */
	private static final double FLAT_DAMAGE = 6.0D;

	/** 一根正在释放的光束。 */
	private static final class Beam {
		private final UUID playerId;
		private final int level;
		private final Vec3 direction;
		private int ticksLeft = HITS * TICKS_PER_HIT;

		Beam(UUID playerId, int level, Vec3 direction) {
			this.playerId = playerId;
			this.level = level;
			this.direction = direction;
		}
	}

	private static final List<Beam> BEAMS = new ArrayList<>();

	private ThunderVerdictSkill() {
	}

	@Override
	public String id() {
		return "thunder_verdict";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return ThunderVerdictItem.thunderMpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		BEAMS.add(new Beam(player.getUUID(), level, player.getLookAngle()));

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35F, 1.6F);
	}

	/** 光束推进器（由 {@link com.sephiria.Sephiria#onInitialize()} 注册）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ThunderVerdictSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (BEAMS.isEmpty()) {
			return;
		}

		Iterator<Beam> iterator = BEAMS.iterator();

		while (iterator.hasNext()) {
			Beam beam = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(beam.playerId);

			if (player == null || !player.isAlive()) {
				iterator.remove();
				continue;
			}

			if (beam.ticksLeft % TICKS_PER_HIT == 0) {
				strike(player, beam);
			}

			drawBeam(player, beam);
			beam.ticksLeft--;

			if (beam.ticksLeft <= 0) {
				iterator.remove();
			}
		}
	}

	/** 一击：把光束走廊里的每个敌人各打一次（走廊 = 沿射线每格取一个 8×8×8 的方框）。 */
	private static void strike(ServerPlayer player, Beam beam) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		double element = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING);
		float damage = (float) (FLAT_DAMAGE + element * ThunderVerdictItem.damagePercent(beam.level) / 100.0D);
		DamageSource source = SephiriaDamage.electricAttack(level, player);
		Vec3 origin = player.getEyePosition();
		Set<LivingEntity> victims = new HashSet<>();

		for (double step = 0.0D; step <= LENGTH; step += 1.0D) {
			Vec3 point = origin.add(beam.direction.scale(step));
			AABB box = new AABB(point.x - HALF_SIZE, point.y - HALF_SIZE, point.z - HALF_SIZE,
					point.x + HALF_SIZE, point.y + HALF_SIZE, point.z + HALF_SIZE);
			victims.addAll(level.getEntitiesOfClass(LivingEntity.class, box));
		}

		for (LivingEntity victim : victims) {
			// 光束宽 8 格：只打有威胁的生物，和平生物不误伤（见 SephiriaDamage.strikeable）
			if (!SephiriaDamage.strikeable(victim, player)) {
				continue;
			}

			// 10 击要能全部落地：每击前清一次无敌帧（真实伤害 / 触电结算同款手法）
			victim.setInvulnerableTime(0);
			victim.hurtServer(level, source, damage);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				ModSounds.ELECTRIC_SPARK, SoundSource.PLAYERS, 0.4F,
				1.0F + level.getRandom().nextFloat() * 0.3F);
	}

	/** 光束的电流粒子：沿射线每 2 格撒一捧。 */
	private static void drawBeam(ServerPlayer player, Beam beam) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		Vec3 origin = player.getEyePosition();

		for (double step = 0.0D; step <= LENGTH; step += 2.0D) {
			Vec3 point = origin.add(beam.direction.scale(step));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
					2, 0.35D, 0.35D, 0.35D, 0.02D);
		}
	}
}
