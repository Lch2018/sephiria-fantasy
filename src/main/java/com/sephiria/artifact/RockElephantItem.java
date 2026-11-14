package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArtifactSkill;
import com.sephiria.artifact.skill.RockElephantSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 岩象（守护 + 风之歌，羁绊）：【唯一】发动型神器，发射 3 根岩石矛，
 * 每根造成防御力 × 14/19/25/32/40% 物理伤害；每额外 15% 攻击速度多 1 根。
 * 冷却 15 秒，消耗冲刺次数时减 3 秒。
 */
public class RockElephantItem extends ArtifactItem {
	private static final double[] DAMAGE_PERCENT_BY_LEVEL = { 14.0D, 19.0D, 25.0D, 32.0D, 40.0D };

	public RockElephantItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.GUARD;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.GUARD, ArtifactCombo.WIND_SONG);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.rock_elephant.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return DAMAGE_PERCENT_BY_LEVEL.length - 1;
	}

	public static double damagePercentAt(int level) {
		return DAMAGE_PERCENT_BY_LEVEL[Math.max(0, Math.min(DAMAGE_PERCENT_BY_LEVEL.length - 1, level))];
	}

	@Override
	public ArtifactSkill skill() {
		return RockElephantSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				dashed(Component.translatable("artifact.sephiria_fantasy.affix.rock_elephant_damage",
						Component.literal(Numbers.format(damagePercentAt(level))).withColor(0xFF55FF55))),
				dashed(Component.translatable("artifact.sephiria_fantasy.affix.rock_elephant_count")),
				dashed(Component.translatable("artifact.sephiria_fantasy.affix.rock_elephant_dash")),
				dashed(Component.translatable("artifact.sephiria_fantasy.affix.rock_elephant_cooldown")));
	}
}
