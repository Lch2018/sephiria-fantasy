package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;

/**
 * 陨铁镜（太阳剑，高级品质）：【唯一】太阳剑回收时额外回收 1 个、太阳剑伤害 +3/6/9/12%（0..3 级）。
 *
 * <p>拾取加成由 {@code sun.SunSword#pickUp} 读 {@code ArtifactEffects#sunSwordPickupBonus}；
 * 「太阳剑伤害」与连击档位、索利斯·德库萨那一项相加。
 */
public class MeteoricMirrorItem extends ArtifactItem {
	/** 各等级的回收时额外回收（个）。 */
	private static final int[] PICKUP_BY_LEVEL = { 1, 1, 1, 1 };
	/** 各等级的太阳剑伤害（%）。 */
	private static final double[] DAMAGE_BY_LEVEL = { 3D, 6D, 9D, 12D };

	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public MeteoricMirrorItem(Properties properties) {
		super(properties);
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.SUN_SWORD;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.meteoric_mirror.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return PICKUP_BY_LEVEL.length - 1;
	}

	@Override
	public int sunSwordPickupBonus(int level) {
		return PICKUP_BY_LEVEL[net.minecraft.util.Mth.clamp(level, 0, PICKUP_BY_LEVEL.length - 1)];
	}

	@Override
	public double sunSwordDamagePercentBonus(int level) {
		return valueAt(DAMAGE_BY_LEVEL, level);
	}


	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_pickup",
						Component.literal(Numbers.format(this.sunSwordPickupBonus(level))).withColor(COLOUR_BONUS)),
				Component.translatable("artifact.sephiria_fantasy.affix.sun_sword_damage",
						Component.literal(Numbers.format(this.sunSwordDamagePercentBonus(level))).withColor(COLOUR_BONUS)));
	}
}
