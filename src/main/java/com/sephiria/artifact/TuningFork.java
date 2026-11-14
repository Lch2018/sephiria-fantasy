package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.stats.PlayerStats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 音叉：施放魔法书攒「声波」层数，武器攻击时一次消耗所有层数，对命中目标打一发混沌伤害。
 *
 * <p>层数上限 5，攒层有 0.8 秒冷却；伤害按物理 / 火 / 冰 / 电四项属性值的平均值 × 200%
 * × 层数，再乘魔法书加速与魔法书伤害倍率——它被明确定为「视为魔法书伤害」，
 * 所以走 {@link SephiriaDamage#magicChaos}，吃魔法书暴击，也触发书库之阳。
 */
public final class TuningFork {
	/** 层数上限。 */
	public static final int MAX_STACKS = 5;
	/** 攒一层的内置冷却（0.8 秒）。 */
	private static final int STACK_COOLDOWN_TICKS = 16;
	/** 每层伤害：四项属性平均值 × 200%。 */
	private static final double DAMAGE_PERCENT = 200.0D;

	private static final class State {
		private int stacks;
		/** 初始值别用 Long.MIN_VALUE：now - MIN_VALUE 会溢出成负数，第一次攒层永远过不了冷却。 */
		private long lastStackTick = -STACK_COOLDOWN_TICKS;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private TuningFork() {
	}

	/** 注册武器命中的消耗触发（由 {@link com.sephiria.Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer player) || entity == player) {
				return;
			}

			if (SephiriaDamage.kindOf(source) != SephiriaDamage.Kind.WEAPON
					|| source.is(SephiriaDamage.ETERNAL_WINTER)
					|| entity instanceof Player) {
				return;
			}

			State state = STATES.get(player.getUUID());

			if (state == null || state.stacks <= 0 || !(entity.level() instanceof ServerLevel level)) {
				return;
			}

			int stacks = state.stacks;
			state.stacks = 0;

			double average = (PlayerStats.physicalTotal(player)
					+ PlayerStats.elementTotal(player, PlayerStats.Element.FIRE)
					+ PlayerStats.elementTotal(player, PlayerStats.Element.ICE)
					+ PlayerStats.elementTotal(player, PlayerStats.Element.LIGHTNING)) / 4.0D;
			double damage = average * DAMAGE_PERCENT / 100.0D * stacks
					* (1.0D + ArtifactEffects.magicBookHastePercent(player) / 100.0D)
					* ArtifactEffects.magicBookDamageMultiplier(player);

			if (damage <= 0.0D) {
				return;
			}

			SephiriaDamage.clearDamageCooldown(entity);
			entity.hurtServer(level, SephiriaDamage.magicChaos(level, player), (float) damage);
			level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY(0.5D), entity.getZ(),
					18, 0.4D, 0.5D, 0.4D, 0.15D);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 0.7F);
		});
	}

	/** 魔法书施放成功后调用：攒一层（最多 5 层，0.8 秒内置冷却）。 */
	public static void onMagicBookCast(ServerPlayer player) {
		if (!ArtifactEffects.tuningForkEnabled(player)) {
			return;
		}

		State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
		long now = player.level().getGameTime();

		if (state.stacks >= MAX_STACKS || now - state.lastStackTick < STACK_COOLDOWN_TICKS) {
			return;
		}

		state.stacks++;
		state.lastStackTick = now;
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.35F, 1.5F);
	}

	/** 玩家退出时清掉层数。 */
	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}
}
