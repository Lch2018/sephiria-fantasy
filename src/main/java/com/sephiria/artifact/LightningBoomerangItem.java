package com.sephiria.artifact;

import com.sephiria.artifact.skill.LightningBoomerangSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 雷电回旋镖（魔法科技，高级品质，魔法书）：【唯一】获得技能「雷电回旋镖」。 */
public class LightningBoomerangItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 6.0D };
	private static final double[] LIGHTNING_PERCENT_BY_LEVEL = { 36.0D, 48.0D, 62.0D, 76.0D, 90.0D };
	private static final double[] MP_COST_BY_LEVEL = { 9.0D, 10.0D, 11.0D, 12.0D, 12.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public LightningBoomerangItem(Properties properties) {
		super(properties, LightningBoomerangSkill.INSTANCE, ArtifactCombo.MAGIC_TECH, "lightning_boomerang",
				"artifact.sephiria_fantasy.lightning_boomerang.flavor", LIGHTNING_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.ADVANCED);
	}

	public static double flatAt(int level) {
		return valueAt(FLAT_DAMAGE_BY_LEVEL, level);
	}

	public static double percentAt(int level) {
		return valueAt(LIGHTNING_PERCENT_BY_LEVEL, level);
	}

	public static double mpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_boomerang_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.lightning_boomerang_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				skillCooldown(10),
				skillMp(mpCost(level)));
	}
}
