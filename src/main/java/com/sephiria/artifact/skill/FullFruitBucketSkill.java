package com.sephiria.artifact.skill;

import com.sephiria.stats.PlayerStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 饱满果实桶（湖泊，稀有）：发动型神器，冷却 30 秒，立即恢复 50/100% MP；
 * MP再生每 1 点让冷却加速 3%。
 */
public final class FullFruitBucketSkill implements ArtifactSkill {
	public static final FullFruitBucketSkill INSTANCE = new FullFruitBucketSkill();

	private static final int BASE_COOLDOWN_TICKS = 600;

	private FullFruitBucketSkill() {
	}

	@Override
	public String id() {
		return "full_fruit_bucket";
	}

	@Override
	public int cooldownTicks() {
		return BASE_COOLDOWN_TICKS;
	}

	@Override
	public int cooldownTicks(ServerPlayer player, int level) {
		double speed = 1.0D + PlayerStats.mpRegenTotal(player) * 0.03D;
		return Math.max(1, (int) Math.round(BASE_COOLDOWN_TICKS / speed));
	}

	@Override
	public double mpCost(int level) {
		return 0.0D;
	}

	@Override
	public void cast(ServerPlayer player, int level) {
		double percent = level <= 0 ? 50.0D : 100.0D;
		PlayerStats.gainMp(player, PlayerStats.maxMpTotal(player) * percent / 100.0D);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
	}
}
