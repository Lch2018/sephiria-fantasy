package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.stats.CombatState;
import com.sephiria.stats.ShieldSystem;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 破碎的镜子（守护，高级）：【唯一】防御力 +4/8；脱战后获得 3/6 点护盾。
 *
 * <p>护盾是「一直留到打破」的那种（没有持续时间），同一面镜子再次触发会把它刷新成当前等级的
 * 上限，而不是越叠越多——用户口径「通过这种方式获得的护盾量最多为 x」。
 */
public class BrokenMirrorItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 4.0D, 8.0D };
	private static final double[] SHIELD_BY_LEVEL = { 3.0D, 6.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;
	private static final String SHIELD_KEY = "broken_mirror";
	private static final Map<UUID, Boolean> WAS_IN_COMBAT = new HashMap<>();
	private static final Map<UUID, Integer> LAST_LEVEL = new HashMap<>();

	public BrokenMirrorItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.broken_mirror.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DEFENSE_BY_LEVEL.length - 1;
	}

	@Override
	public double defenseBonus(int level) {
		return valueAt(DEFENSE_BY_LEVEL, level);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(BrokenMirrorItem::tick);
	}

	public static void forget(ServerPlayer player) {
		WAS_IN_COMBAT.remove(player.getUUID());
		LAST_LEVEL.remove(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) {
				continue;
			}

			boolean inCombat = CombatState.isInCombat(player);
			Boolean previousCombat = WAS_IN_COMBAT.put(player.getUUID(), inCombat);
			int level = ArtifactEffects.highestLevelOf(player, ModItems.BROKEN_MIRROR);
			Integer previousLevel = LAST_LEVEL.put(player.getUUID(), level);

			if (inCombat || level < 0) {
				continue;
			}

			// 两种情况给盾：①刚从战斗状态脱离；②神器在脱战状态下被激活（含进服时已经带着）
			boolean justLeftCombat = previousCombat != null && previousCombat;
			boolean justActivated = previousLevel == null || previousLevel < 0;

			if (justLeftCombat || justActivated) {
				ShieldSystem.grant(player, SHIELD_KEY, valueAtStatic(SHIELD_BY_LEVEL, level), 0);
			}
		}
	}

	private static double valueAtStatic(double[] table, int level) {
		return table[Math.max(0, Math.min(table.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		double shield = valueAtStatic(SHIELD_BY_LEVEL, level);
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.broken_mirror",
						Component.literal(Numbers.format(shield)).withColor(COLOUR_BONUS)));
	}
}
