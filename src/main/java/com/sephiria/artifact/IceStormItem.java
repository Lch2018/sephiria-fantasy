package com.sephiria.artifact;

import com.sephiria.artifact.skill.IceStormSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 冰风暴（冰川，传说品质，魔法书）：【唯一】获得技能「冰风暴」。 */
public class IceStormItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 1.0D };
	private static final double[] ICE_PERCENT_BY_LEVEL = { 14.0D, 17.0D, 20.0D, 23.0D, 26.0D };
	private static final double[] MP_COST_BY_LEVEL = { 28.0D, 31.0D, 34.0D, 37.0D, 37.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public IceStormItem(Properties properties) {
		super(properties, IceStormSkill.INSTANCE, ArtifactCombo.GLACIER, "ice_storm",
				"artifact.sephiria_fantasy.ice_storm.flavor", ICE_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.LEGENDARY);
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
				Component.translatable("artifact.sephiria_fantasy.affix.ice_storm_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_storm_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_storm_interval"),
				Component.translatable("artifact.sephiria_fantasy.affix.ice_storm_duration"),
				skillCooldown(22),
				skillMp(mpCost(level)));
	}
}
