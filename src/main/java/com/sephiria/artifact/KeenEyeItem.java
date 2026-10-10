package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 锐利之眼（精密，高级品质，魔法书）：<b>获得技能「锐利之眼」</b>——暴击几率 +10/13/16%，
 * 持续 10/15/20 秒，冷却 30 秒，蓝耗 24/16/6（0..2 级，等级越高又省蓝又强又久）。
 *
 * <p>三张表都是「等级越高越好」：暴击与时长变长，蓝耗变低。技能本体在 {@code artifact.skill.KeenEyeSkill}，
 * 限时暴击加成走 {@code TimedAttributes#grantStat}（暴击几率不是原版属性，挂不进临时修饰符）。
 */
public class KeenEyeItem extends ArtifactItem {
	/** 各等级的蓝耗（点）。 */
	private static final double[] MP_COST_BY_LEVEL = { 24.0D, 16.0D, 6.0D };
	/** 各等级的暴击几率加成（百分点）。 */
	private static final double[] CRIT_BONUS_BY_LEVEL = { 10.0D, 13.0D, 16.0D };
	/** 各等级的持续时间（秒）。 */
	private static final int[] DURATION_SECONDS_BY_LEVEL = { 10, 15, 20 };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public KeenEyeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.PRECISION;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.keen_eye.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return CRIT_BONUS_BY_LEVEL.length - 1;
	}

	/** 该等级的蓝耗（点）：锐利之眼技能用它。 */
	public static double keenMpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	/** 该等级的暴击几率加成（百分点）：技能发动时按它给限时加成。 */
	public static double critBonusPercent(int level) {
		return valueAt(CRIT_BONUS_BY_LEVEL, level);
	}

	/** 该等级的持续时间（秒）：技能本体与提示框共用。 */
	public static int durationSeconds(int level) {
		return DURATION_SECONDS_BY_LEVEL[Mth.clamp(level, 0, DURATION_SECONDS_BY_LEVEL.length - 1)];
	}

	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return com.sephiria.artifact.skill.KeenEyeSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.keen_eye_skill",
						Component.literal(Numbers.format(this.critBonusPercent(level))).withColor(COLOUR_BONUS),
						Component.literal(String.valueOf(durationSeconds(level))),
						Component.literal(Numbers.format(keenMpCost(level))).withColor(COLOUR_BONUS)));
	}
}
