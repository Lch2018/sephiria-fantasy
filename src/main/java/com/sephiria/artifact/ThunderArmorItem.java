package com.sephiria.artifact;

import com.sephiria.artifact.skill.ThunderArmorSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 雷霆之甲（魔法科技，稀有品质，魔法书）：【唯一】获得技能「雷霆之甲」。 */
public class ThunderArmorItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 1.0D };
	private static final double[] LIGHTNING_PERCENT_BY_LEVEL = { 12.0D, 14.0D, 16.0D, 19.0D, 22.0D };
	private static final double[] MP_COST_BY_LEVEL = { 36.0D, 33.0D, 30.0D, 30.0D, 30.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ThunderArmorItem(Properties properties) {
		super(properties, ThunderArmorSkill.INSTANCE, ArtifactCombo.MAGIC_TECH, "thunder_armor",
				"artifact.sephiria_fantasy.thunder_armor.flavor", LIGHTNING_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.RARE);
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
				Component.translatable("artifact.sephiria_fantasy.affix.thunder_armor_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.thunder_armor_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.thunder_armor_interval"),
				Component.translatable("artifact.sephiria_fantasy.affix.thunder_armor_duration"),
				skillCooldown(25),
				skillMp(mpCost(level)));
	}
}
