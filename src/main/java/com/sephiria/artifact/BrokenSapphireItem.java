package com.sephiria.artifact;

import com.sephiria.registry.ModItems;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 破碎的蓝宝石（影子，稀有品质）：【唯一】闪避 +2/4/6、防御力 +2/4/6；
 * 每 16/14/12 秒获得一层「帷幕」，必然闪避下一次攻击。
 *
 * <p>帷幕只挡<b>有来源实体的攻击</b>（与 {@code Dodge} 同一条口径：摔落 / 火焰这类环境伤害不挡）。
 * 状态只在内存里，和闪烁的眼睛一样：重登后从下一层开始重新计时。
 */
public class BrokenSapphireItem extends ArtifactItem {
	/** 各等级的闪避（点）。 */
	private static final double[] DODGE_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	/** 各等级的防御力（点）。 */
	private static final double[] DEFENSE_BY_LEVEL = { 2.0D, 4.0D, 6.0D };
	/** 各等级获得一层帷幕的间隔（秒）。 */
	private static final int[] VEIL_SECONDS_BY_LEVEL = { 16, 14, 12 };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	private static final Map<UUID, State> STATES = new HashMap<>();

	public BrokenSapphireItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SHADOW;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.broken_sapphire.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DODGE_BY_LEVEL.length - 1;
	}

	@Override
	public double dodgeBonus(int level) {
		return valueAt(DODGE_BY_LEVEL, level);
	}

	@Override
	public double defenseBonus(int level) {
		return valueAt(DEFENSE_BY_LEVEL, level);
	}

	/** 某等级获得帷幕的间隔（秒）。 */
	private static int veilSeconds(int level) {
		return VEIL_SECONDS_BY_LEVEL[Math.max(0, Math.min(VEIL_SECONDS_BY_LEVEL.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.dodge",
						Component.literal(Numbers.format(this.dodgeBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.broken_sapphire_veil",
						Component.literal(String.valueOf(veilSeconds(level))).withColor(COLOUR_BONUS)));
	}

	/** 注册帷幕计时器（由 {@code Sephiria#onInitialize()} 调用）。 */
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(BrokenSapphireItem::tick);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int level = ArtifactEffects.highestLevelOf(player, ModItems.BROKEN_SAPPHIRE);

			if (level < 0) {
				STATES.remove(player.getUUID());
				continue;
			}

			long now = player.level().getGameTime();
			int interval = veilSeconds(level) * 20;
			State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State());

			if (state.nextTick < 0L) {
				state.nextTick = now + interval;
			}

			if (!state.ready && now >= state.nextTick) {
				state.ready = true;
				playGrant(player);
			}
		}
	}

	/**
	 * 受到攻击前的检查：有帷幕就消耗掉并取消这次伤害。
	 *
	 * <p>只应由「有来源实体」的伤害调用（见 {@code MirageDodgeMixin}）。
	 */
	public static boolean onIncomingDamage(ServerPlayer player) {
		State state = STATES.get(player.getUUID());

		if (state == null || !state.ready) {
			return false;
		}

		int level = ArtifactEffects.highestLevelOf(player, ModItems.BROKEN_SAPPHIRE);

		if (level < 0) {
			STATES.remove(player.getUUID());
			return false;
		}

		state.ready = false;
		state.nextTick = player.level().getGameTime() + veilSeconds(level) * 20L;
		playConsume(player);
		return true;
	}

	/** HUD 用的充能进度：-1 = 没带；1000 = 已有帷幕；其余是距离下一层的比例。 */
	public static int chargeProgress(ServerPlayer player) {
		int level = ArtifactEffects.highestLevelOf(player, ModItems.BROKEN_SAPPHIRE);

		if (level < 0) {
			return -1;
		}

		State state = STATES.get(player.getUUID());

		if (state == null || state.nextTick < 0L) {
			return 0;
		}

		if (state.ready) {
			return 1000;
		}

		int interval = veilSeconds(level) * 20;
		long left = Math.max(0L, state.nextTick - player.level().getGameTime());
		double progress = 1.0D - (double) left / interval;
		return (int) Math.round(Math.max(0.0D, Math.min(1.0D, progress)) * 1000.0D);
	}

	private static void playGrant(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
				18, 0.6D, 0.8D, 0.6D, 0.02D);
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
				6, 0.4D, 0.6D, 0.4D, 0.01D);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.7F);
	}

	private static void playConsume(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
				30, 0.8D, 0.9D, 0.8D, 0.08D);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
	}

	public static void forget(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	private static final class State {
		private boolean ready;
		private long nextTick = -1L;
	}
}
