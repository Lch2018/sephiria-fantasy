package com.sephiria.artifact;

import com.sephiria.artifact.skill.ArrowRainSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 箭雨（冰川，稀有品质，魔法书）：【唯一】获得技能「箭雨」。 */
public class ArrowRainItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 2.0D, 3.0D, 4.0D, 5.0D };
	private static final double[] ICE_PERCENT_BY_LEVEL = { 8.0D, 14.0D, 20.0D, 26.0D };
	private static final double[] MP_COST_BY_LEVEL = { 22.0D, 24.0D, 26.0D, 28.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ArrowRainItem(Properties properties) {
		super(properties, ArrowRainSkill.INSTANCE, ArtifactCombo.GLACIER, "arrow_rain",
				"artifact.sephiria_fantasy.arrow_rain.flavor", FLAT_DAMAGE_BY_LEVEL.length - 1,
				ArtifactRarity.RARE);
	}

	public static double flatAt(int level) {
		return valueAt(FLAT_DAMAGE_BY_LEVEL, level);
	}

	public static double percentAt(int level) {
		return valueAt(ICE_PERCENT_BY_LEVEL, level);
	}

	public static double mpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.arrow_rain_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.arrow_rain_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.arrow_rain_frostbite",
						Component.literal("66").withColor(COLOUR_BONUS)),
				skillCooldown(16),
				skillMp(mpCost(level)));
	}
}
