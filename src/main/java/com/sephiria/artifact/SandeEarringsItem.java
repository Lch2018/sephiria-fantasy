package com.sephiria.artifact;

import com.sephiria.debuff.Debuffs;
import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 桑德耳环（魔法科技，高级品质）：【唯一】每 4 秒对附近 1/2/2/3/3/4 名敌人造成闪电属性伤害 10%
 * 的闪电攻击，并施加触电减益效果；闪电属性伤害 +1/2/3/4/5/6（0..5 级）。
 *
 * <p>闪电攻击走 {@code Kind.ELECTRIC}：数值在打出去那一刻按触发者当前的电元素强度面板值现算
 * （换装跟着走），吃「电属性攻击的暴击几率」（麒麟的角）；打中后直接调用
 * {@code Debuffs#applyShock} 施加触电——不要求魔法科技连击到 2 档，耳环自己就能挂。
 *
 * <p>「附近」取 6 格（与连击面板的「范围 600」同口径），每次挑最近的目标，最多打该等级的人数。
 */
public class SandeEarringsItem extends ArtifactItem {
	/** 各等级每 4 秒能打的目标数。 */
	private static final int[] TARGETS_BY_LEVEL = { 1, 2, 2, 3, 3, 4 };
	/** 各等级的电元素强度加成（点）。 */
	private static final double[] LIGHTNING_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D };
	/** 闪电攻击的倍率（%）：电元素强度面板值的百分之几。 */
	public static final double ATTACK_PERCENT = 10.0D;
	/** 触发间隔（tick）：4 秒。 */
	private static final int INTERVAL_TICKS = 80;
	/** 「附近」的半径（格）。 */
	private static final double RADIUS = 6.0D;
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 上次触发的刻：攻击者 → 时间刻（装上后先等一个周期，不立刻打）。 */
	private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();

	public SandeEarringsItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.sande_earrings.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return TARGETS_BY_LEVEL.length - 1;
	}

	@Override
	public int sandeTargetCount(int level) {
		return TARGETS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, TARGETS_BY_LEVEL.length - 1)];
	}

	@Override
	public double lightningElementBonus(int level) {
		return valueAt(LIGHTNING_BY_LEVEL, level);
	}

	/** 每 4 秒的闪电攻击（服务端刻推进；没带耳环的玩家直接跳过）。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(SandeEarringsItem::tick);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) {
				continue;
			}

			int level = ArtifactEffects.highestLevelOf(player, ModItems.SANDE_EARRINGS);
			int targets = level < 0 ? 0 : sandeTargetCountAt(level);

			if (targets <= 0) {
				LAST_ATTACK.remove(player.getUUID());
				continue;
			}

			long now = player.level().getGameTime();
			Long last = LAST_ATTACK.get(player.getUUID());

			// 第一次见到（刚装上）：先记下时间，等满 4 秒再打第一发
			if (last == null) {
				LAST_ATTACK.put(player.getUUID(), now);
				continue;
			}

			if (now - last < INTERVAL_TICKS) {
				continue;
			}

			LAST_ATTACK.put(player.getUUID(), now);
			attack(player, targets);
		}
	}

	/** 该等级的目标数（静态取法，供推进器用）。 */
	private static int sandeTargetCountAt(int level) {
		return TARGETS_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, TARGETS_BY_LEVEL.length - 1)];
	}

	private static void attack(ServerPlayer player, int targets) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		double damage = PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING) * ATTACK_PERCENT / 100.0D;

		if (damage <= 0.0D) {
			return;
		}

		AABB area = player.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS);
		// 自动攻击不打和平生物：只挑有威胁的（见 SephiriaDamage.strikeable），每次取最近的若干个
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area).stream()
				.filter(victim -> SephiriaDamage.strikeable(victim, player))
				.sorted(Comparator.comparingDouble(victim -> victim.distanceToSqr(player)))
				.limit(targets)
				.toList();

		if (victims.isEmpty()) {
			return;
		}

		net.minecraft.world.damagesource.DamageSource source = SephiriaDamage.electricAttack(level, player);

		for (LivingEntity victim : victims) {
			// 同一次打多个目标、且要能稳定打中：先清无敌帧（真实伤害同款手法）
			victim.setInvulnerableTime(0);
			victim.hurtServer(level, source, (float) damage);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.5D), victim.getZ(),
					10, 0.35D, 0.5D, 0.35D, 0.05D);
			Debuffs.applyShock(victim, player);
		}
	}

	/** 玩家退出时清计时表。 */
	public static void forget(ServerPlayer player) {
		LAST_ATTACK.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sande_attack",
						Component.literal(Numbers.format(this.sandeTargetCount(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(ATTACK_PERCENT)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_element",
						Component.literal(Numbers.format(this.lightningElementBonus(level))).withColor(COLOUR_BONUS)));
	}
}
