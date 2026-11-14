package com.sephiria.artifact;

import com.sephiria.ability.Invulnerability;
import com.sephiria.registry.ModItems;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 白边面包（守护，传说）：【唯一】受伤后获得 0.8/1.0/1.2 秒无敌。
 *
 * <p>用 {@link Invulnerability} 的通用无敌窗口，而不是原版受击冷却：这样后续伤害会被
 * 完全挡掉，也会走无敌 HUD（无敌条）。在 {@code AFTER_DAMAGE} 里授予，所以当前这一下
 * 照常结算，从下一 tick 开始无敌。
 */
public class WhiteEdgeBreadItem extends ArtifactItem {
	private static final int[] INVULNERABLE_TICKS_BY_LEVEL = { 16, 20, 24 };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WhiteEdgeBreadItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.white_edge_bread.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return INVULNERABLE_TICKS_BY_LEVEL.length - 1;
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player) || damageTaken <= 0.0F) {
				return;
			}

			int ticks = invulnerableTicks(player);

			if (ticks > 0) {
				Invulnerability.grant(player, ticks);
			}
		});
	}

	public static int invulnerableTicks(ServerPlayer player) {
		int level = ArtifactEffects.highestLevelOf(player, ModItems.WHITE_EDGE_BREAD);

		if (level < 0) {
			return 0;
		}

		return INVULNERABLE_TICKS_BY_LEVEL[Math.max(0, Math.min(INVULNERABLE_TICKS_BY_LEVEL.length - 1, level))];
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		double seconds = INVULNERABLE_TICKS_BY_LEVEL[
				Math.max(0, Math.min(INVULNERABLE_TICKS_BY_LEVEL.length - 1, level))] / 20.0D;
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.white_edge_bread",
				Component.literal(Numbers.format(seconds)).withColor(COLOUR_BONUS)));
	}
}
