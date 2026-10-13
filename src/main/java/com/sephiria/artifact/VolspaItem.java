package com.sephiria.artifact;

import com.sephiria.util.Numbers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 沃尔斯帕（冰霜武具，稀有品质）：【唯一】战斗中每 {@code FrostArmament.SPEAR_CHARGE_TICKS}
 * （6 秒）充能一次；充能后周围有敌人就自动飞来冰长矛攻击敌人 1/1/1/1/1/2 次
 * [伤害：5/5/6/6/8/8 + 冰元素强度 × 50/55/60/65/70/75%]（0..5 级，共 6 档）。
 *
 * <p>冰长矛的机制见 {@code frost.FrostArmament}：从玩家 12 格内的随机位置出现、冲向敌人，
 * 飞 18 格、比剑气快 30%、贯穿敌人、可以穿墙，伤害范围比剑气小 30%。
 * 它是现成的「自动攻击」型神器（与红蛇之眼的陨石、桑德耳环同型：索敌半径 12 格、脱战不动手）。
 *
 * <p>连击 10 档与冰之翼的「额外触发」会让这一轮多飞几根。
 */
public class VolspaItem extends ArtifactItem {
	/** 各等级每次触发的冰长矛数量（根）。 */
	private static final int[] COUNT_BY_LEVEL = { 1, 1, 1, 1, 1, 2 };
	/** 各等级的固定伤害（点）。 */
	private static final double[] FLAT_DAMAGE_BY_LEVEL = { 5.0D, 5.0D, 6.0D, 6.0D, 8.0D, 8.0D };
	/** 各等级的「冰元素强度」倍率（%）。 */
	private static final double[] ICE_PERCENT_BY_LEVEL = { 50.0D, 55.0D, 60.0D, 65.0D, 70.0D, 75.0D };
	/** 词条数值的颜色：绿色。 */
	private static final int COLOUR_BONUS = 0xFF55FF55;

	public VolspaItem(Properties properties) {
		super(properties);
	}

	/** 该等级每次触发的长矛数（充能系统按它结算；表与提示框共用一处）。 */
	public static int countAt(int level) {
		return COUNT_BY_LEVEL[Mth.clamp(level, 0, COUNT_BY_LEVEL.length - 1)];
	}

	/** 该等级的固定伤害（点）。 */
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
		return ArtifactRarity.RARE;
	}

	@Override
	public String flavorKey() {
		return "artifact.sephiria_fantasy.volspa.flavor";
	}

	@Override
	public boolean unique() {
		return true;
	}

	@Override
	public int maxLevel() {
		return COUNT_BY_LEVEL.length - 1;
	}

	@Override
	public java.util.List<Component> affixLines(int level) {
		return java.util.List.of(Component.translatable("artifact.sephiria_fantasy.affix.ice_spear",
				Component.literal(Numbers.format(countAt(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(flatDamageAt(level))).withColor(COLOUR_BONUS),
				Component.literal(Numbers.format(icePercentAt(level))).withColor(COLOUR_BONUS)));
	}
}
