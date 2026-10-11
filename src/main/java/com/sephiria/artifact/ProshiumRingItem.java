package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 普罗修姆戒指（冰川，普通品质）：【唯一】使用武器攻击或魔法书造成伤害时，
 * 对目标附加 5/10/15 冰属性伤害，冷却 2/1.5/1 秒（0..2 级）。
 *
 * <p>与「被雷击中的树枝」的附加闪电同型，区别只有元素：走 {@code sephiria:ice_attack}
 * （{@code Kind.ICE_ATTACK}），吃减伤与黄金之手，并且**照常触发霜之触**
 * ——所以这枚戒指既是伤害来源，也是叠冻伤的一条快路（挂层的那一下自己也会再判定一次霜之触，
 * 但霜之触有自己的 2 秒冷却，不会自我连锁）。
 */
public class ProshiumRingItem extends ArtifactItem {
	/** 各等级附加的冰属性伤害（点，固定值）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 5.0D, 10.0D, 15.0D };
	/** 各等级的冷却（秒）：2 / 1.5 / 1。 */
	private static final double[] COOLDOWN_SECONDS_BY_LEVEL = { 2.0D, 1.5D, 1.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;
	/** 冷却表：攻击者 → 上次触发的时间刻。 */
	private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

	public ProshiumRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GLACIER;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.COMMON;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.proshium_ring.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public double onHitIceDamage(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}

	@Override
	public double onHitIceCooldownSeconds(int level) {
		return valueAt(COOLDOWN_SECONDS_BY_LEVEL, level);
	}

	/** 挂在伤害事件上：武器 / 魔法书打出的伤害触发附加冰伤（冷却随等级 2/1.5/1 秒）。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			SephiriaDamage.Kind kind = SephiriaDamage.kindOf(source);

			if (kind != SephiriaDamage.Kind.WEAPON && kind != SephiriaDamage.Kind.ARTIFACT) {
				return;
			}

			// 与树枝的附加闪电一致：这一击不打玩家（普攻 PVP 照旧，只是那一击不带这份冰）
			if (entity instanceof net.minecraft.world.entity.player.Player) {
				return;
			}

			double flat = ArtifactEffects.onHitIceDamage(player);
			int cooldown = ArtifactEffects.onHitIceCooldownTicks(player);

			if (flat <= 0.0D || cooldown <= 0 || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			long now = level.getGameTime();
			Long last = LAST_HIT.get(player.getUUID());

			// 与电击之触同一处坑：别拿 Long.MIN_VALUE 当「从没打过」的初值（now - MIN_VALUE 溢出成负数）
			if (last != null && now - last < cooldown) {
				return;
			}

			LAST_HIT.put(player.getUUID(), now);
			// 额外那一下先清无敌帧，免得被目标上一刻吃到的伤害顶掉
			SephiriaDamage.clearDamageCooldown(entity);
			entity.hurtServer(level, SephiriaDamage.iceAttack(level, player), (float) flat);
			level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.5D), entity.getZ(),
					8, 0.3D, 0.4D, 0.3D, 0.05D);
		});
	}

	/** 玩家退出时清冷却表（与 Debuffs.forget 同一批调用）。 */
	public static void forget(ServerPlayer player) {
		LAST_HIT.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.on_hit_ice",
				Component.literal(Numbers.format(this.onHitIceDamage(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(this.onHitIceCooldownSeconds(level))).withColor(COLOUR_BONUS)));
	}
}
