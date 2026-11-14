package com.sephiria.artifact;

import com.sephiria.artifact.skill.MeteorShowerSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 陨石雨（余烬，传说品质，魔法书）：【唯一】获得技能「陨石雨」。 */
public class MeteorShowerItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 5.0D };
	private static final double[] FIRE_PERCENT_BY_LEVEL = { 10.0D, 20.0D, 36.0D, 40.0D };
	private static final int[] COUNT_BY_LEVEL = { 30, 35, 40, 45 };
	private static final double[] MP_COST_BY_LEVEL = { 35.0D, 38.0D, 41.0D, 44.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MeteorShowerItem(Properties properties) {
		super(properties, MeteorShowerSkill.INSTANCE, ArtifactCombo.EMBER, "meteor_shower",
				"artifact.sephiria_fantasy.meteor_shower.flavor", FIRE_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.LEGENDARY);
	}

	public static double flatAt(int level) {
		return valueAt(FLAT_DAMAGE_BY_LEVEL, level);
	}

	public static double percentAt(int level) {
		return valueAt(FIRE_PERCENT_BY_LEVEL, level);
	}

	public static int countAt(int level) {
		return COUNT_BY_LEVEL[Math.max(0, Math.min(COUNT_BY_LEVEL.length - 1, level))];
	}

	public static double mpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.meteor_shower_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.meteor_shower_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.meteor_shower_count",
						Component.literal(String.valueOf(countAt(level))).withColor(COLOUR_BONUS)),
				skillCooldown(26),
				skillMp(mpCost(level)));
	}
}
