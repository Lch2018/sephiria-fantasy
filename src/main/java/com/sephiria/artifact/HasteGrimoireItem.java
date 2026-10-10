package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 急速（风之歌，高级品质，魔法书）：<b>获得技能「急速」</b>——攻击速度与移动速度 +15%，
 * 持续 24 秒，冷却 38 秒，蓝耗 36/24/12（0..2 级，等级越高越省蓝）。
 *
 * <p>「获得技能」与「发动型神器」的区别只在数值与蓝耗：魔法书类的技能通常要花蓝，
 * 以后还会吃到「魔法书」相关属性（暂未实现）。技能本身在 {@code artifact.skill.HasteSkill}。
 */
public class HasteGrimoireItem extends ArtifactItem {
	/** 各等级的蓝耗（点）。 */
	private static final double[] MP_COST_BY_LEVEL = { 36.0D, 24.0D, 12.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public HasteGrimoireItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.WIND_SONG;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.haste_grimoire.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return MP_COST_BY_LEVEL.length - 1;
	}

	/** 该等级的蓝耗（点）：急速技能用它。 */
	public static double hasteMpCost(int level) {
		return valueAt(MP_COST_BY_LEVEL, level);
	}

	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return com.sephiria.artifact.skill.HasteSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.haste_skill",
						Component.literal(Numbers.format(hasteMpCost(level))).withColor(COLOUR_BONUS)));
	}
}
