package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 锅盖（守护，稀有）：【唯一】MP再生 +2/4/6/8/11/15、闪避 +1/2/3/4/5/6；
 * 每次对敌人造成物理暴击获得「忍耐」：防御力 +3，持续 6 秒，最多 3 层。
 *
 * <p>「忍耐」的层数只在这里记，防御力在 {@link com.sephiria.stats.PlayerStats#defenseTotal}
 * 里加上去；到期不主动清，下一次查询时顺手丢掉。
 */
public class PotLidItem extends ArtifactItem {
	private static final double[] MP_REGEN_BY_LEVEL = { 0.2D, 0.4D, 0.6D, 0.8D, 1.1D, 1.5D };
	private static final double[] DODGE_BY_LEVEL = { 1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D };
	private static final int BUFF_DURATION_TICKS = 120;
	private static final int MAX_STACKS = 3;
	private static final double DEFENSE_PER_STACK = 3.0D;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private record Buff(int stacks, long expireTick) {
	}

	private static final Map<UUID, Buff> BUFFS = new HashMap<>();

	public PotLidItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.pot_lid.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MP_REGEN_BY_LEVEL.length - 1;
	}

	@Override
	public double mpRegenBonus(int level) {
		return valueAt(MP_REGEN_BY_LEVEL, level);
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	/** 物理暴击钩子（由 {@code SephiriaDamage#applyCrit} 调用）。 */
	public static void onPhysicalCrit(ServerPlayer player) {
		if (ArtifactEffects.highestLevelOf(player, ModItems.POT_LID) < 0) {
			return;
		}

		long now = player.level().getGameTime();
		Buff buff = BUFFS.get(player.getUUID());
		int stacks = buff == null || now >= buff.expireTick() ? 0 : buff.stacks();
		BUFFS.put(player.getUUID(), new Buff(Math.min(MAX_STACKS, stacks + 1), now + BUFF_DURATION_TICKS));

		// 防御力面板每秒对账一次，这里主动推一次让玩家立刻看到层数带来的变化
		com.sephiria.stats.PlayerStats.sync(player);
	}

	/** 「忍耐」当前提供的防御力（点）。 */
	public static double buffDefense(ServerPlayer player) {
		Buff buff = BUFFS.get(player.getUUID());

		if (buff == null) {
			return 0.0D;
		}

		if (player.level().getGameTime() >= buff.expireTick()) {
			BUFFS.remove(player.getUUID());
			return 0.0D;
		}

		return buff.stacks() * DEFENSE_PER_STACK;
	}

	public static void forget(ServerPlayer player) {
		BUFFS.remove(player.getUUID());
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.mp_regen",
						Component.literal(Numbers.format(this.mpRegenBonus(level) * 10.0D)).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.pot_lid"));
	}
}
