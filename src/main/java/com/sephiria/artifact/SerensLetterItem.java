package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.stats.ShieldSystem;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 瑟伦潦草的信（守护，稀有）：【唯一】每累计消耗 40 MP，获得 4/6/8 点护盾（持续 15 秒）。
 *
 * <p>任何状态下都累计（用户 2026-11-14 改了：原来只在战斗状态计数，且脱战清零，
 * 实际上很难触发）。每跨过一个 40 MP 阈值就发一层盾；护盾可以叠多层，15 秒后各自过期。
 */
public class SerensLetterItem extends ArtifactItem {
	private static final double[] SHIELD_BY_LEVEL = { 4.0D, 6.0D, 8.0D };
	private static final double MP_THRESHOLD = 40.0D;
	private static final int SHIELD_DURATION_TICKS = 300;
	private static final int COLOUR_BONUS = 0xFF55FF55;
	private static final Map<UUID, Double> SPENT = new HashMap<>();

	public SerensLetterItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.serens_letter.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return SHIELD_BY_LEVEL.length - 1;
	}

	/** MP 消耗钩子（由 {@code PlayerStats#spendMp} 调用）。 */
	public static void onMpSpent(ServerPlayer player, double amount) {
		if (amount <= 0.0D) {
			return;
		}

		int level = ArtifactEffects.highestLevelOf(player, ModItems.SERENS_LETTER);

		if (level < 0) {
			SPENT.remove(player.getUUID());
			return;
		}

		double total = SPENT.getOrDefault(player.getUUID(), 0.0D) + amount;
		double shield = SHIELD_BY_LEVEL[Math.max(0, Math.min(SHIELD_BY_LEVEL.length - 1, level))];
		boolean granted = false;

		while (total >= MP_THRESHOLD) {
			total -= MP_THRESHOLD;
			ShieldSystem.grant(player, shield, SHIELD_DURATION_TICKS);
			granted = true;
		}

		if (total <= 1.0E-6D) {
			SPENT.remove(player.getUUID());
		} else {
			SPENT.put(player.getUUID(), total);
		}

		if (granted) {
			com.sephiria.artifact.ArtifactHud.send(player);
		}
	}

	public static void forget(ServerPlayer player) {
		SPENT.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.serens_letter",
				Component.literal(Numbers.format(SHIELD_BY_LEVEL[
						Math.max(0, Math.min(SHIELD_BY_LEVEL.length - 1, level))])).withColor(COLOUR_BONUS)));
	}
}
