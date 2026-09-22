package com.sephiria.artifact;

import net.minecraft.network.chat.Component;

/**
 * 一件神器：连招 + 品质 + 随等级变化的效果。
 *
 * <p>等级：获得时是 0 级（{@link ArtifactItem#LEVEL}），可用神器附魔币升级；
 * 放在赛菲利亚背包的格子里时，实际等级 = 自身等级 + 格子等级，为负则这件神器不生效。
 */
public interface SephiriaArtifact extends Quality {

	/** 连招（坚固……）：双连击神器这里是主连击，另一个见 {@link #combos()}。 */
	ArtifactCombo combo();

	/**
	 * 这件神器参与的所有连击：默认就是 {@link #combo()} 一个。
	 *
	 * <p><b>双连击神器</b>（风车是第一个）重写这个方法，它会同时给两种连击各加 1 级。
	 */
	default java.util.List<ArtifactCombo> combos() {
		return java.util.List.of(combo());
	}

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

	/** 该等级给的冲刺存储上限加成（次）。 */
	default int dashChargesBonus(int level) {
		return 0;
	}

	/** 该等级给的冲刺恢复速度加成（百分点，100 = 100%）。 */
	default double dashRegenPercentBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「最高元素伤害」加成（点）。
	 *
	 * <p>加在物理强度 / 火 / 冰 / 电这四项里<b>数值最高</b>的那一项上。
	 */
	default double highestElementBonus(int level) {
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

	/** 该等级给的移动速度加成（百分点，100 = 100%）。 */
	default double moveSpeedPercentBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的「普通攻击伤害」加成（百分点，100 = 100%）：只加成普通攻击，技能不吃。 */
	default double normalAttackDamagePercentBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的暴击几率加成（百分点）：任何由玩家造成的伤害都吃。 */
	default double critChanceBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「武器攻击的暴击几率」加成（百分点）。
	 *
	 * <p>只有<b>武器打出的</b>伤害吃得到（普通攻击、横扫、弩矢、武器技能），神器自己造成的伤害吃不到。
	 */
	default double weaponCritChanceBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的暴击伤害加成（百分点），加在默认的 150% 上。 */
	default double critDamageBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「无视防御伤害」（点）。
	 *
	 * <p>玩家每次造成伤害时，额外打一次这个数值的真实伤害：不吃护甲/韧性/防御力/减伤，
	 * 但会按该次攻击的倍率放大（普通攻击吃普攻那一套，技能吃技能那一套）。
	 */
	default double ignoreDefenseBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「暴击溅射」比例（%）：暴击时对周围敌人造成最高属性值 × 这个比例的伤害。
	 *
	 * <p>0 表示没有这个词条（红色露水是唯一有这个效果的）。
	 */
	default double critSplashPercent(int level) {
		return 0.0D;
	}

	/** 该等级给的闪避（点）：按 0.8×(1−e^(−闪避/43.28)) 折算成闪避率。 */
	default double dodgeBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「闪避触发时恢复的冲刺次数」。
	 *
	 * <p>0 表示没有这个词条（弹力带是唯一有它的）。
	 */
	default double dodgeRestoreDashCharges(int level) {
		return 0.0D;
	}

	/**
	 * 这件神器给的<b>神器技能</b>（没有就返回 null）。
	 *
	 * <p>对应「发动型神器」与「获得技能」两种词条。技能本身无状态，数值按等级现算，
	 * 所以这里返回单例即可——同一个技能在不同等级下效果不同，由调用方把等级传进去。
	 */
	default com.sephiria.artifact.skill.ArtifactSkill skill() {
		return null;
	}
}
