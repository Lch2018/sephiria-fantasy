package com.sephiria.artifact.skill;

import com.sephiria.artifact.ArtifactEffects;
import com.sephiria.artifact.ThunderArmorItem;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 雷霆之甲（稀有魔法书）：生成持续 10 秒的闪电护甲，每 0.33 秒（这里取 7 刻）对周围 8 格内
 * 最近的 3 个敌人释放连锁闪电。
 *
 * <p>每一跳对 3 个目标各结算一次电属性伤害（可重复命中同一目标，只要它仍在最近的 3 个里）；
 * 闪电画成玩家 → 目标 → 目标 → 目标的折线粒子。
 */
public final class ThunderArmorSkill implements ArtifactSkill {
	public static final ThunderArmorSkill INSTANCE = new ThunderArmorSkill();

	private static final int COOLDOWN_TICKS = 500;
	private static final int DURATION_TICKS = 200;
	private static final int INTERVAL_TICKS = 7;
	private static final double LOCK_RADIUS = 8.0D;
	private static final int TARGETS = 3;

	private static final Map<UUID, Armor> ARMORS = new HashMap<>();

	private ThunderArmorSkill() {
	}

	@Override
	public String id() {
		return "thunder_armor";
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN_TICKS;
	}

	@Override
	public double mpCost(int level) {
		return ThunderArmorItem.mpCost(level);
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		double damage = (ThunderArmorItem.flatAt(level)
				+ PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)
				* ThunderArmorItem.percentAt(level) / 100.0D)
				* ArtifactEffects.magicBookSkillDamageMultiplier(player);
		long now = player.level().getGameTime();
		ARMORS.put(player.getUUID(), new Armor((float) damage, now, now));

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.45F, 1.5F);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(ThunderArmorSkill::tick);
	}

	private static void tick(MinecraftServer server) {
		if (ARMORS.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, Armor>> iterator = ARMORS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Armor> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null || !player.isAlive() || now >= entry.getValue().endTick) {
				iterator.remove();
				continue;
			}

			if (now >= entry.getValue().nextStrike && player.level() instanceof ServerLevel level) {
				strike(level, player, entry.getValue());
				entry.getValue().nextStrike = now + INTERVAL_TICKS;
			}

			if (player.level() instanceof ServerLevel level) {
				aura(level, player);
			}
		}
	}

	private static void strike(ServerLevel level, ServerPlayer player, Armor armor) {
		List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
				new AABB(player.position(), player.position()).inflate(LOCK_RADIUS)));
		targets.removeIf(victim -> !SephiriaDamage.autoTargetable(victim, player)
				|| victim.position().distanceToSqr(player.position()) > LOCK_RADIUS * LOCK_RADIUS);
		targets.sort(Comparator.comparingDouble(victim -> victim.distanceToSqr(player)));

		Vec3 from = player.position().add(0.0D, 0.9D, 0.0D);

		for (int index = 0; index < Math.min(TARGETS, targets.size()); index++) {
			LivingEntity target = targets.get(index);
			SephiriaDamage.clearDamageCooldown(target);
			target.hurtServer(level, SephiriaDamage.magicElectricAttack(level, player), armor.damage);
			chain(level, from, target.getBoundingBox().getCenter());
			from = target.getBoundingBox().getCenter();
		}
	}

	private static void chain(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();

		if (length < 1.0E-4D) {
			return;
		}

		Vec3 step = delta.scale(1.0D / length);

		for (double traveled = 0.0D; traveled <= length; traveled += 0.5D) {
			Vec3 at = from.add(step.scale(traveled));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
		}

		level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
				SoundSource.PLAYERS, 0.2F, 1.7F);
	}

	private static void aura(ServerLevel level, ServerPlayer player) {
		double angle = level.getGameTime() * 0.35D;

		for (int index = 0; index < 3; index++) {
			double a = angle + index * Math.PI * 2.0D / 3.0D;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					player.getX() + Math.cos(a) * 0.9D, player.getY() + 1.0D,
					player.getZ() + Math.sin(a) * 0.9D, 1, 0.05D, 0.2D, 0.05D, 0.01D);
		}
	}

	private static final class Armor {
		private final float damage;
		private final long endTick;
		private long nextStrike;

		private Armor(float damage, long startTick, long firstStrike) {
			this.damage = damage;
			this.endTick = startTick + DURATION_TICKS;
			this.nextStrike = firstStrike;
		}
	}
}
