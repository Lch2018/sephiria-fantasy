package com.sephiria.potion;

import com.sephiria.artifact.ArtifactRarity;

/**
 * 五种药水的效果数值：改平衡只动这个文件。
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

	/** 各药水的品质（顺序与 {@link com.sephiria.registry.ModItems#POTIONS} 一致）。 */
	public static final ArtifactRarity REGENERATION_RARITY = ArtifactRarity.COMMON;
	public static final ArtifactRarity APPLE_JUICE_RARITY = ArtifactRarity.COMMON;
	public static final ArtifactRarity TRAPPIST_SACRED_RARITY = ArtifactRarity.ADVANCED;
	public static final ArtifactRarity BIG_DICE_RARITY = ArtifactRarity.RARE;
	public static final ArtifactRarity VAMPIRE_LORD_OATH_RARITY = ArtifactRarity.LEGENDARY;

	private Potions() {
	}
}
