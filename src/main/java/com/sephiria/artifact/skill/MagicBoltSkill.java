package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ColorParticleOption;
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
import java.util.List;
import java.util.UUID;

/**
 * 火焰 / 寒冰 / 闪电魔弹（三本普通魔法书）：向准星方向投出小型元素投射物。
 *
 * <p>三者的飞行规则一致：从眼睛前方出发、沿释放瞬间的准星方向直线飞行，
 * 飞 {@value #DISTANCE} 格（与冰弓一致）后消失；撞到第一个有威胁生物就结算并消失，
 * 不做方块碰撞。寒冰魔弹一次从身体两侧各结晶出一枚；闪电魔弹命中点周围
 * {@value #LIGHTNING_AOE_RADIUS} 格球形范围内的所有敌人一起吃伤害。
 *
 * <p>伤害 = 固定值 + 对应元素强度 × 百分比，数值按神器等级 0/1 取表；
 * 冷却与蓝耗由 {@link ArtifactSkills} 统一处理。投射物本身是服务端逻辑 + 粒子，
 * 不注册自定义实体，客户端不需要额外渲染代码。
 */
public final class MagicBoltSkill implements ArtifactSkill {
	public static final MagicBoltSkill FIRE = new MagicBoltSkill(BoltKind.FIRE);
	public static final MagicBoltSkill ICE = new MagicBoltSkill(BoltKind.ICE);
	public static final MagicBoltSkill LIGHTNING = new MagicBoltSkill(BoltKind.LIGHTNING);

	/** 冷却 1 秒。 */
	private static final int COOLDOWN_TICKS = 20;
	/** 蓝耗 2 点。 */
	private static final double MP_COST = 2.0D;
	/** 投射物每刻飞多少格。 */
	private static final double SPEED = 3.0D;
	/** 飞行距离（格）：与冰弓一致。 */
	private static final double DISTANCE = 36.0D;
	/** 单发命中判定半径（格）。 */
	private static final double HIT_RADIUS = 0.8D;
	/** 闪电魔弹的 AOE 半径（格）。 */
	private static final double LIGHTNING_AOE_RADIUS = 2.0D;
	/** 两枚寒冰魔弹的横向间距（格）。 */
	private static final double ICE_SIDE_OFFSET = 0.45D;
	/** 双角管：三路魔弹之间的夹角（度）。 */
	private static final double SPLIT_ANGLE_DEGREES = 7.0D;
	/** 寒冰魔弹被双角管拆成两波时，第二波延迟的刻数。 */
	private static final int ICE_SECOND_WAVE_DELAY_TICKS = 4;

	private static final List<Bolt> BOLTS = new ArrayList<>();

	private final BoltKind kind;

	private MagicBoltSkill(BoltKind kind) {
		this.kind = kind;
	}

	@Override
	public String id() {
		return this.kind.id();
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return MP_COST;
	}

	/** 该等级固定伤害（提示框与结算共用）。 */
	public double flatDamage(int level) {
		return this.kind.flatAt(level);
	}

	/** 该等级元素强度倍率（%）。 */
	public double percentDamage(int level) {
		return this.kind.percentAt(level);
	}

	/** 最高等级：0/1 两档。 */
	public int maxLevel() {
		return 1;
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		double damage = this.kind.damage(player, level);
		boolean split = ArtifactEffects.magicBoltSplit(player);

		if (split) {
			damage *= ArtifactEffects.magicBoltSplitDamageMultiplier(player);
		}

		int count = split ? 3 : this.kind.projectileCount();
		// 寒冰魔弹原本一次两枚；双角管把它拆成「两波 × 三路」= 6 发，两波间隔 4 刻
		int waves = split && this.kind == BoltKind.ICE ? 2 : 1;
		LivingEntity target = ArtifactEffects.magicBoltTracking(player) ? nearestToCrosshair(player) : null;

		for (int wave = 0; wave < waves; wave++) {
			int delay = wave * ICE_SECOND_WAVE_DELAY_TICKS;

			for (int index = 0; index < count; index++) {
				// 双角管：三路像火焰箭那样以 7° 夹角散开，每一路的伤害都按减伤后的值结算
				Vec3 direction = split
						? look.yRot((float) Math.toRadians((index - 1) * SPLIT_ANGLE_DEGREES)).normalize()
						: look;
				Vec3 from = split
						? player.getEyePosition().add(direction.scale(0.5D))
						: origin(player, look, index, count);
				BOLTS.add(new Bolt(serverLevel, player.getUUID(), this.kind, from, direction, 0.0D,
						(float) damage, target, delay));
			}
		}

		castEffects(serverLevel, player, this.kind);
	}

