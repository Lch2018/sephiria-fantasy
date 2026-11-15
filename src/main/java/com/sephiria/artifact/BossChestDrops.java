package com.sephiria.artifact;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.sephiria.difficulty.RootRetreat;
import com.sephiria.registry.ModItems;
import com.sephiria.talent.Talents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * BOSS 死亡掉落（用户 2026-10-21）：每死一个 BOSS，固定掉一个稀有神器宝箱 + 一个稀有石板宝箱。
 *
 * <p>不要求玩家补最后一刀——摔死、岩浆、其它环境伤害同样会掉（用户 2026-10-21 明确）。
 * BOSS 的判定复用天赋系统那一套（{@code c:bosses} 标签 + 自带 {@code BossEvent} 血条 +
 * 原版末影龙 / 凋灵），见 {@link Talents#isBoss}。
 */
public final class BossChestDrops {
	private BossChestDrops() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity.level() instanceof ServerLevel level) || !Talents.isBoss(entity)) {
				return;
			}

			// 根之退 40 / 50 阶段：BOSS 掉的稀有神器 / 稀有石板宝箱各 +1 / +2。
			// 用击杀者的阶段；环境伤害（摔死 / 岩浆）没有击杀者时退回附近玩家。
			ServerPlayer owner = Talents.attackerOf(source);

			if (owner == null) {
				owner = nearestPlayer(level, entity.getX(), entity.getY(), entity.getZ(), 64.0D);
			}

			int stage = owner == null ? 0 : RootRetreat.stageOf(owner);
			int count = 1 + (stage >= RootRetreat.COURAGE_RED_STAGE ? 2
					: stage >= RootRetreat.COURAGE_STAGE ? 1 : 0);

			for (int index = 0; index < count; index++) {
				entity.spawnAtLocation(new ItemStack(ModItems.RARE_ARTIFACT_CHEST));
				entity.spawnAtLocation(new ItemStack(ModItems.RARE_SLATE_CHEST));
			}
		});
	}

	/** 没有击杀者时找最近的玩家：给环境伤害一个归属。 */
	private static ServerPlayer nearestPlayer(ServerLevel level, double x, double y, double z, double range) {
		ServerPlayer nearest = null;
		double best = range * range;

		for (ServerPlayer player : level.players()) {
			double distance = player.distanceToSqr(x, y, z);

			if (distance <= best) {
				best = distance;
				nearest = player;
			}
		}

		return nearest;
	}
}
