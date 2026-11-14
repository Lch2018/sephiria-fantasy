package com.sephiria.artifact;

import com.sephiria.artifact.skill.FrostKnifeSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 霜刀（冰川，高级品质，魔法书）：【唯一】获得技能「霜刀」。 */
public class FrostKnifeItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 5.0D };
	private static final double[] ICE_PERCENT_BY_LEVEL = { 18.0D, 23.0D, 30.0D, 38.0D, 44.0D };
	private static final double[] MP_COST_BY_LEVEL = { 10.0D, 11.0D, 12.0D, 13.0D, 14.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FrostKnifeItem(Properties properties) {
		super(properties, FrostKnifeSkill.INSTANCE, ArtifactCombo.GLACIER, "frost_knife",
				"artifact.sephiria_fantasy.frost_knife.flavor", ICE_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.ADVANCED);
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
				Component.translatable("artifact.sephiria_fantasy.affix.frost_knife_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.frost_knife_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				skillCooldown(10),
				skillMp(mpCost(level)));
	}
}
