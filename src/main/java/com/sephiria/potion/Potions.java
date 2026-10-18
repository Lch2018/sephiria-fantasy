package com.sephiria.potion;

import com.sephiria.artifact.ArtifactRarity;

/**
 * 九种药水的效果数值：改平衡只动这个文件。
 *
 * <p>品质决定名字颜色与商店价（白 25 / 蓝 1500 / 黄 5043 / 红不卖）。
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

	private Potions() {
	}
}
