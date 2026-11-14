package com.sephiria.artifact;

import com.sephiria.artifact.skill.FlameArrowSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 火焰箭（余烬，高级品质，魔法书）：【唯一】获得技能「火焰箭」。 */
public class FlameArrowItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 4.0D, 5.0D, 6.0D };
	private static final double[] FIRE_PERCENT_BY_LEVEL = { 24.0D, 48.0D, 72.0D };
	private static final double[] MP_COST_BY_LEVEL = { 15.0D, 17.0D, 19.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FlameArrowItem(Properties properties) {
		super(properties, FlameArrowSkill.INSTANCE, ArtifactCombo.EMBER, "flame_arrow",
				"artifact.sephiria_fantasy.flame_arrow.flavor", FLAT_DAMAGE_BY_LEVEL.length - 1,
				ArtifactRarity.ADVANCED);
	}

	public static double flatAt(int level) {
		return valueAt(FLAT_DAMAGE_BY_LEVEL, level);
	}

	public static double percentAt(int level) {
		return valueAt(FIRE_PERCENT_BY_LEVEL, level);
	}

	public static double mpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.flame_arrow_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.flame_arrow_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.flame_arrow_burn",
						Component.literal("2").withColor(COLOUR_BONUS)),
				skillCooldown(9),
				skillMp(mpCost(level)));
	}
}
