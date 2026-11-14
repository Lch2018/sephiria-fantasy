package com.sephiria.artifact;

import com.sephiria.damage.SephiriaDamage;
import com.sephiria.registry.ModItems;
import com.sephiria.stats.PlayerStats;
import com.sephiria.util.Numbers;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 荆棘壳（守护，高级）：【唯一】荆棘 +150/300/450%、防御力 +1/2/3。
 *
 * <p>荆棘伤害 = 玩家防御力 × 荆棘% × 0.1，物理属性，反给伤害来源。
 * 只有真正扣到血的那一下才反伤（护盾全吃掉时玩家没受伤，也就不弹）。
 */
public class ThornShellItem extends ArtifactItem {
	private static final double[] THORN_BY_LEVEL = { 150.0D, 300.0D, 450.0D };
	private static final double[] DEFENSE_BY_LEVEL = { 1.0D, 2.0D, 3.0D };
	private static final double THORN_SCALE = 0.1D;
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ThornShellItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.thorn_shell.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return THORN_BY_LEVEL.length - 1;
	}

	@Override
	public double defenseBonus(int level) {
		return valueAt(DEFENSE_BY_LEVEL, level);
	}

	public double thornPercent(int level) {
		return valueAt(THORN_BY_LEVEL, level);
	}

	/** 由 {@code Sephiria#onInitialize()} 调用。 */
	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, blockedDamage, damageTaken, blocked) -> {
			if (!(entity instanceof ServerPlayer player) || damageTaken <= 0.0F
					|| source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
				return;
			}

			int level = ArtifactEffects.highestLevelOf(player, ModItems.THORN_SHELL);

			if (level < 0) {
				return;
			}

			Entity attacker = source.getEntity();

			if (!(attacker instanceof LivingEntity living) || living == player) {
				return;
			}

			double thorn = THORN_BY_LEVEL[Math.max(0, Math.min(THORN_BY_LEVEL.length - 1, level))];
			double damage = PlayerStats.defenseTotal(player) * thorn / 100.0D * THORN_SCALE;

			if (damage <= 0.0D) {
				return;
			}

			SephiriaDamage.clearDamageCooldown(living);
			living.hurtServer((ServerLevel) player.level(), player.level().damageSources().thorns(player),
					(float) damage);
		});
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.thorn",
						Component.literal(Numbers.format(this.thornPercent(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.defense",
						Component.literal(Numbers.format(this.defenseBonus(level))).withColor(COLOUR_BONUS)));
	}
}
