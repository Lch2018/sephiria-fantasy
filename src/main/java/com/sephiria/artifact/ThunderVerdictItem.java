package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 雷之裁决（魔法科技，传说品质，魔法书）：【唯一】获得技能「雷之裁决」——
 * 向准星方向释放 8×8×30 的电击光束攻击 10 次，单次伤害 = 6 + 电元素强度 × 50/60/70/80/90%，
 * 冷却 25 秒，消耗 MP 24/28/32/36/36（0..4 级，等级越高越强）。
 *
 * <p>技能本体在 {@code artifact.skill.ThunderVerdictSkill}；光束是<b>电属性伤害</b>
 * （{@code Kind.ELECTRIC}）：吃麒麟的角的「电属性攻击的暴击几率」，也吃以后要做的魔法书加成。
 *
 * <p>「范围」按现有口径写（格数 × 100）：光束长 30 格 → 3000；宽高 8 格写在说明里。
 */
public class ThunderVerdictItem extends ArtifactItem {
	/** 各等级的蓝耗（点）。 */
	private static final double[] MP_COST_BY_LEVEL = { 24.0D, 28.0D, 32.0D, 36.0D, 36.0D };
	/** 各等级单次伤害里「电元素强度」的倍率（%）。 */
	private static final double[] DAMAGE_PERCENT_BY_LEVEL = { 50.0D, 60.0D, 70.0D, 80.0D, 90.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ThunderVerdictItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.MAGIC_TECH;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.thunder_verdict.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MP_COST_BY_LEVEL.length - 1;
	}

	/** 该等级的蓝耗（点）：雷之裁决技能用它。 */
	public static double thunderMpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	/** 该等级单次伤害里「电元素强度」的倍率（%）。 */
	public static double damagePercent(int level) {
		return valueAt(DAMAGE_PERCENT_BY_LEVEL, level);
	}

	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return com.sephiria.artifact.skill.ThunderVerdictSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.thunder_verdict_skill",
				Component.literal(Numbers.format(damagePercent(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(thunderMpCost(level))).withColor(COLOUR_BONUS)));
	}
}
