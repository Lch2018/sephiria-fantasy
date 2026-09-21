package com.sephiria.artifact;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 神器的「连击」（类似自走棋的羁绊）。
 *
 * <p>放在赛菲利亚背包里的神器会一起提升所属连击的等级：<b>同一个神器只算一次</b>
 * （带三个力量护符也还是 +1 级），卸下则等级随之下降。
 *
 * <p>达到阈值时按「当前达到的最高档」给增益，不是逐档累加：坚固的 2/4/6/8 四档都是
 * 物理强度 +N，取最高档（8 级就是 +8，而不是 +2+4+6+8）；10 级的 +15% 物理伤害增幅
 * 是另一种增益，会叠在它之上。
 */
public enum ArtifactCombo {
	/** 坚固：物理强度与物理伤害增幅。 */
	STURDY("sturdy", new Tier[] {
			new Tier(2, "artifact.sephiria.combo.sturdy.t2", 2.0D, 0.0D),
			new Tier(4, "artifact.sephiria.combo.sturdy.t4", 4.0D, 0.0D),
			new Tier(6, "artifact.sephiria.combo.sturdy.t6", 6.0D, 0.0D),
			new Tier(8, "artifact.sephiria.combo.sturdy.t8", 8.0D, 0.0D),
			new Tier(10, "artifact.sephiria.combo.sturdy.t10", 0.0D, 15.0D)
	});

	/** 一档增益：达到 {@code level} 级时给 {@code physical} 点物理强度、{@code physicalAmp} % 物理伤害增幅。 */
	public record Tier(int level, String textKey, double physical, double physicalAmp) {
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
		double total = 0.0D;

		for (Tier tier : this.tiers) {
			if (comboLevel >= tier.level()) {
				total += tier.physicalAmp();
			}
		}

		return total;
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
