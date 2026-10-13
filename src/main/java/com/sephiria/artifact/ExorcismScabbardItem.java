package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 驱邪剑鞘（冰霜武具，高级品质）：【唯一】每 {@code FrostArmament.SCABBARD_CHARGE_TICKS}（6 秒）
 * 充能一次；充能后<b>左键挥击</b>（打中敌人与挥空都算，手上得是本模组的武器）就从眼睛前方
 * 沿<b>准星方向</b>放出一道冰属性剑气——伤害 = 固定值 4/6/8/10/12/14/16
 * + 冰元素强度 × 30/40/50/60/70/80/100%（0..6 级，共 7 档）。
 *
 * <p>剑气是一道<b>飞出去</b>的弧（见 {@code frost.FrostArmament}）：飞 30 格（与雷之裁决同距离）、
 * 沿途对轨迹周围一圈的敌人各打一次、撞到方块就停；<b>不需要瞄准敌人</b>。
 * 连击的「充能速度」缩短充能时间、「冰霜武具的伤害」放大这一下，10 档每次挥出两道。
 *
 * <p>伤害类型是 {@code sephiria:ice_attack}（冰属性攻击伤害）：吃通用暴击、减伤与黄金之手，
 * 也照常触发冰川的霜之触——与普罗修姆戒指的附加冰伤同一条路。
 */
public class ExorcismScabbardItem extends ArtifactItem {
	/** 各等级的固定伤害（点）。 */
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 4.0D, 6.0D, 8.0D, 10.0D, 12.0D, 14.0D, 16.0D };
	/** 各等级的「冰元素强度」倍率（%）：剑气伤害的第二段。 */
	private static final double[] ICE_PERCENT_BY_LEVEL = { 30.0D, 40.0D, 50.0D, 60.0D, 70.0D, 80.0D, 100.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public ExorcismScabbardItem(Properties properties) {
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
		return "artifact.sephiria_fantasy.exorcism_scabbard.flavor";
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
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.frost_qi",
				Component.literal(Numbers.format(flatDamageAt(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(icePercentAt(level))).withColor(COLOUR_BONUS)));
	}
}
