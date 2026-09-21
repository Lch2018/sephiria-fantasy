package com.sephiria.slate;

import java.util.List;

/**
 * 各石板的影响范围（相对石板所在格的偏移与增量）。
 *
 * <p>数值集中在这里：每块石板一段，改范围与数值只动这一个文件。
 * 坐标是 {@code (dx, dy)}，dx 右为正、dy <b>下</b>为正；朝向 0 就是参考图里画的方向。
 *
 * <p>所有石板都不影响自己所在的格子，只影响周围——参考图里数字都画在旁边的格子里。
 */
public final class Slates {
	/** 未来：正上方三格 + 左侧一格，各 +1（可旋转）。 */
	public static final List<PatternSlateItem.Cell> FUTURE = List.of(
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(1, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1));

	/** 誓言：正上方 +2、再上一格 +1，左右各 +1，正下方 +1（可旋转）。 */
	public static final List<PatternSlateItem.Cell> OATH = List.of(
			new PatternSlateItem.Cell(0, -2, 2),
			new PatternSlateItem.Cell(0, -1, 1),
			new PatternSlateItem.Cell(-1, 0, 1),
			new PatternSlateItem.Cell(1, 0, 1),
			new PatternSlateItem.Cell(0, 1, 1));

	/** 信念：正上方一格 +5（可旋转）。 */
	public static final List<PatternSlateItem.Cell> BELIEF = List.of(
			new PatternSlateItem.Cell(0, -1, 5));

	/** 入口：正上方一排 +1 / +2 / +1（<b>不可旋转</b>）。 */
	public static final List<PatternSlateItem.Cell> ENTRANCE = List.of(
			new PatternSlateItem.Cell(-1, -1, 1),
			new PatternSlateItem.Cell(0, -1, 2),
			new PatternSlateItem.Cell(1, -1, 1));

	/** 竞争：左上方与正上方各 -1，正下方 +3（可旋转）。 */
	public static final List<PatternSlateItem.Cell> COMPETITION = List.of(
			new PatternSlateItem.Cell(-1, -1, -1),
			new PatternSlateItem.Cell(0, -1, -1),
			new PatternSlateItem.Cell(0, 1, 3));

	private Slates() {
	}
}
