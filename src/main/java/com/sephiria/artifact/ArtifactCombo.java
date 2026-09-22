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
			new Tier(2, "artifact.sephiria.combo.sturdy.t2", 2.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(4, "artifact.sephiria.combo.sturdy.t4", 4.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(6, "artifact.sephiria.combo.sturdy.t6", 6.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(8, "artifact.sephiria.combo.sturdy.t8", 8.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(10, "artifact.sephiria.combo.sturdy.t10", 0.0D, 15.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D)
	}),

	/** 风之歌：攻速、武器伤害与冲刺上限。 */
	WIND_SONG("wind_song", new Tier[] {
			new Tier(2, "artifact.sephiria.combo.wind.t2", 0.0D, 0.0D, 8.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(4, "artifact.sephiria.combo.wind.t4", 0.0D, 0.0D, 12.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(6, "artifact.sephiria.combo.wind.t6", 0.0D, 0.0D, 16.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(8, "artifact.sephiria.combo.wind.t8", 0.0D, 0.0D, 20.0D, 0.0D, 0, 0.0D, 0.0D, 0.0D),
			new Tier(10, "artifact.sephiria.combo.wind.t10", 0.0D, 0.0D, 0.0D, 15.0D, 1, 0.0D, 0.0D, 0.0D)
	}),

	/** 影子：闪避（各档累加，10 级共 +33 点）。 */
	SHADOW("shadow", new Tier[] {
			new Tier(2, "artifact.sephiria.combo.shadow.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 4.0D),
			new Tier(4, "artifact.sephiria.combo.shadow.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 5.0D),
			new Tier(6, "artifact.sephiria.combo.shadow.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 6.0D),
			new Tier(8, "artifact.sephiria.combo.shadow.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 8.0D),
			new Tier(10, "artifact.sephiria.combo.shadow.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 0.0D, 10.0D)
	}),

	/** 精密：暴击几率，10 级那档给暴击伤害。 */
	PRECISION("precision", new Tier[] {
			new Tier(2, "artifact.sephiria.combo.precision.t2", 0.0D, 0.0D, 0.0D, 0.0D, 0, 4.0D, 0.0D, 0.0D),
			new Tier(4, "artifact.sephiria.combo.precision.t4", 0.0D, 0.0D, 0.0D, 0.0D, 0, 6.0D, 0.0D, 0.0D),
			new Tier(6, "artifact.sephiria.combo.precision.t6", 0.0D, 0.0D, 0.0D, 0.0D, 0, 8.0D, 0.0D, 0.0D),
			new Tier(8, "artifact.sephiria.combo.precision.t8", 0.0D, 0.0D, 0.0D, 0.0D, 0, 10.0D, 0.0D, 0.0D),
			new Tier(10, "artifact.sephiria.combo.precision.t10", 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D, 30.0D, 0.0D)
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
	 */
	public record Tier(int level, String textKey, double physical, double physicalAmp, double attackSpeed,
			double weaponDamage, int dashCharges, double critChance, double critDamage, double dodge) {
		/** 这一档给的说明行。 */
		public Component line() {
			return Component.translatable(this.textKey);
		}
	}

	private final String id;
	private final Tier[] tiers;

	ArtifactCombo(String id, Tier[] tiers) {
		this.id = id;
		this.tiers = tiers;
	}

	public String id() {
		return this.id;
	}

	public String translationKey() {
		return "sephiria.combo." + this.id;
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
			net.minecraft.network.chat.MutableComponent line = Component.translatable("artifact.sephiria.combo.line",
					Component.literal(String.valueOf(tier.level())),
					tier.line());
			lines.add(unlocked ? line.withColor(0xFFFFFFFF) : line.withColor(0xFF808080));
		}

		return lines;
	}
}
