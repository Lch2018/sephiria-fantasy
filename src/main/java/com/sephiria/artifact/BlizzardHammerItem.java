package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 暴风雪之锤（冰霜武具，高级品质）：【唯一】<b>战斗中</b>每 {@code FrostArmament.HAMMER_CHARGE_TICKS}
 * （7 秒）充能一次；充能后<b>冲刺</b>时朝最近的敌人扔出<b>一把</b>锤子——伤害 = 固定值
 * 6/8/10/12/14/16 + 冰元素强度 × 50/70/80/100/120/130%（0..5 级，共 6 档），
 * 锤子落地时周围一圈（AOE）都吃这一下。
 *
 * <p>索敌半径见 {@code FrostArmament.HAMMER_RANGE}（12 格）；连击 10 档的「额外触发 1 次」
 * 会让这一冲多扔一把（第二把打下一个最近的敌人）。附近没有敌人时冲刺不会把充能吃掉，留着下次再砸。
 *
 * <p>落地的锤子是一个旋转着飞过去的铁斧展示实体（没有锤子建模，先借原版铁斧，见
 * {@code frost.FrostArmament}）。伤害类型与剑鞘一致：{@code sephiria:ice_attack}
 * （冰属性攻击伤害，照常触发霜之触）。
 */
public class BlizzardHammerItem extends ArtifactItem {
	/** 各等级的固定伤害（点）。 */
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 6.0D, 8.0D, 10.0D, 12.0D, 14.0D, 16.0D };
	/** 各等级的「冰元素强度」倍率（%）。 */
	private static final double[] ICE_PERCENT_BY_LEVEL = { 50.0D, 70.0D, 80.0D, 100.0D, 120.0D, 130.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public BlizzardHammerItem(Properties properties) {
		super(properties);
	}

	/** 该等级的固定伤害（充能系统按它结算；表与提示框共用一处）。 */
	public static double flatDamageAt(int level) {
		return FLAT_DAMAGE_BY_LEVEL[Mth.clamp(level, 0, FLAT_DAMAGE_BY_LEVEL.length - 1)];
	}

	/** 该等级的「冰元素强度」倍率（%）。 */
	public static double icePercentAt(int level) {
		return ICE_PERCENT_BY_LEVEL[Mth.clamp(level, 0, ICE_PERCENT_BY_LEVEL.length - 1)];
	}

	@Override
	public ArtifactCombo combo() {
		return ArtifactCombo.FROST_ARMAMENT;
	}

	@Override
	public ArtifactRarity rarity() {
		return ArtifactRarity.ADVANCED;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.blizzard_hammer.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return FLAT_DAMAGE_BY_LEVEL.length - 1;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.blizzard_hammer",
				Component.literal(Numbers.format(flatDamageAt(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(icePercentAt(level))).withColor(COLOUR_BONUS)));
	}
}
