package com.sephiria.artifact;

import com.sephiria.artifact.skill.FireCircusSkill;
import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/** 火焰马戏团（余烬，稀有品质，魔法书）：【唯一】获得技能「火焰马戏团」。 */
public class FireCircusItem extends AdvancedMagicBookItem {
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 4.0D };
	private static final double[] FIRE_PERCENT_BY_LEVEL = { 11.0D, 15.0D, 20.0D, 26.0D };
	private static final double[] CRIT_BONUS_BY_LEVEL = { 10.0D, 16.0D, 22.0D, 30.0D };
	private static final double[] MP_COST_BY_LEVEL = { 16.0D, 18.0D, 19.0D, 20.0D };
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public FireCircusItem(Properties properties) {
		super(properties, FireCircusSkill.INSTANCE, ArtifactCombo.EMBER, "fire_circus",
				"artifact.sephiria_fantasy.fire_circus.flavor", FIRE_PERCENT_BY_LEVEL.length - 1,
				ArtifactRarity.RARE);
	}

	public static double flatAt(int level) {
		return valueAt(FLAT_DAMAGE_BY_LEVEL, level);
	}

	public static double percentAt(int level) {
		return valueAt(FIRE_PERCENT_BY_LEVEL, level);
	}

	public static double critBonus(int level) {
		return valueAt(CRIT_BONUS_BY_LEVEL, level);
	}

	public static double mpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	protected java.util.List<Component> descriptionLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.fire_circus_effect"),
				Component.translatable("artifact.sephiria_fantasy.affix.fire_circus_damage",
						Component.literal(Numbers.format(flatAt(level))).withColor(COLOUR_BONUS),
						Component.literal(Numbers.format(percentAt(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.fire_circus_crit",
						Component.literal(Numbers.format(critBonus(level))).withColor(COLOUR_BONUS)),
				skillCooldown(10),
				skillMp(mpCost(level)));
	}
}
