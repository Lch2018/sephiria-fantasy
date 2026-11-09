package com.sephiria.potion;

import com.sephiria.artifact.ArtifactRarity;

/**
 * 二十一种药水的效果数值：改平衡只动这个文件。
 *
 * <p>品质决定名字颜色与商店价（白 50 / 蓝 3000 / 黄 10086 / 红不卖）。
 */
public final class Potions {
	/** 再生药水（白）：立即回复最大生命值的 20%。 */
	public static final PotionEffect REGENERATION =
			new PotionEffect(PotionEffect.Kind.HEAL_PERCENT, 20.0D, 0);
	/** 苹果汁（白）：每秒回 1 点生命，持续 30 秒（共 30 点）。 */
	public static final PotionEffect APPLE_JUICE =
			new PotionEffect(PotionEffect.Kind.HEAL_PER_SECOND, 1.0D, 30);
	/** 特拉普派的神圣（蓝）：物理伤害 +2，永久。 */
	public static final PotionEffect TRAPPIST_SACRED =
			new PotionEffect(PotionEffect.Kind.PHYSICAL, 2.0D, 0);
	/** 大骰子药水（黄）：立即获得 3 个骰子。 */
	public static final PotionEffect BIG_DICE =
			new PotionEffect(PotionEffect.Kind.DICE, 3.0D, 0);
	/** 吸血鬼领主的誓约（红）：HP 偷取 +1，永久。 */
	public static final PotionEffect VAMPIRE_LORD_OATH =
			new PotionEffect(PotionEffect.Kind.LIFESTEAL, 1.0D, 0);
	/** 索莱拉的偶然（黄）：随机一种连击 +1 级，永久（用户 2026-10-18 新增）。 */
	public static final PotionEffect SOLERA_CHANCE =
			new PotionEffect(PotionEffect.Kind.COMBO, 1.0D, 0);
	/** 梅尔岑之烟（蓝）：火元素强度 +2，永久。 */
	public static final PotionEffect MELTZEN_SMOKE =
			new PotionEffect(PotionEffect.Kind.FIRE, 2.0D, 0);
	/** 阿凡提努斯的水晶（蓝）：冰元素强度 +2，永久。 */
	public static final PotionEffect AVANTINUS_CRYSTAL =
			new PotionEffect(PotionEffect.Kind.ICE, 2.0D, 0);
	/** 高泽之晨（蓝）：电元素强度 +2，永久。 */
	public static final PotionEffect GAOZE_MORNING =
			new PotionEffect(PotionEffect.Kind.LIGHTNING, 2.0D, 0);

	/** 马迪朗的平静（蓝）：防御力 +3，永久。 */
	public static final PotionEffect MADIRAN_CALM =
			new PotionEffect(PotionEffect.Kind.DEFENSE, 3.0D, 0);
	/** 格里塞特的浅淡（蓝）：闪避 +3，永久。 */
	public static final PotionEffect GRISETTE_PALE =
			new PotionEffect(PotionEffect.Kind.DODGE, 3.0D, 0);
	/** 贵兹的合奏（蓝）：最高元素伤害 +2，永久。 */
	public static final PotionEffect GEWURZ_ENSEMBLE =
			new PotionEffect(PotionEffect.Kind.HIGHEST_ELEMENT, 2.0D, 0);
	/** 内比奥罗的执着（黄）：物理伤害 +4，永久。 */
	public static final PotionEffect NEBBIOLO_PERSISTENCE =
			new PotionEffect(PotionEffect.Kind.PHYSICAL, 4.0D, 0);
	/** 乌尼·布兰的雾霭（黄）：冰属性伤害 +4，永久。 */
	public static final PotionEffect UGNI_BLANC_MIST =
			new PotionEffect(PotionEffect.Kind.ICE, 4.0D, 0);
	/** Tempranillo 的热情（黄）：火焰属性伤害 +4，永久。 */
	public static final PotionEffect TEMPRANILLO_PASSION =
			new PotionEffect(PotionEffect.Kind.FIRE, 4.0D, 0);
	/** 马尔贝克的浓郁（黄）：闪电属性伤害 +4，永久。 */
	public static final PotionEffect MALBEC_RICHNESS =
			new PotionEffect(PotionEffect.Kind.LIGHTNING, 4.0D, 0);
	/** 蘑菇汤（黄）：最高元素伤害 +6，永久；并列时随机只加一种。 */
	public static final PotionEffect MUSHROOM_SOUP =
			new PotionEffect(PotionEffect.Kind.HIGHEST_ELEMENT, 6.0D, 0);
	/** 马德拉的坚韧（黄）：最终 HP +5%，永久。 */
	public static final PotionEffect MADEIRA_FORTITUDE =
			new PotionEffect(PotionEffect.Kind.MAX_HP_PERCENT, 5.0D, 0);
	/** 萨格兰蒂诺的忍耐（黄）：防御力 +5，永久。 */
	public static final PotionEffect SAGRANTINO_PATIENCE =
			new PotionEffect(PotionEffect.Kind.DEFENSE, 5.0D, 0);
	/** 普萨的留白（黄）：闪避 +5，永久。 */
	public static final PotionEffect POULSARD_WHITESPACE =
			new PotionEffect(PotionEffect.Kind.DODGE, 5.0D, 0);
	/** 黎明之露（红）：所有伤害放大 +4%，永久。 */
	public static final PotionEffect DAWN_DEW =
			new PotionEffect(PotionEffect.Kind.ALL_DAMAGE_AMP, 4.0D, 0);

	/** 各药水的品质（顺序与 {@link com.sephiria.registry.ModItems#POTIONS} 一致）。 */
	public static final ArtifactRarity REGENERATION_RARITY = ArtifactRarity.COMMON;
	public static final ArtifactRarity APPLE_JUICE_RARITY = ArtifactRarity.COMMON;
	public static final ArtifactRarity TRAPPIST_SACRED_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity BIG_DICE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity VAMPIRE_LORD_OATH_RARITY = ArtifactRarity.LEGENDARY;
	public static final ArtifactRarity SOLERA_CHANCE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity MELTZEN_SMOKE_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity AVANTINUS_CRYSTAL_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity GAOZE_MORNING_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity MADIRAN_CALM_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity GRISETTE_PALE_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity GEWURZ_ENSEMBLE_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity NEBBIOLO_PERSISTENCE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity UGNI_BLANC_MIST_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity TEMPRANILLO_PASSION_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity MALBEC_RICHNESS_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity MUSHROOM_SOUP_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity MADEIRA_FORTITUDE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity SAGRANTINO_PATIENCE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity POULSARD_WHITESPACE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity DAWN_DEW_RARITY = ArtifactRarity.LEGENDARY;

	private Potions() {
	}
}
