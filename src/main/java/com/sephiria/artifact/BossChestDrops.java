package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.talent.Talents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
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

			// 两个箱子各一个，直接掉在 BOSS 死亡的位置（来源不限，环境伤害也算）
			entity.spawnAtLocation(level, new ItemStack(ModItems.RARE_ARTIFACT_CHEST));
			entity.spawnAtLocation(level, new ItemStack(ModItems.RARE_SLATE_CHEST));
		});
	}
}
