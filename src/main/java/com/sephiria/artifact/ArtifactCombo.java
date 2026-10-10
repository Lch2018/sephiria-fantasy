package com.sephiria.artifact;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 神器的「连击」（类似自走棋的羁绊）。
 *
 * <p>放在赛菲利亚背包里的神器会一起提升所属连击的等级：<b>每件生效的神器各记 1 级</b>，
 * 但带【唯一】的同种神器只算一次——三个力量护符还是 +1 级，而三本没有【唯一】的盾牌术教材
 * 是 +3 级。卸下则等级随之下降。
 *
 * <p>达到阈值的增益<b>逐档累加</b>：坚固的 2/4/6/8 四档都是物理强度 +N，8 级就是 +2+4+6+8；
 * 10 级那档是另一种增益（物理伤害增幅 / 武器伤害 + 冲刺次数），同样叠上去。
 */
public enum ArtifactCombo {
	/** 坚固：物理强度与物理伤害增幅。 */
	STURDY("sturdy", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.sturdy.t2", 2.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.sturdy.t4", 4.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.sturdy.t6", 6.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.sturdy.t8", 8.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.sturdy.t10", 0.0D, 15.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false)
	}),

	/** 风之歌：攻速、武器伤害与冲刺上限。 */
	WIND_SONG("wind_song", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.wind.t2", 0.0D, 0.0D, 8.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.wind.t4", 0.0D, 0.0D, 12.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.wind.t6", 0.0D, 0.0D, 16.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.wind.t8", 0.0D, 0.0D, 20.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.wind.t10", 0.0D, 0.0D, 0.0D, 15.0D, 1, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false)
	}),

	/** 影子：闪避（各档累加，10 级共 +33 点）。 */
	SHADOW("shadow", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.shadow.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 4.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.shadow.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 5.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.shadow.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 6.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.shadow.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 8.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.shadow.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 10.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false)
	}),

	/** 精密：暴击几率，10 级那档给暴击伤害。 */
	PRECISION("precision", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.precision.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 4.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.precision.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 6.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.precision.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 8.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.precision.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 10.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.precision.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 30.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false)
	}),

	/** 谈判：谈判力（商店折扣）与叶子获得量，4 级解锁黄金之手。 */
	NEGOTIATION("negotiation", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.negotiation.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					10.0D, 15.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.negotiation.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					15.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, true)
	}),

	/** 元素：最高元素伤害，6 级那档把四项强度（含物理）一起放大。 */
	ELEMENT("element", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.element.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 5.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.element.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 6.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.element.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D, 0.0D, false, false)
	}),

	/** 魔法科技：2 档激活「电击之触」，电元素强度驱动触电的结算伤害。 */
	MAGIC_TECH("magic_tech", "artifact.sephiria_fantasy.combo.magic_tech.intro", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.magic_tech.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, true, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.magic_tech.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 6.0D, 0.0D, 0.0D, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.magic_tech.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 150.0D, 0.0D, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.magic_tech.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 8.0D, 0.0D, 0.0D, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.magic_tech.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, -1.0D, false, false)
	}),

	/**
	 * 余烬：2 档激活「火焰之触」，火元素强度驱动灼伤的结算伤害。
	 *
	 * <p>与魔法科技同型（都靠一条「之触」把减益挂到目标身上），区别在减益本身：
	 * 灼伤会<b>刷新持续时间</b>并按层数放大每跳，触电则不刷新。
	 */
	EMBER("ember", "artifact.sephiria_fantasy.combo.ember.intro", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.ember.t2", 0.0D, 0.0D, 0.0D),
			new Tier(4, "artifact.sephiria_fantasy.combo.ember.t4", 6.0D, 0.0D, 0.0D),
			new Tier(6, "artifact.sephiria_fantasy.combo.ember.t6", 8.0D, 0.0D, 0.0D),
			new Tier(8, "artifact.sephiria_fantasy.combo.ember.t8", 0.0D, 150.0D, 0.0D),
			new Tier(10, "artifact.sephiria_fantasy.combo.ember.t10", 0.0D, 0.0D, 18.0D)
	}),

	/** 乌云：2 档点亮乌云（基础容量 15），4/6/8/10 档加容量，10 档解锁「2 点射」。 */
	DARK_CLOUD("dark_cloud", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.dark_cloud.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false, 0, true, false),
			new Tier(4, "artifact.sephiria_fantasy.combo.dark_cloud.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false, 8, false, false),
			new Tier(6, "artifact.sephiria_fantasy.combo.dark_cloud.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false, 12, false, false),
			new Tier(8, "artifact.sephiria_fantasy.combo.dark_cloud.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false, 16, false, false),
			new Tier(10, "artifact.sephiria_fantasy.combo.dark_cloud.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false, 20, false, true)
	}),

	/** 太阳剑：2 档激活（基础 5 支），4/6/8/10 档加伤害，6/8/10 档加数量上限。 */
	SUN_SWORD("sun_sword", "artifact.sephiria_fantasy.combo.sun_sword.intro", new Tier[] {
			new Tier(2, "artifact.sephiria_fantasy.combo.sun_sword.t2", 0.0D, 0),
			new Tier(4, "artifact.sephiria_fantasy.combo.sun_sword.t4", 6.0D, 0),
			new Tier(6, "artifact.sephiria_fantasy.combo.sun_sword.t6", 8.0D, 1),
			new Tier(8, "artifact.sephiria_fantasy.combo.sun_sword.t8", 10.0D, 2),
			new Tier(10, "artifact.sephiria_fantasy.combo.sun_sword.t10", 12.0D, 6)
	});

	/**
	 * 一档增益：达到 {@code level} 级时生效。各字段按需填，用不到的就是 0。
	 *
	 * @param physical      物理强度（点）
	 * @param physicalAmp   物理伤害增幅（%）
	 * @param attackSpeed   攻击速度（%）
	 * @param weaponDamage  武器伤害（%）
	 * @param dashCharges   冲刺存储上限（次）
	 * @param critChance    暴击几率（%）
	 * @param critDamage    暴击伤害（%），加在默认的 150% 上
	 * @param dodge         闪避（点）：按 0.8×(1−e^(−闪避/43.28)) 折算成闪避率
	 * @param negotiation   谈判力（点）：给商店价格打折
	 * @param leafGainPercent 叶子获得量（%）
	 * @param highestElement 最高元素伤害（点）：加到物理/火/冰/电四项强度里数值最高的那一项上
	 * @param allElementPercent 「所有元素伤害提升」（%）：四项强度（含物理）一起按同一比例放大
	 * @param lightningElement 电元素强度（点）：并进触发者的电元素强度面板值，触电每次激活按它结算
	 * @param fireElement   火元素强度（点）：并进触发者的火元素强度面板值，灼伤每跳按它结算
	 * @param touchHaste    「之触冷却加速」（%）：电击之触 / 火焰之触共用，实际间隔 = 基础间隔 ÷ (1 + 加成)
	 * @param shockDurationDelta 「触电激活时间」调整（秒，负数缩短默认的 2 秒）
	 * @param electricTouch 激活「电击之触」：造成伤害时给目标附加「触电」减益（到档即开）
	 * @param goldenHands   黄金之手：每持有 200 叶子，对敌人造成的伤害 +1%（最大 +20%）
	 * @param cloudCapacity 乌云容量加成（点）：加在基础容量 15 上（只有乌云用）
	 * @param cloudActive   激活「乌云」：召唤头顶的乌云（到档即开，只有乌云用）
	 * @param cloudDoubleShot 「乌云 2 点射」：一次攻击连打两下、消耗 2 点容量（到档即开，只有乌云用）
	 * @param sunDamagePercent 「太阳剑伤害」加成（%）：放大投掷太阳剑那一下的伤害（只有太阳剑用）
	 * @param sunCapacity   太阳剑数量上限加成（支）：加在基础 5 支上（只有太阳剑用）
	 * @param burnBasePercent 「灼伤的基础伤害倍率」（%）：10 档把它从默认的 8 换成 18（只有余烬用）
	 */
	public record Tier(int level, String textKey, double physical, double physicalAmp, double attackSpeed,
			double weaponDamage, int dashCharges, double critChance, double critDamage, double dodge,
			double negotiation, double leafGainPercent, double highestElement, double allElementPercent,
			double lightningElement, double touchHaste, double shockDurationDelta,
			boolean electricTouch, boolean goldenHands,
			int cloudCapacity, boolean cloudActive, boolean cloudDoubleShot,
			double sunDamagePercent, int sunCapacity, double fireElement, double burnBasePercent) {
		/**
		 * 不带乌云 / 太阳剑 / 余烬字段的紧凑构造：这三套的专属字段只有它们自己用，其它连击照旧传 19 个参数。
		 *
		 * <p>加了这几套之后档位构造有 26 个参数，容易数错；除非写乌云 / 太阳剑 / 余烬档位，否则都用这个。
		 */
		public Tier(int level, String textKey, double physical, double physicalAmp, double attackSpeed,
				double weaponDamage, int dashCharges, double critChance, double critDamage, double dodge,
				double negotiation, double leafGainPercent, double highestElement, double allElementPercent,
				double lightningElement, double touchHaste, double shockDurationDelta,
				boolean electricTouch, boolean goldenHands) {
			this(level, textKey, physical, physicalAmp, attackSpeed, weaponDamage, dashCharges, critChance,
					critDamage, dodge, negotiation, leafGainPercent, highestElement, allElementPercent,
					lightningElement, touchHaste, shockDurationDelta, electricTouch, goldenHands,
					0, false, false, 0.0D, 0, 0.0D, 0.0D);
		}

		/** 只带乌云字段的构造（太阳剑 / 余烬字段留空）：乌云那五档用。 */
		public Tier(int level, String textKey, double physical, double physicalAmp, double attackSpeed,
				double weaponDamage, int dashCharges, double critChance, double critDamage, double dodge,
				double negotiation, double leafGainPercent, double highestElement, double allElementPercent,
				double lightningElement, double touchHaste, double shockDurationDelta,
				boolean electricTouch, boolean goldenHands,
				int cloudCapacity, boolean cloudActive, boolean cloudDoubleShot) {
			this(level, textKey, physical, physicalAmp, attackSpeed, weaponDamage, dashCharges, critChance,
					critDamage, dodge, negotiation, leafGainPercent, highestElement, allElementPercent,
					lightningElement, touchHaste, shockDurationDelta, electricTouch, goldenHands,
					cloudCapacity, cloudActive, cloudDoubleShot, 0.0D, 0, 0.0D, 0.0D);
		}

		/** 只带余烬字段的构造（火元素强度 / 之触冷却加速 / 灼伤倍率）：余烬那五档用。 */
		public Tier(int level, String textKey, double fireElement, double touchHaste, double burnBasePercent) {
			this(level, textKey, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, touchHaste, 0.0D, false, false,
					0, false, false, 0.0D, 0, fireElement, burnBasePercent);
		}

		/** 只带太阳剑字段的构造（余烬字段留空）：太阳剑那五档用。 */
		public Tier(int level, String textKey, double sunDamagePercent, int sunCapacity) {
			this(level, textKey, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D,
					0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false, false,
					0, false, false, sunDamagePercent, sunCapacity, 0.0D, 0.0D);
		}

		/** 这一档给的说明行。 */
		public Component line() {
			return Component.translatable(this.textKey);
		}
	}

	private final String id;
	/** 档位上方那行「主动效果」说明的键；没有说明行的连击是 null。 */
	private final String introKey;
	private final Tier[] tiers;

	ArtifactCombo(String id, Tier[] tiers) {
		this(id, null, tiers);
	}

	/** 带「主动效果」说明行的连击（魔法科技这类有专门被动效果的）。 */
	ArtifactCombo(String id, String introKey, Tier[] tiers) {
		this.id = id;
		this.introKey = introKey;
		this.tiers = tiers;
	}

	public String id() {
		return this.id;
	}

	public String translationKey() {
		return "sephiria_fantasy.combo." + this.id;
	}

	/** 全部阈值（从低到高）。 */
	public Tier[] tiers() {
		return this.tiers;
	}

	/** 各档累加：返回某个字段在当前等级下的总和。 */
	private double sum(int comboLevel, java.util.function.ToDoubleFunction<Tier> field) {
		double total = 0.0D;

		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level()) {
				total += field.applyAsDouble(tier);
			}
		}

		return total;
	}

	/** 攻击速度加成（%）：各档累加。 */
	public double attackSpeedPercent(int comboLevel) {
		return sum(comboLevel, Tier::attackSpeed);
	}

	/** 武器伤害加成（%）：各档累加。 */
	public double weaponDamagePercent(int comboLevel) {
		return sum(comboLevel, Tier::weaponDamage);
	}

	/** 冲刺存储上限加成（次）：各档累加。 */
	public int dashCharges(int comboLevel) {
		return (int) sum(comboLevel, Tier::dashCharges);
	}

	/** 暴击几率加成（%）：各档累加。 */
	public double critChancePercent(int comboLevel) {
		return sum(comboLevel, Tier::critChance);
	}

	/** 暴击伤害加成（%）：各档累加，加在默认的 150% 上。 */
	public double critDamagePercent(int comboLevel) {
		return sum(comboLevel, Tier::critDamage);
	}

	/** 闪避加成（点）：各档累加。 */
	public double dodgeBonus(int comboLevel) {
		return sum(comboLevel, Tier::dodge);
	}

	/** 谈判力加成（点）：各档累加。 */
	public double negotiationBonus(int comboLevel) {
		return sum(comboLevel, Tier::negotiation);
	}

	/** 叶子获得量加成（%）：各档累加。 */
	public double leafGainPercent(int comboLevel) {
		return sum(comboLevel, Tier::leafGainPercent);
	}

	/** 「最高元素伤害」加成（点）：各档累加，只加到四项强度里数值最高的那一项上。 */
	public double highestElementBonus(int comboLevel) {
		return sum(comboLevel, Tier::highestElement);
	}

	/** 「所有元素伤害提升」加成（%）：各档累加，物理/火/冰/电四项强度一起按同一比例放大。 */
	public double allElementPercent(int comboLevel) {
		return sum(comboLevel, Tier::allElementPercent);
	}

	/** 「电击之触」激活档：到档即开，不累加。 */
	public boolean electricTouch(int comboLevel) {
		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level() && tier.electricTouch()) {
				return true;
			}
		}

		return false;
	}

	/** 电元素强度加成（点）：各档累加，并进触发者的电元素强度面板值。 */
	public double lightningElement(int comboLevel) {
		return sum(comboLevel, Tier::lightningElement);
	}

	/** 火元素强度加成（点）：各档累加，并进触发者的火元素强度面板值。 */
	public double fireElement(int comboLevel) {
		return sum(comboLevel, Tier::fireElement);
	}

	/** 「之触冷却加速」加成（%）：各档累加，实际间隔 = 基础间隔 ÷ (1 + 加成)。电击之触与火焰之触共用。 */
	public double touchHastePercent(int comboLevel) {
		return sum(comboLevel, Tier::touchHaste);
	}

	/** 「火焰之触」激活档：到档即开（余烬的第一档就是 2 档），不累加。 */
	public boolean flameTouch(int comboLevel) {
		return comboLevel >= this.tiers[0].level();
	}

	/**
	 * 「灼伤的基础伤害倍率」（%）：没到 10 档返回 0，表示「沿用默认值」（见 {@code Debuffs.BURN_BASE_PERCENT}）；
	 * 10 档返回 18，把默认的 8 换掉（换、不是加，所以这里只取档位给的数值）。
	 */
	public double burnBasePercent(int comboLevel) {
		return sum(comboLevel, Tier::burnBasePercent);
	}

	/** 「触电激活时间」调整（秒）：各档累加，负数缩短默认的 2 秒。 */
	public double shockDurationDelta(int comboLevel) {
		return sum(comboLevel, Tier::shockDurationDelta);
	}

	/** 乌云容量加成（点）：各档累加；基础容量另算（见 {@code DarkCloud.BASE_CAPACITY}）。 */
	public int cloudCapacity(int comboLevel) {
		return (int) sum(comboLevel, Tier::cloudCapacity);
	}

	/** 「乌云」激活档：到档即开，不累加。 */
	public boolean cloudActive(int comboLevel) {
		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level() && tier.cloudActive()) {
				return true;
			}
		}

		return false;
	}

	/** 「乌云 2 点射」档：到档即开，不累加（连续攻击两次、消耗 2 点容量）。 */
	public boolean cloudDoubleShot(int comboLevel) {
		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level() && tier.cloudDoubleShot()) {
				return true;
			}
		}

		return false;
	}

	/** 「太阳剑」激活档：到第一档（2 档）即开，基础数量 5 支另算（见 {@code SunSword.BASE_CAPACITY}）。 */
	public boolean sunActive(int comboLevel) {
		return comboLevel >= this.tiers[0].level();
	}

	/** 「太阳剑伤害」加成（%）：各档累加，放大投掷太阳剑那一下的伤害。 */
	public double sunSwordDamagePercent(int comboLevel) {
		return sum(comboLevel, Tier::sunDamagePercent);
	}

	/** 太阳剑数量上限加成（支）：各档累加，基础 5 支另算。 */
	public int sunSwordCapacity(int comboLevel) {
		return (int) sum(comboLevel, Tier::sunCapacity);
	}

	/** 连击悬停里档位上方的那行「主动效果」说明（电击之触）；没有说明的连击返回 null。 */
	public Component introLine() {
		return this.introKey == null ? null : Component.translatable(this.introKey);
	}

	/** 黄金之手：到档即开，不累加。 */
	public boolean goldenHands(int comboLevel) {
		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level() && tier.goldenHands()) {
				return true;
			}
		}

		return false;
	}

	/** 当前等级已经吃到的物理强度：<b>各档累加</b>（8 级就是 2+4+6+8 = +20）。 */
	public double physicalBonus(int comboLevel) {
		double total = 0.0D;

		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level() && tier.physical() > 0.0D) {
				total += tier.physical();
			}
		}

		return total;
	}

	/** 当前等级已经吃到的物理伤害增幅（%），各档累加。 */
	public double physicalAmpPercent(int comboLevel) {
		return sum(comboLevel, Tier::physicalAmp);
	}

	/** 下一个阈值；已经点满时返回最后一个阈值。 */
	public int nextTier(int comboLevel) {
		for (Tier tier : this.tiers) {
			if (comboLevel < tier.level()) {
				return tier.level();
			}
		}

		return this.tiers[this.tiers.length - 1].level();
	}

	/** 名字的颜色按已到达的档位变化：没到第一档灰、之后白/绿/蓝/橙/红。 */
	public int nameColour(int comboLevel) {
		// 阈值 2/4/6/8/10 分别对应白/绿/蓝/橙/红
		int[] colours = { 0xFF808080, 0xFFFFFFFF, 0xFF55FF55, 0xFF5B8CFF, 0xFFFF9A3C, 0xFFFF5555 };
		int reached = 0;

		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level()) {
				reached++;
			}
		}

		return colours[Math.min(reached, colours.length - 1)];
	}

	/** 连击面板里连击的效果列表（含未解锁的档位）。 */
	public List<Component> effectLines(int comboLevel) {
		List<Component> lines = new java.util.ArrayList<>();

		for (Tier tier : this.tiers) {
			boolean unlocked = comboLevel >= tier.level();
			net.minecraft.network.chat.MutableComponent line = Component.translatable("artifact.sephiria_fantasy.combo.line",
					Component.literal(String.valueOf(tier.level())),
					tier.line());
			lines.add(unlocked ? line.withColor(0xFFFFFFFF) : line.withColor(0xFF808080));
		}

		return lines;
	}
}
