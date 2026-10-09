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

	/** 该等级给的谈判力加成（点）：给商店买入打折，见 {@code PlayerStats#shopDiscountPercent}。 */
	default double negotiationBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的叶子获得量加成（百分点，100 = 100%）。 */
	default double leafGainPercentBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的经验掉落加成（百分点，100 = 100%）。 */
	default double xpDropPercentBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级在商店每买一瓶药水时生成的叶子（幸运的奖章）。
	 *
	 * <p>0 表示没有这个词条。生成走 {@code PlayerStats#grantLeaves}——不是经验，
	 * 不推进升级宝箱的计数。
	 */
	default double potionBuyLeafBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的最大蓝量加成（点，可为负）。
	 *
	 * <p>上限 = 默认值 50 + 各来源加成，下限夹在 1；剑耳环是第一个给负数加成的——
	 * 它削的是上限，不是当前蓝量（超出的部分花掉再按新上限回）。
	 */
	default double maxMpBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「电元素强度」加成（点）。
	 *
	 * <p>魔法科技那批神器的通用钩子（电击护符 / 桑德耳环 / 雷云追踪指南针）：并进电元素强度的
	 * 面板值，触电与各种闪电攻击都按面板值结算。萤火虫的加成有「受伤后禁用」，
	 * 走 {@link #fireflyLightningElement} 单独算。
	 */
	default double lightningElementBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「冰元素强度」加成（点，可为负）。
	 *
	 * <p>麒麟的角用它削冰：负值直接进冰元素强度的固定值段（下限 0）。
	 */
	default double iceElementBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「火元素强度」加成（点）。
	 *
	 * <p>索利斯那两枚徽章的通用钩子：并进火元素强度的面板值，太阳剑按它换算伤害
	 * （「火焰属性伤害」与「火元素强度」是同一个属性）。
	 */
	default double fireElementBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「最大生命值」加成（点）。
	 *
	 * <p>由 {@code stats/StatAttributes} 以原版属性修饰符的形式挂到玩家身上（每 4 刻对账一次），
	 * 所以复活、换装扮都会自己补回来。
	 */
	default double maxHpBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「太阳剑数量上限」加成（支）：加在基础 5 支与连击档位之上。
	 *
	 * <p>0 表示没有这个词条（索利斯·帕尔沃是唯一有它的）。
	 */
	default int sunSwordCapacityBonus(int level) {
		return 0;
	}

	/**
	 * 该等级给的「电属性攻击的暴击几率」加成（百分点）。
	 *
	 * <p>只加在电属性伤害上（触电结算、附加闪电伤害、闪电攻击）；魔法书那种通用伤害走
	 * {@link #critChanceBonus}。
	 */
	default double electricCritChanceBonus(int level) {
		return 0.0D;
	}

	/** 该等级给的「触电叠加上限」加成（层）：加在默认的 2 层上。 */
	default int shockStackBonus(int level) {
		return 0;
	}

	/** 该等级给的「灼伤叠加上限」加成（层）：加在默认的 2 层上（火焰虫与熔岩珠都填这条）。 */
	default int burnStackBonus(int level) {
		return 0;
	}

	/**
	 * 该等级「灼伤异常状态额外伤害」加成（%）：每跳伤害乘 (1 + 加成)。
	 *
	 * <p>0 表示没有这个词条（红色线球是唯一有它的）。
	 */
	default double burnDamagePercent(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「灼伤的攻击速度」加成（%）：把每跳的间隔压短，实际间隔 = 基础间隔 ÷ (1 + 加成)。
	 *
	 * <p>口径与乌云的「消耗速度」一致（间隔向下取整到刻，最小 1 刻）；0 表示没有这个词条
	 * （橡木炭是唯一有它的）。
	 */
	default double burnTickSpeedPercent(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「赋予灼伤时额外给予的次数」：挂灼伤的那一下多叠几层（层数上限照旧管着）。
	 *
	 * <p>0 表示没有这个词条（熔岩珠是唯一有它的）。
	 */
	default int burnExtraApplications(int level) {
		return 0;
	}

	/**
	 * 该等级「每 5 秒掉落的陨石」个数（红蛇之眼）。
	 *
	 * <p>0 表示没有这个词条。
	 */
	default int meteorCount(int level) {
		return 0;
	}

	/**
	 * 该等级陨石那一下的伤害倍率（%）：火元素强度面板值 × 它。
	 *
	 * <p>0 表示没有这个词条（红蛇之眼是唯一有它的）。
	 */
	default double meteorDamagePercent(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「强化触电」概率（%）：施加触电时按它掷，命中就立刻额外结算一次触电伤害。
	 *
	 * <p>0 表示没有这个词条（雷云追踪指南针是唯一有它的）。
	 */
	default double enhancedShockChance(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「使用武器攻击或魔法书造成伤害时附加的闪电属性伤害」（点，固定值）。
	 *
	 * <p>0 表示没有这个词条（被雷击中的树枝是唯一有它的）；冷却由物品类自己记。
	 */
	default double onHitLightningDamage(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「电元素强度」加成（点）——萤火虫专用：受到攻击后的 6 秒里整件失效，
	 * 所以它不走 {@link #lightningElementBonus}，由汇总处单独判断。
	 */
	default double fireflyLightningElement(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「每 4 秒的闪电攻击」能打几个目标（桑德耳环）。
	 *
	 * <p>0 表示没有这个词条。
	 */
	default int sandeTargetCount(int level) {
		return 0;
	}

	/**
	 * 该等级「乌云的消耗速度」按攻击速度的百分之多少往上加（%）。
	 *
	 * <p>0 表示没有这个词条（乐谱《台风》是唯一有它的）。实际消耗速度 = 100% + 攻击速度 × 这个比例，
	 * 攻击间隔 = 基础间隔 ÷ 消耗速度。
	 */
	default double cloudSpeedPercentOfAttackSpeed(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「武器攻击时附加的闪电属性伤害」（点，固定值，无冷却）。
	 *
	 * <p>0 表示没有这个词条（乐谱《台风》是唯一有它的）。与「被雷击中的树枝」的
	 * {@link #onHitLightningDamage} 分开：那个带 2 秒冷却、武器与魔法书都触发，这个只有武器、不限次数。
	 */
	default double weaponLightningDamage(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「乌云容量」加成（点）：加在基础容量 15 与乌云连击的档位加成之上。
	 */
	default int cloudCapacityBonus(int level) {
		return 0;
	}

	/** 该等级给的「蓝量再生」加成（点/秒）。 */
	default double mpRegenBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级给的「战斗中乌云恢复速度」加成（%）。
	 *
	 * <p>战斗中乌云每 5 秒回上限的 5%，这一项把回复量乘 (1 + 加成)；脱战那条线不受它影响。
	 */
	default double cloudCombatRegenPercent(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「乌云的额外伤害」加成（%）：每记雷击的伤害乘 (1 + 加成)。
	 *
	 * <p>0 表示没有这个词条（雷石是唯一有它的）。
	 */
	default double cloudDamagePercent(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「乌云的闪电被强化」的概率（%）：命中就打出双倍伤害。
	 *
	 * <p>0 表示没有这个词条（云种箭头是唯一有它的）。
	 */
	default double cloudEnhancedChance(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「乌云的消耗速度」固定加成（%）。
	 *
	 * <p>与 {@link #cloudSpeedPercentOfAttackSpeed} 不同：那个按<b>攻击速度的比例</b>往上加
	 * （乐谱《台风》），这个是固定点数（桅杆模型），两者相加后一起算攻击间隔。
	 */
	default double cloudSpeedBonus(int level) {
		return 0.0D;
	}

	/**
	 * 该等级「乌云攻击不消耗容量」的概率（%）。
	 *
	 * <p>0 表示没有这个词条（雷文泥板是唯一有它的）。
	 */
	default double cloudFreeShotChance(int level) {
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
