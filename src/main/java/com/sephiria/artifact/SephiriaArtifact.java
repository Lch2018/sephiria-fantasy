package com.sephiria.artifact;

import net.minecraft.network.chat.Component;

/**
 * 一件神器：连招 + 品质 + 随等级变化的效果。
 *
 * <p>等级：获得时是 0 级（{@link ArtifactItem#LEVEL}），可用神器附魔币升级；
 * 放在赛菲利亚背包的格子里时，实际等级 = 自身等级 + 格子等级，为负则这件神器不生效。
 */
public interface SephiriaArtifact {

	/** 连招（坚固……）。 */
	ArtifactCombo combo();

	ArtifactRarity rarity();

	/** 背景描述（风味文本）的语言键。 */
	String flavorKey();

	/**
	 * 是否带【唯一】。
	 *
	 * <p>带这个词条的神器，玩家持有多个<b>相同</b>的（同名同种）时只算等级最高的那一个；
	 * 不带的话每个副本各算一份。
	 */
	boolean unique();

	/** 最高等级（各神器的等级表长度不同，力量护符 0..3、战士的证明 0..5）。 */
	int maxLevel();

	/**
	 * 这个等级下的效果行（{@code - 物理伤害 +2} 那种）。
	 *
	 * <p>数值由神器自己报告，所以改常量提示框与结算同时变。
	 */
	java.util.List<Component> affixLines(int level);

	/** 该等级给的物理强度加成（背包里按【唯一】规则汇总）。 */
	default double physicalBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的特殊攻击伤害加成（百分点，100 = 100%）：只影响武器技能。 */
	default double specialAttackBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的近战攻击范围加成（百分点，100 = 100%）。 */
	default double meleeRangePercentBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的攻击速度加成（百分点，100 = 100%）。 */
	default double attackSpeedBonus(int level) {
		return 0.0D;
	}
}
