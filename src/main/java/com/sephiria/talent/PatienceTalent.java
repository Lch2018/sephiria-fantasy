package com.sephiria.talent;

import com.mojang.serialization.Codec;
import com.sephiria.Sephiria;
import com.sephiria.backpack.ArtifactBackpack;
import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * 天赋「耐心」的三项阶段能力。
 *
 * <ul>
 *   <li><b>一阶段：石板基础</b>——直接出现在赛菲莉娅背包里，不能移出 / 丢弃；第一阶段熄灭时
 *       从背包删掉。背包满时把第一格的东西丢在脚下再放进第一格（用户 2026-10-21 定的口径）。</li>
 *   <li><b>二阶段：低血回复</b>——生命值低于 7 时每秒回 1 点。</li>
 *   <li><b>三阶段：十日复活</b>——死亡时以 60% 最大生命值复活，10 个游戏日一次。
 *       不死图腾优先：图腾生效时玩家不会死亡，{@code ALLOW_DEATH} 根本不会触发。</li>
 * </ul>
 */
public final class PatienceTalent {
	/** 复活冷却：10 个游戏日（1 日 = 24000 刻）。 */
	private static final long REVIVE_COOLDOWN_TICKS = Talents.PATIENCE_REVIVE_COOLDOWN_TICKS;

	/** 上次复活之后「可以再次复活」的绝对游戏刻。 */
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> REVIVE_READY_AT =
			net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
					.persistent(Codec.LONG)
					.copyOnDeath()
					.buildAndRegister(net.minecraft.resources.Identifier.fromNamespaceAndPath(
							Sephiria.MOD_ID, "patience_revive_ready_at"));

	private PatienceTalent() {
	}

	public static void register() {
		// 进服时补一次石板基础（老存档 / 刚点上一阶段就退出重进的）
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ensure(handler.player);
			// 生存三阶段如果没点亮（重置 / 退点后异常退出），把残留的刻印石板清掉
			SlateImprint.dropIfInactive(handler.player);
			// 智慧二阶段：整理重复神器 / 还原已经失效的合并
			WisdomTalent.onJoin(handler.player);
		});

		// 二阶段：低于 7 点生命时每秒回 1
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (Talents.stage(player, Talent.PATIENCE) < 2) {
					continue;
				}

				if (player.isAlive() && player.getHealth() > 0.0F
						&& player.getHealth() < Talents.PATIENCE_LOW_HP_THRESHOLD) {
					player.heal((float) Talents.PATIENCE_LOW_HP_REGEN);
				}
			}
		});

		// 三阶段：死亡时以 60% 生命复活（图腾已经先拦过一轮，能走到这里就是没有图腾）
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player) || Talents.stage(player, Talent.PATIENCE) < 3) {
				return true;
			}

			// 用主世界的游戏刻计时：10 个游戏日按主世界算（其它维度的 day time 可能不同）
			long now = player.level().getServer().overworld().getGameTime();
			long ready = readyAt(player);

			if (now < ready) {
				return true;
			}

			player.setAttached(REVIVE_READY_AT, now + REVIVE_COOLDOWN_TICKS);
			float health = (float) (player.getMaxHealth() * Talents.PATIENCE_REVIVE_HEALTH_PERCENT / 100.0D);
			player.setHealth(Math.max(1.0F, health));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

			if (player.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
						player.getX(), player.getY() + 1.0D, player.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.3D);
			}

			player.sendSystemMessage(Component.translatable("message.sephiria_fantasy.patience_revive"), true);
			return false;
		});
	}

	/** 阶段变化：点亮一阶段发石板基础，熄灭一阶段删掉它。 */
	public static void onStageChanged(ServerPlayer player, int oldStage, int newStage) {
		if (newStage >= 1 && oldStage < 1) {
			ensure(player);
		} else if (newStage < 1 && oldStage >= 1) {
			removeBase(player);
		}
	}

	/** 一阶段点亮时保证背包里有一块石板基础；没点亮时保证没有。 */
	public static void ensure(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		boolean active = Talents.stage(player, Talent.PATIENCE) >= 1;

		if (!active) {
			removeBase(player);
			return;
		}

		if (containsBase(backpack)) {
			return;
		}

		int size = backpack.getContainerSize();
		int target = -1;

		for (int slot = 0; slot < size; slot++) {
			if (backpack.getItem(slot).isEmpty()) {
				target = slot;
				break;
			}
		}

		if (target < 0) {
			// 背包满了：把第一格的东西丢在脚下，再把石板基础放进第一格（用户口径）
			ItemStack first = backpack.getItem(0);

			if (!first.isEmpty()) {
				player.drop(first.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
			}

			target = 0;
		}

		backpack.setItem(target, new ItemStack(ModItems.SLATE_BASE));
		backpack.setChanged();
	}

	/** 是否是一阶段的「石板基础」（菜单锁格、刻印排除都用它）。 */
	public static boolean isBase(ItemStack stack) {
		return stack.getItem() == ModItems.SLATE_BASE;
	}

	private static boolean containsBase(ArtifactBackpack backpack) {
		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			if (isBase(backpack.getItem(slot))) {
				return true;
			}
		}

		return false;
	}

	private static void removeBase(ServerPlayer player) {
		ArtifactBackpack backpack = ArtifactBackpack.of(player);
		boolean changed = false;

		for (int slot = 0; slot < ArtifactBackpack.MAX_SLOTS; slot++) {
			if (isBase(backpack.getItem(slot))) {
				backpack.setItem(slot, ItemStack.EMPTY);
				changed = true;
			}
		}

		if (changed) {
			backpack.setChanged();
		}
	}

	private static long readyAt(ServerPlayer player) {
		Long value = player.getAttached(REVIVE_READY_AT);
		return value == null ? 0L : value;
	}
}
