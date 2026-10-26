package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArtifactSkills;
import com.sephiria.artifact.skill.SkillSlots;
import com.sephiria.network.HudSyncPayload;
import com.sephiria.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能 HUD 与神器 HUD 的服务端数据源。
 *
 * <p>技能 HUD：6 个技能栏里可解析的技能（图标 = 神器物品）+ 各自的冷却。冷却按<b>栏位</b>记，
 * 与 {@link ArtifactSkills} 同一张表。
 *
 * <p>神器 HUD：只收「有冷却 / 充能」的神器——冷却条目由各神器自己的计时表提供，充能条目
 * 目前是闪烁的眼睛（海市蜃楼）。列表按「旧 → 新」追加；客户端 HUD 右边缘固定、从右向左画，
 * 所以新加的神器只会在左边出现，不会挤动已有图标。
 */
public final class ArtifactHud {
	/** 推送间隔（tick）：5 刻一包，充能条也能平滑地转；客户端在两包之间按刻数平滑冷却。 */
	private static final int SYNC_INTERVAL = 5;

	private static int ticker;

	private ArtifactHud() {
	}

	/** 由 {@code Sephiria#onInitialize()} 调用。 */
	public static void registerTicker() {
		ServerTickEvents.END_SERVER_TICK.register(ArtifactHud::tick);
	}

	private static void tick(MinecraftServer server) {
		if (++ticker % SYNC_INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
		}
	}

	/** 立刻给一个玩家推一份（施放技能 / 改技能栏 / 进服时用）。 */
	public static void send(ServerPlayer player) {
		ServerPlayNetworking.send(player, new HudSyncPayload(skills(player), artifacts(player)));
	}

	private static List<HudSyncPayload.Entry> skills(ServerPlayer player) {
		List<HudSyncPayload.Entry> entries = new ArrayList<>();
		List<String> slots = SkillSlots.of(player);

		for (int slot = 0; slot < SkillSlots.COUNT; slot++) {
			ArtifactSkills.Available resolved = ArtifactSkills.resolve(player, slots.get(slot));

			if (resolved == null) {
				continue;
			}

			entries.add(new HudSyncPayload.Entry(BuiltInRegistries.ITEM.getKey(resolved.item()),
					ArtifactSkills.cooldownLeft(player, slot), resolved.skill().cooldownTicks(), -1));
		}

		return entries;
	}

	private static List<HudSyncPayload.Entry> artifacts(ServerPlayer player) {
		List<HudSyncPayload.Entry> entries = new ArrayList<>();

		addCooldown(entries, player, ModItems.RED_SNAKE_EYE,
				RedSnakeEyeItem.cooldownLeft(player), RedSnakeEyeItem.cooldownTicks());
		addCooldown(entries, player, ModItems.SANDE_EARRINGS,
				SandeEarringsItem.cooldownLeft(player), SandeEarringsItem.cooldownTicks());
		addCooldown(entries, player, ModItems.PROSHIUM_RING,
				ProshiumRingItem.cooldownLeft(player), ProshiumRingItem.cooldownTicks(player));
		addCooldown(entries, player, ModItems.LIGHTNING_STRUCK_BRANCH,
				LightningStruckBranchItem.cooldownLeft(player), LightningStruckBranchItem.cooldownTicks());

		// 冰霜武具的三件充能神器（剑鞘 / 锤子 / 长矛）
		addCharge(entries, player, ModItems.EXORCISM_SCABBARD,
				com.sephiria.frost.FrostArmament.chargeProgress(player, ModItems.EXORCISM_SCABBARD));
		addCharge(entries, player, ModItems.BLIZZARD_HAMMER,
				com.sephiria.frost.FrostArmament.chargeProgress(player, ModItems.BLIZZARD_HAMMER));
		addCharge(entries, player, ModItems.VOLSPA,
				com.sephiria.frost.FrostArmament.chargeProgress(player, ModItems.VOLSPA));

		// 酷寒断头台：神器 HUD 里的冷却图标保留（用户 2026-10-26 要求），
		// 冰霜武具文字行里的「斩」也继续显示同样的冷却状态。
		addCooldown(entries, player, ModItems.CRYO_GUILLOTINE,
				CryoGuillotineItem.cooldownLeft(player), CryoGuillotineItem.cooldownTicks(player));

		// 闪烁的眼睛是充能神器：0..1000 千分比
		double charge = FlickeringEyeItem.charge(player);

		if (charge >= 0.0D) {
			entries.add(new HudSyncPayload.Entry(BuiltInRegistries.ITEM.getKey(ModItems.FLICKERING_EYE),
					-1, -1, (int) Math.round(Math.clamp(charge / 100.0D, 0.0D, 1.0D) * 1000.0D)));
		}

		return entries;
	}

	private static void addCharge(List<HudSyncPayload.Entry> entries, ServerPlayer player,
			net.minecraft.world.item.Item item, int charge) {
		if (charge < 0 || ArtifactEffects.highestLevelOf(player, item) < 0) {
			return;
		}

		entries.add(new HudSyncPayload.Entry(BuiltInRegistries.ITEM.getKey(item), -1, -1,
				Math.max(0, Math.min(1000, charge))));
	}

	private static void addCooldown(List<HudSyncPayload.Entry> entries, ServerPlayer player,
			net.minecraft.world.item.Item item, int remaining, int total) {
		if (remaining < 0 || total <= 0 || ArtifactEffects.highestLevelOf(player, item) < 0) {
			return;
		}

		entries.add(new HudSyncPayload.Entry(BuiltInRegistries.ITEM.getKey(item), remaining, total, -1));
	}
}