	/** 注册投射物推进器（由 {@code Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MagicBoltSkill::tick);
	}

	private static Vec3 origin(ServerPlayer player, Vec3 look, int index, int count) {
		Vec3 eye = player.getEyePosition().add(look.scale(0.5D));

		if (count <= 1) {
			return eye;
		}

		// 寒冰魔弹从身体两侧各结晶一枚：以准星方向为轴取水平右向量
		Vec3 right = new Vec3(-look.z, 0.0D, look.x);

		if (right.lengthSqr() < 1.0E-6D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}

		return eye.add(right.scale(index == 0 ? -ICE_SIDE_OFFSET : ICE_SIDE_OFFSET));
	}

	private static void castEffects(ServerLevel level, ServerPlayer player, BoltKind kind) {
		switch (kind) {
			case FIRE -> {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.3F);
				level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
						12, 0.25D, 0.25D, 0.25D, 0.3D);
			}
			case ICE -> {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F, 1.2F);
				level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0D, player.getZ(),
						14, 0.35D, 0.35D, 0.35D, 0.05D);
			}
			case LIGHTNING -> {
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.25F, 1.8F);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
						player.getX(), player.getY() + 1.0D, player.getZ(),
						16, 0.3D, 0.3D, 0.3D, 0.15D);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		if (BOLTS.isEmpty()) {
			return;
		}

		List<Bolt> inFlight = new ArrayList<>(BOLTS);
		BOLTS.clear();

		for (Bolt bolt : inFlight) {
			ServerPlayer owner = server.getPlayerList().getPlayer(bolt.ownerId());

			if (owner == null || !owner.isAlive()) {
				continue;
			}

			// 寒冰魔弹被双角管拆成两波：第二波先在原地等几刻再起飞
			if (bolt.delay() > 0) {
				BOLTS.add(new Bolt(bolt.level(), bolt.ownerId(), bolt.kind(), bolt.pos(), bolt.fallback(),
						bolt.traveled(), bolt.damage(), bolt.target(), bolt.delay() - 1));
				continue;
			}

			Vec3 direction = bolt.fallback();

			if (bolt.target() != null && bolt.target().isAlive() && bolt.target().level() == bolt.level()) {
				Vec3 delta = bolt.target().getBoundingBox().getCenter().subtract(bolt.pos());
				direction = delta.lengthSqr() < 1.0E-6D ? direction : delta.normalize();
			}

			double nextTraveled = bolt.traveled() + SPEED;
			Vec3 start = bolt.pos();
			Vec3 end = bolt.pos().add(direction.scale(SPEED));

			// 用这一小段扫一遍，避免 3 格/刻的速度穿过小目标
			AABB sweep = new AABB(start, end).inflate(HIT_RADIUS);
			List<LivingEntity> victims = bolt.level().getEntitiesOfClass(LivingEntity.class, sweep);
			boolean hit = false;

			for (LivingEntity victim : victims) {
				if (!SephiriaDamage.strikeable(victim, owner)) {
					continue;
				}

				if (bolt.kind().aoeRadius() > 0.0D) {
					strikeAoe(bolt, owner, end);
				} else {
					damage(bolt, owner, victim);
				}

				hitEffects(bolt.level(), bolt.kind(), end);
				hit = true;
				break;
			}

			if (hit) {
				continue;
			}

			if (nextTraveled >= DISTANCE) {
				hitEffects(bolt.level(), bolt.kind(), end);
				continue;
			}

			trail(bolt.level(), bolt.kind(), end);
			BOLTS.add(new Bolt(bolt.level(), bolt.ownerId(), bolt.kind(), end, bolt.fallback(),
					nextTraveled, bolt.damage(), bolt.target(), 0));
		}
	}

	/** 闪电魔弹：命中点周围半径 2 格的球形范围内每个有威胁生物吃一次伤害。 */
	private static void strikeAoe(Bolt bolt, ServerPlayer owner, Vec3 center) {
		double radius = bolt.kind().aoeRadius();
		AABB area = new AABB(center, center).inflate(radius);

		for (LivingEntity victim : bolt.level().getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.strikeable(victim, owner)
					|| victim.position().distanceToSqr(center) > radius * radius) {
				continue;
			}

			damage(bolt, owner, victim);
		}
	}

	private static void damage(Bolt bolt, ServerPlayer owner, LivingEntity victim) {
		SephiriaDamage.clearDamageCooldown(victim);
		victim.hurtServer(bolt.level(), bolt.kind().source(bolt.level(), owner), bolt.damage());
	}

	private static void trail(ServerLevel level, BoltKind kind, Vec3 at) {
		switch (kind) {
			case FIRE -> {
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
				level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.06D, 0.06D, 0.06D, 0.0D);
			}
			case ICE -> level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z,
					3, 0.08D, 0.08D, 0.08D, 0.01D);
			case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
					4, 0.1D, 0.1D, 0.1D, 0.02D);
		}
	}

	private static void hitEffects(ServerLevel level, BoltKind kind, Vec3 at) {
		switch (kind) {
			case FIRE -> {
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 14, 0.35D, 0.35D, 0.35D, 0.05D);
				level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.25D, 0.25D, 0.25D, 0.02D);
			}
			case ICE -> level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z,
					18, 0.35D, 0.35D, 0.35D, 0.04D);
			case LIGHTNING -> {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
						20, 0.4D, 0.4D, 0.4D, 0.08D);
				level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 0.95F, 0.6F),
						at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	/** 三个魔弹的元素、投射物数量、AOE 半径与伤害表。 */
	private enum BoltKind {
		FIRE("fire_bolt", PlayerStats.Element.FIRE, 1, 0.0D,
				new double[]{ 2.0D, 5.0D }, new double[]{ 10.0D, 15.0D }),
		ICE("ice_bolt", PlayerStats.Element.ICE, 2, 0.0D,
				new double[]{ 1.0D, 2.0D }, new double[]{ 5.0D, 8.0D }),
		LIGHTNING("lightning_bolt", PlayerStats.Element.LIGHTNING, 1, LIGHTNING_AOE_RADIUS,
				new double[]{ 2.0D, 4.0D }, new double[]{ 8.0D, 13.0D });

		private final String id;
		private final PlayerStats.Element element;
		private final int projectileCount;
		private final double aoeRadius;
		private final double[] flatDamage;
		private final double[] percentDamage;

		BoltKind(String id, PlayerStats.Element element, int projectileCount, double aoeRadius,
				double[] flatDamage, double[] percentDamage) {
			this.id = id;
			this.element = element;
			this.projectileCount = projectileCount;
			this.aoeRadius = aoeRadius;
			this.flatDamage = flatDamage;
			this.percentDamage = percentDamage;
		}

		private String id() {
			return this.id;
		}

		private int projectileCount() {
			return this.projectileCount;
		}

		private double aoeRadius() {
			return this.aoeRadius;
		}

		private double flatAt(int level) {
			return this.flatDamage[Math.max(0, Math.min(this.flatDamage.length - 1, level))];
		}

		private double percentAt(int level) {
			return this.percentDamage[Math.max(0, Math.min(this.percentDamage.length - 1, level))];
		}

		private double damage(ServerPlayer player, int level) {
			double element = PlayerStats.elementTotal(player, this.element);
			return (flatAt(level) + element * percentAt(level) / 100.0D)
					* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		}

		private DamageSource source(ServerLevel level, ServerPlayer player) {
			return switch (this) {
				case FIRE -> SephiriaDamage.magicFireAttack(level, player);
				case ICE -> SephiriaDamage.magicIceAttack(level, player);
				case LIGHTNING -> SephiriaDamage.magicElectricAttack(level, player);
			};
		}
	}

	/** 准星射线附近最近的敌人（距离按到射线的垂距算；没有就返回 null）。 */
	private static LivingEntity nearestToCrosshair(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return null;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		AABB area = player.getBoundingBox().inflate(DISTANCE);
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (!SephiriaDamage.autoTargetable(victim, player)) {
				continue;
			}

			Vec3 center = victim.getBoundingBox().getCenter();
			Vec3 delta = center.subtract(eye);
			double along = delta.dot(look);

			if (along < 0.0D || along > DISTANCE) {
				continue;
			}

			double distance = eye.add(look.scale(along)).distanceToSqr(center);

			if (distance < bestDistance) {
				bestDistance = distance;
				best = victim;
			}
		}

		return best;
	}

	/** 一枚在飞行的魔弹；target 非空时每刻转向它，死了就回到原准星方向。delay > 0 时先等待。 */
	private record Bolt(ServerLevel level, UUID ownerId, BoltKind kind, Vec3 pos, Vec3 fallback,
			double traveled, float damage, LivingEntity target, int delay) {
	}
}
