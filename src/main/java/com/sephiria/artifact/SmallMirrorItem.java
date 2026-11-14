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
 * 小镜子（守护，稀有）：【唯一】使用冲刺时获得 3/3/6 点护盾（持续 5 秒，冷却 5 秒）；防御力 +3/6/9。
 *
 * <p>盾按来源键覆盖：5 秒冷却与 5 秒持续刚好首尾相接，不会叠成两层。
 */
public class SmallMirrorItem extends ArtifactItem {
	private static final double[] DEFENSE_BY_LEVEL = { 3.0D, 6.0D, 9.0D };
	private static final double[] SHIELD_BY_LEVEL = { 3.0D, 3.0D, 6.0D };
	private static final int DURATION_TICKS = 100;
	private static final int COOLDOWN_TICKS = 100;
	private static final int COLOUR_BONUS = 0xFF55FF55;
	private static final String SHIELD_KEY = "small_mirror";
	private static final Map<UUID, Long> LAST_DASH = new HashMap<>();

	public SmallMirrorItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.small_mirror.flavor";
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

	/** 冲刺钩子（由 {@code DashSkill#perform} 调用）。 */
	public static void onDash(ServerPlayer player) {
		int level = ArtifactEffects.highestLevelOf(player, ModItems.SMALL_MIRROR);

		if (level < 0) {
			return;
		}

		long now = player.level().getGameTime();
		Long last = LAST_DASH.get(player.getUUID());

		if (last != null && now - last < COOLDOWN_TICKS) {
			return;
		}

		LAST_DASH.put(player.getUUID(), now);
		ShieldSystem.grant(player, SHIELD_KEY,
				SHIELD_BY_LEVEL[Math.max(0, Math.min(SHIELD_BY_LEVEL.length - 1, level))], DURATION_TICKS);
		com.sephiria.artifact.ArtifactHud.send(player);
	}

	public static void forget(ServerPlayer player) {
		LAST_DASH.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.small_mirror",
						Component.literal(Numbers.format(SHIELD_BY_LEVEL[
								Math.max(0, Math.min(SHIELD_BY_LEVEL.length - 1, level))])).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)));
	}
}
