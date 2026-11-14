package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArtifactSkill;
import com.sephiria.artifact.skill.WaterGrabSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 水之精灵（湖泊 + 精密，羁绊，【唯一】）：发动型神器，消耗 30% 最大 MP，
 * 在准星位置施放 WaterGrab；伤害 = 最大 MP × 32/48/64/80/100%，额外暴击 +10/15/20/25/30%。
 */
public class WaterSpiritItem extends ArtifactItem {
	private static final double[] DAMAGE_PERCENT_BY_LEVEL = { 32.0D, 48.0D, 64.0D, 80.0D, 100.0D };
	private static final double[] CRIT_BY_LEVEL = { 10.0D, 15.0D, 20.0D, 25.0D, 30.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public WaterSpiritItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.LAKE;
	}

	@Override
	public java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(ArtifactCombo.LAKE, ArtifactCombo.PRECISION);
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.BOND;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.water_spirit.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return 4;
	}

	@Override
	public ArtifactSkill skill() {
		return WaterGrabSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		int clamped = Math.max(0, Math.min(DAMAGE_PERCENT_BY_LEVEL.length - 1, level));
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.water_spirit_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.water_spirit_damage",
						Component.literal(Numbers.format(DAMAGE_PERCENT_BY_LEVEL[clamped])).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.water_spirit_crit",
						Component.literal(Numbers.format(CRIT_BY_LEVEL[clamped])).withColor(COLOUR_BONUS)));
	}
}
