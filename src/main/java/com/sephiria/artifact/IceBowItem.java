package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 冰弓（冰霜武具，传说品质）：【唯一】<b>发动型神器</b>——0.8 秒自动装填 1 发（最多 7 发），
 * 发动时向准星方向发射所有储存的箭矢，每发造成 4/5/6/7/8/9 + 冰元素强度 × 20/25/30/35/40/50%
 * 的冰属性伤害（0..5 级，共 6 档）。
 *
 * <p>箭矢与弹药都在 {@code frost.FrostArmament}：弹药每 0.8 秒回 1 发（上限 7，与连击档位无关），
 * HUD 那行显示成「弓x/7」；技能本体是 {@code artifact.skill.IceBowSkill}（发动型不耗蓝，
 * 冷却只有 0.5 秒——真正的限制是弹药）。
 *
 * <p>箭矢是直线飞行的实体：撞到第一个有威胁生物就结算伤害并消失（不贯穿），会被方块挡住。
 */
public class IceBowItem extends ArtifactItem {
	/** 各等级的固定伤害（点，每发）。 */
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 4.0D, 5.0D, 6.0D, 7.0D, 8.0D, 9.0D };
	/** 各等级的「冰元素强度」倍率（%，每发）。 */
	private static final double[] ICE_PERCENT_BY_LEVEL = { 20.0D, 25.0D, 30.0D, 35.0D, 40.0D, 50.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public IceBowItem(Properties properties) {
		super(properties);
	}

	/** 该等级的固定伤害（点，每发；充能系统按它结算）。 */
	public static double flatDamageAt(int level) {
		return FLAT_DAMAGE_BY_LEVEL[Mth.clamp(level, 0, FLAT_DAMAGE_BY_LEVEL.length - 1)];
	}

	/** 该等级的「冰元素强度」倍率（%，每发）。 */
	public static double icePercentAt(int level) {
		return ICE_PERCENT_BY_LEVEL[Mth.clamp(level, 0, ICE_PERCENT_BY_LEVEL.length - 1)];
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.LEGENDARY;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.ice_bow.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return FLAT_DAMAGE_BY_LEVEL.length - 1;
	}

	/** 发动型神器：技能「冰弓」由 {@code artifact.skill.IceBowSkill} 实现。 */
	@Override
	public com.sephiria.artifact.skill.ArtifactSkill skill() {
		return com.sephiria.artifact.skill.IceBowSkill.INSTANCE;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.ice_bow",
				Component.literal(Numbers.format(flatDamageAt(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(icePercentAt(level))).withColor(COLOUR_BONUS)));
	}
}
